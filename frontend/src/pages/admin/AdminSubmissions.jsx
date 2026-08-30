import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  adminListProblems, adminListSubmissions, adminMarkSolved, adminUnmarkSolved,
  adminGetSolvedUsers, adminResetSolved
} from '../../api/client.js';

const PAGE_SIZE = 20;

export default function AdminSubmissions() {
  const [searchParams, setSearchParams] = useSearchParams();
  const slugFilter = searchParams.get('slug') || '';
  const emailFilter = searchParams.get('email') || '';

  const [problems, setProblems] = useState([]);
  const [emailInput, setEmailInput] = useState(emailFilter);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [expandedId, setExpandedId] = useState(null);

  const [solvedUsers, setSolvedUsers] = useState([]);
  const [solvedUsersLoading, setSolvedUsersLoading] = useState(false);
  const [resetBusyUserId, setResetBusyUserId] = useState(null);

  useEffect(() => {
    adminListProblems().then(setProblems).catch(() => {});
  }, []);

  useEffect(() => { setPage(0); }, [slugFilter, emailFilter]);

  const load = useCallback(() => {
    setFetching(true);
    setError(null);
    adminListSubmissions({ slug: slugFilter, email: emailFilter, page, size: PAGE_SIZE })
      .then(setResult)
      .catch((e) => setError(e.message))
      .finally(() => setFetching(false));
  }, [slugFilter, emailFilter, page]);

  useEffect(() => { load(); }, [load]);

  // The "who solved this" panel only makes sense once a specific problem is
  // selected — resetting is done per problem+user, not globally.
  const loadSolvedUsers = useCallback(() => {
    if (!slugFilter) { setSolvedUsers([]); return; }
    setSolvedUsersLoading(true);
    adminGetSolvedUsers(slugFilter)
      .then(setSolvedUsers)
      .catch((e) => setError(e.message))
      .finally(() => setSolvedUsersLoading(false));
  }, [slugFilter]);

  useEffect(() => { loadSolvedUsers(); }, [loadSolvedUsers]);

  function updateFilter(key, value) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value); else next.delete(key);
    setSearchParams(next);
  }

  async function handleToggleSolved(submission) {
    setBusyId(submission.id);
    try {
      if (submission.solved) {
        await adminUnmarkSolved(submission.id);
      } else {
        await adminMarkSolved(submission.id);
      }
      load();
      loadSolvedUsers();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleReset(userId) {
    setResetBusyUserId(userId);
    try {
      await adminResetSolved(slugFilter, userId);
      loadSolvedUsers();
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setResetBusyUserId(null);
    }
  }

  const submissions = result?.content ?? [];
  const totalPages = result?.totalPages ?? 1;
  const totalElements = result?.totalElements ?? 0;
  const selectedProblemName = problems.find((p) => p.slug === slugFilter)?.name;

  return (
    <div className="page">
      <h1 className="page-title">Admin — Submissions</h1>

      <div className="search-bar">
        <select
          className="difficulty-select"
          value={slugFilter}
          onChange={(e) => updateFilter('slug', e.target.value)}
        >
          <option value="">All problems</option>
          {problems.map((p) => (
            <option key={p.slug} value={p.slug}>{p.name}</option>
          ))}
        </select>
        <input
          className="search-input"
          placeholder="Filter by submitter email…"
          value={emailInput}
          onChange={(e) => setEmailInput(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') updateFilter('email', emailInput); }}
          onBlur={() => updateFilter('email', emailInput)}
        />
      </div>

      {error && <p className="error">{error}</p>}

      {slugFilter && (
        <div className="solved-users-panel">
          <h3>Solved by — {selectedProblemName || slugFilter}</h3>
          {solvedUsersLoading && <p className="hint-text">Loading…</p>}
          {!solvedUsersLoading && solvedUsers.length === 0 && (
            <p className="hint-text">No one has solved this problem yet.</p>
          )}
          {!solvedUsersLoading && solvedUsers.map((u) => (
            <div key={u.userId} className="solved-user-row">
              <span>
                {u.userName || u.userEmail} <span className="hint-text">({u.userEmail})</span>
                <span className="hint-text"> · first solved {new Date(u.firstSolvedAt).toLocaleDateString()}</span>
              </span>
              <button
                className="link-btn danger"
                disabled={resetBusyUserId === u.userId}
                onClick={() => handleReset(u.userId)}
              >
                {resetBusyUserId === u.userId ? 'Resetting…' : 'Reset'}
              </button>
            </div>
          ))}
        </div>
      )}

      {fetching && <p className="hint-text">Loading…</p>}

      {!fetching && (
        <>
          <p className="hint-text">
            {totalElements} submission{totalElements !== 1 ? 's' : ''} found
          </p>

          <ul className="problem-list">
            {submissions.map((s) => (
              <li key={s.id} className="problem-row admin-row submission-row">
                <div className="submission-summary">
                  <div>
                    <strong>{s.problemName}</strong>
                    <span className="hint-text"> · {s.userName || s.userEmail} ({s.userEmail})</span>
                  </div>
                  <div className="hint-text">
                    {s.language} · {s.passedCount}/{s.totalCount} tests passed
                    {' · '}{new Date(s.createdAt).toLocaleString()}
                    {s.markedBy && <> · last reviewed by {s.markedBy}</>}
                  </div>
                </div>
                <div className="admin-row-actions">
                  <span className={`badge ${s.solved ? 'status-published' : 'status-draft'}`}>
                    {s.solved ? 'Solved' : 'Not solved'}
                  </span>
                  <button
                    className="link-btn"
                    onClick={() => setExpandedId(expandedId === s.id ? null : s.id)}
                  >
                    {expandedId === s.id ? 'Hide code' : 'View code'}
                  </button>
                  <button
                    className="link-btn"
                    disabled={busyId === s.id}
                    onClick={() => handleToggleSolved(s)}
                    title="Flips just this submission's own record — use the panel above to reset the user's overall solved status"
                  >
                    {s.solved ? 'Unmark this submission' : 'Mark this submission solved'}
                  </button>
                </div>
                {expandedId === s.id && (
                  <div className="submission-detail">
                    {s.approach && (
                      <>
                        <div className="testcase-label">Approach</div>
                        <p>{s.approach}</p>
                      </>
                    )}
                    <div className="testcase-label">Code</div>
                    <pre className="submission-code"><code>{s.code}</code></pre>
                    {s.aiFeedback && (
                      <>
                        <div className="testcase-label">AI feedback</div>
                        <p>{s.aiFeedback}</p>
                      </>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ul>

          {submissions.length === 0 && !error && <p>No submissions found.</p>}

          {totalPages > 1 && (
            <div className="pagination">
              <button className="page-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
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
