import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminListProblems, adminDeleteProblem, adminPublishProblem, adminUnpublishProblem } from '../../api/client.js';

export default function AdminProblemList() {
  const [problems, setProblems] = useState([]);
  const [error, setError] = useState(null);
  const [busySlug, setBusySlug] = useState(null);

  function load() {
    adminListProblems().then(setProblems).catch((e) => setError(e.message));
  }

  useEffect(load, []);

  async function handleDelete(slug) {
    if (!confirm(`Delete "${slug}"? This can't be undone.`)) return;
    setBusySlug(slug);
    try {
      await adminDeleteProblem(slug);
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusySlug(null);
    }
  }

  async function handleToggleStatus(problem) {
    setBusySlug(problem.slug);
    try {
      if (problem.status === 'PUBLISHED') {
        await adminUnpublishProblem(problem.slug);
      } else {
        await adminPublishProblem(problem.slug);
      }
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusySlug(null);
    }
  }

  return (
    <div className="page">
      <div className="admin-header-row">
        <h1 className="page-title">Admin — Problems</h1>
        <Link to="/admin/problems/new" className="submit-btn">+ New Problem</Link>
      </div>
      {error && <p className="error">{error}</p>}

      <ul className="problem-list">
        {problems.map((p) => (
          <li key={p.id} className="problem-row admin-row">
            <div>
              <Link to={`/admin/problems/${p.slug}/edit`}>{p.name}</Link>
              <span className={`badge badge-${p.difficulty?.toLowerCase()}`} style={{ marginLeft: 8 }}>
                {p.difficulty}
              </span>
              <span className={`badge status-${p.status?.toLowerCase()}`} style={{ marginLeft: 8 }}>
                {p.status}
              </span>
            </div>
            <div className="admin-row-actions">
              <button
                className="link-btn"
                disabled={busySlug === p.slug}
                onClick={() => handleToggleStatus(p)}
              >
                {p.status === 'PUBLISHED' ? 'Unpublish' : 'Publish'}
              </button>
              <Link to={`/admin/problems/${p.slug}/edit`} className="link-btn">Edit</Link>
              <Link to={`/admin/submissions?slug=${p.slug}`} className="link-btn">Submissions</Link>
              <button
                className="link-btn danger"
                disabled={busySlug === p.slug}
                onClick={() => handleDelete(p.slug)}
              >
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>
      {problems.length === 0 && !error && <p>No problems yet — create one.</p>}
    </div>
  );
}
