import { useEffect, useState, useCallback } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { listProblems, getProgress } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import LoginChoice from './LoginChoice.jsx';

const PAGE_SIZE = 10;

// When a Solved/Unsolved filter is active, the backend has no way to filter
// by per-user solved status at the DB/ES query level (solved-ness lives in
// our submissions table, not the search index), so we fetch a large batch
// matching the search/difficulty filters and do the solved-filter + paging
// client-side instead. Fine at the scale this app runs at; if the problem
// count ever gets huge this would need a real backend filter.
const FETCH_ALL_SIZE = 500;

function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export default function Home() {
  const { user, isAdmin, loading } = useAuth();

  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState('ALL');
  const [status, setStatus] = useState('ALL'); // ALL | SOLVED | UNSOLVED
  const [page, setPage] = useState(0);

  const [result, setResult] = useState(null);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState(null);
  const [progress, setProgress] = useState(null);

  const debouncedSearch = useDebounce(search, 300);

  // Reset to page 0 whenever any filter changes
  useEffect(() => { setPage(0); }, [debouncedSearch, difficulty, status]);

  const load = useCallback(() => {
    setFetching(true);
    setError(null);

    const filteringByStatus = status !== 'ALL';
    const requestSize = filteringByStatus ? FETCH_ALL_SIZE : PAGE_SIZE;
    const requestPage = filteringByStatus ? 0 : page;

    listProblems({ search: debouncedSearch, difficulty, page: requestPage, size: requestSize })
      .then((data) => {
        // Backend always returns { content, page, size, totalElements,
        // totalPages } now, whether it served from Elasticsearch or the
        // MySQL fallback — no more branching on response shape here.
        const content = data.content ?? [];

        if (!filteringByStatus) {
          setResult(data);
          return;
        }

        // Client-side filter + paginate over the full matching set.
        const filtered = content.filter((p) => (status === 'SOLVED' ? p.solved : !p.solved));
        const totalElements = filtered.length;
        const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
        const pageContent = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);
        setResult({ content: pageContent, totalPages, totalElements, page });
      })
      .catch((e) => setError(e.message))
      .finally(() => setFetching(false));
  }, [debouncedSearch, difficulty, status, page]);

  useEffect(() => { load(); }, [load]);

  // Progress is fetched separately (not derived from the current page) so it
  // stays accurate regardless of search/filter/pagination state.
  const refreshProgress = useCallback(() => {
    if (!user) return;
    getProgress().then(setProgress).catch(() => {});
  }, [user]);

  useEffect(() => { refreshProgress(); }, [refreshProgress]);

  if (loading) return <div className="page">Loading…</div>;
  if (!user) return <LoginChoice />;
  if (isAdmin) return <Navigate to="/admin/problems" replace />;

  const problems = result?.content ?? [];
  const totalPages = result?.totalPages ?? 1;
  const totalElements = result?.totalElements ?? 0;

  return (
    <div className="page">
      <h1 className="page-title">Problems</h1>

      {progress && progress.total > 0 && (
        <div className="progress-tracker">
          <div className="progress-tracker-label">
            <span>{progress.solved} / {progress.total} solved</span>
            <span className="hint-text">
              {Math.round((progress.solved / progress.total) * 100)}%
            </span>
          </div>
          <div className="progress-bar-track">
            <div
              className="progress-bar-fill"
              style={{ width: `${(progress.solved / progress.total) * 100}%` }}
            />
          </div>
        </div>
      )}

      <div className="search-bar">
        <input
          className="search-input"
          placeholder="Search problems…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="difficulty-select"
          value={difficulty}
          onChange={(e) => setDifficulty(e.target.value)}
        >
          <option value="ALL">All difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <select
          className="difficulty-select"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="ALL">All problems</option>
          <option value="SOLVED">Solved</option>
          <option value="UNSOLVED">Unsolved</option>
        </select>
      </div>

      {error && <p className="error">{error} — is the backend running on :8080?</p>}

      {fetching && <p className="hint-text">Searching…</p>}

      {!fetching && (
        <>
          <p className="hint-text">
            {totalElements} problem{totalElements !== 1 ? 's' : ''} found
          </p>
          <ul className="problem-list">
            {problems.map((p) => (
              <li key={p.slug} className={`problem-row ${p.solved ? 'solved-row' : ''}`}>
                <span className="problem-title">
                  <Link to={`/problems/${p.slug}`}>{p.name}</Link>
                </span>
                <span className="problem-row-right">
                  {p.solved && <span className="solved-tag">✓ Solved</span>}
                  {p.difficulty && (
                    <span className={`badge badge-${p.difficulty.toLowerCase()}`}>
                      {p.difficulty}
                    </span>
                  )}
                </span>
              </li>
            ))}
          </ul>
          {problems.length === 0 && !error && (
            <p>
              {status === 'ALL'
                ? `No problems found${search ? ` for "${search}"` : ''} — try a different search.`
                : status === 'SOLVED'
                  ? "You haven't solved any problems matching these filters yet."
                  : 'Nothing left unsolved matching these filters — nice work.'}
            </p>
          )}

          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="page-btn"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ← Prev
              </button>
              {Array.from({ length: totalPages }, (_, i) => (
                <button
                  key={i}
                  className={`page-btn ${i === page ? 'page-btn-active' : ''}`}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))}
              <button
                className="page-btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
