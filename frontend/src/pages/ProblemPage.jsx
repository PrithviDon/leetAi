import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { getProblem, submitSolution, googleLoginUrl, getMySubmissions } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function ProblemPage() {
  const { slug } = useParams();
  const { user } = useAuth();
  const [problem, setProblem] = useState(null);
  const [solved, setSolved] = useState(false);
  const [language, setLanguage] = useState('javascript');
  const [code, setCode] = useState('');
  const [approach, setApproach] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    getProblem(slug).then((p) => {
      setProblem(p);
      setSolved(!!p.solved);
      setCode(p.starterCodeJs);
    }).catch((e) => setError(e.message));
  }, [slug]);

  useEffect(() => {
    if (!user) { setHistory([]); return; }
    getMySubmissions(slug).then(setHistory).catch(() => {});
  }, [slug, user]);

  function handleLanguageChange(lang) {
    setLanguage(lang);
    if (!problem) return;
    setCode(lang === 'python' ? problem.starterCodePython : problem.starterCodeJs);
  }

  async function handleSubmit() {
    if (!user) {
      setError('Log in to submit a solution.');
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await submitSolution(slug, { code, language, approach });
      setResult(res);
      if (res.solved) setSolved(true);
      getMySubmissions(slug).then(setHistory).catch(() => {});
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  if (error && !problem) return <div className="page"><p className="error">{error}</p></div>;
  if (!problem) return <div className="page">Loading…</div>;

  return (
    <div className="problem-page">
      <section className="problem-panel">
        <h1>
          {problem.name}
          {solved && <span className="solved-pill" title="You've solved this problem">✓ Solved</span>}
        </h1>
        <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span>
        <p className="description">{problem.description}</p>

        {problem.testCases && problem.testCases.length > 0 && (
          <div className="testcases-section">
            <h3>Test cases</h3>
            <ul className="testcase-list">
              {problem.testCases.map((tc, i) => (
                <li key={i} className="testcase-item">
                  <div className="testcase-row">
                    <span className="testcase-label">Input</span>
                    <code>{tc.input}</code>
                  </div>
                  <div className="testcase-row">
                    <span className="testcase-label">Expected</span>
                    <code>{tc.expectedOutput}</code>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        <h3>Your approach</h3>
        <textarea
          className="approach-box"
          placeholder="Explain your approach in plain English before you code — the AI coach reads this."
          value={approach}
          onChange={(e) => setApproach(e.target.value)}
          rows={5}
        />

        {user && history.length > 0 && (
          <div className="history-section">
            <button className="link-btn" onClick={() => setShowHistory((s) => !s)}>
              {showHistory ? 'Hide' : 'Show'} your submissions ({history.length})
            </button>
            {showHistory && (
              <ul className="history-list">
                {history.map((h) => (
                  <li key={h.id} className={`history-item ${h.solved ? 'test-pass' : 'test-fail'}`}>
                    <span>{h.solved ? '✓ Solved' : `${h.passedCount}/${h.totalCount} passed`}</span>
                    <span className="hint-text"> · {h.language} · {new Date(h.createdAt).toLocaleString()}</span>
                    {h.markedBy && <span className="hint-text"> · reviewed by {h.markedBy}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </section>

      <section className="editor-panel">
        <div className="editor-toolbar">
          <select value={language} onChange={(e) => handleLanguageChange(e.target.value)}>
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>
          {user ? (
            <button onClick={handleSubmit} disabled={loading} className="submit-btn">
              {loading ? 'Running…' : 'Submit'}
            </button>
          ) : (
            <a href={googleLoginUrl()} className="submit-btn">Log in to submit</a>
          )}
        </div>
        <Editor
          height="420px"
          language={language === 'python' ? 'python' : 'javascript'}
          theme="vs-dark"
          value={code}
          onChange={(v) => setCode(v ?? '')}
        />

        {error && <p className="error">{error}</p>}

        {result && (
          <div className="result-panel">
            <h3 className={result.allPassed ? 'verdict-pass' : 'verdict-fail'}>
              {result.allPassed ? 'Accepted' : 'Not quite'} — {result.passedCount}/{result.totalCount} tests passed
            </h3>

            <ul className="test-results">
              {result.results.map((r, i) => (
                <li key={i} className={r.passed ? 'test-pass' : 'test-fail'}>
                  <span>{r.passed ? '✓' : '✗'} Test {i + 1}</span>
                  {!r.passed && (
                    <div className="test-detail">
                      <div>input: {r.input}</div>
                      <div>expected: {r.expected}</div>
                      <div>actual: {r.actual ?? '(none)'}</div>
                      {r.stderr && <div className="stderr">{r.stderr}</div>}
                    </div>
                  )}
                </li>
              ))}
            </ul>

            <h3>AI coach feedback</h3>
            <div className="ai-feedback">{result.aiFeedback}</div>
          </div>
        )}
      </section>
    </div>
  );
}
