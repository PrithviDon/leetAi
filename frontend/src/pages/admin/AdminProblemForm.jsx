import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import {
  adminGetProblem, adminCreateProblem, adminUpdateProblem, adminTestRun
} from '../../api/client.js';

const emptyTestCase = () => ({ input: '', expectedOutput: '', hidden: false });

export default function AdminProblemForm() {
  const { slug } = useParams();
  const isEdit = Boolean(slug);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '', description: '', difficulty: 'MEDIUM', functionName: '',
    starterCodeJs: '', starterCodePython: ''
  });
  const [testCases, setTestCases] = useState([emptyTestCase()]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState(null);

  // Test-run scratch state (edit mode only — needs a saved slug to call the endpoint)
  const [refLanguage, setRefLanguage] = useState('javascript');
  const [refCode, setRefCode] = useState('');
  const [testRunResult, setTestRunResult] = useState(null);
  const [testRunning, setTestRunning] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    adminGetProblem(slug).then((p) => {
      setForm({
        name: p.name, description: p.description, difficulty: p.difficulty,
        functionName: p.functionName, starterCodeJs: p.starterCodeJs || '',
        starterCodePython: p.starterCodePython || ''
      });
      setTestCases(p.testCases?.length ? p.testCases.map(tc => ({ ...tc, hidden: tc.hidden ?? false })) : [emptyTestCase()]);
      setRefCode(p.starterCodeJs || '');
      setStatus(p.status);
    }).catch((e) => setError(e.message));
  }, [slug, isEdit]);

  function updateField(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function updateTestCase(i, field, value) {
    setTestCases((tcs) => tcs.map((tc, idx) => idx === i ? { ...tc, [field]: value } : tc));
  }

  function addTestCase() {
    setTestCases((tcs) => [...tcs, emptyTestCase()]);
  }

  function removeTestCase(i) {
    setTestCases((tcs) => tcs.filter((_, idx) => idx !== i));
  }

  async function handleSave(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const payload = { ...form, testCases };
      if (isEdit) {
        await adminUpdateProblem(slug, payload);
        navigate('/admin/problems');
      } else {
        const created = await adminCreateProblem(payload);
        navigate(`/admin/problems/${created.slug}/edit`);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleTestRun() {
    setTestRunning(true);
    setTestRunResult(null);
    try {
      const result = await adminTestRun(slug, { code: refCode, language: refLanguage });
      setTestRunResult(result);
    } catch (e) {
      setError(e.message);
    } finally {
      setTestRunning(false);
    }
  }

  return (
    <div className="page admin-form-page">
      <h1 className="page-title">{isEdit ? `Edit: ${form.name}` : 'New Problem'}</h1>
      {status && <span className={`badge status-${status.toLowerCase()}`}>{status}</span>}
      {error && <p className="error">{error}</p>}

      <form onSubmit={handleSave} className="admin-form">
        <label>Name
          <input value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
        </label>

        <label>Description
          <textarea rows={5} value={form.description} onChange={(e) => updateField('description', e.target.value)} required />
        </label>

        <label>Difficulty
          <select value={form.difficulty} onChange={(e) => updateField('difficulty', e.target.value)}>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
        </label>

        <label>Function name (must match the function users implement)
          <input value={form.functionName} onChange={(e) => updateField('functionName', e.target.value)} required />
        </label>

        <label>Starter code — JavaScript
          <textarea rows={4} className="code-textarea" value={form.starterCodeJs}
            onChange={(e) => updateField('starterCodeJs', e.target.value)} />
        </label>

        <label>Starter code — Python
          <textarea rows={4} className="code-textarea" value={form.starterCodePython}
            onChange={(e) => updateField('starterCodePython', e.target.value)} />
        </label>

        <h3>Test cases</h3>
        {testCases.map((tc, i) => (
          <div key={i} className="admin-testcase-row">
            <input placeholder='Input, e.g. [[2,7,11,15], 9]' value={tc.input}
              onChange={(e) => updateTestCase(i, 'input', e.target.value)} required />
            <input placeholder='Expected output, e.g. [0,1]' value={tc.expectedOutput}
              onChange={(e) => updateTestCase(i, 'expectedOutput', e.target.value)} required />
            <label className="hidden-checkbox">
              <input type="checkbox" checked={tc.hidden}
                onChange={(e) => updateTestCase(i, 'hidden', e.target.checked)} />
              Hidden
            </label>
            <button type="button" className="link-btn danger" onClick={() => removeTestCase(i)}>Remove</button>
          </div>
        ))}
        <button type="button" className="link-btn" onClick={addTestCase}>+ Add test case</button>

        <div className="admin-form-actions">
          <button type="submit" className="submit-btn" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create as draft'}
          </button>
        </div>
      </form>

      {isEdit && (
        <section className="admin-testrun-section">
          <h3>Validate test cases</h3>
          <p className="hint-text">
            Run a reference solution through the same Docker sandbox real submissions use,
            against every test case (including hidden ones) — catches a broken expected
            output before you publish.
          </p>
          <div className="editor-toolbar">
            <select value={refLanguage} onChange={(e) => setRefLanguage(e.target.value)}>
              <option value="javascript">JavaScript</option>
              <option value="python">Python</option>
            </select>
            <button className="submit-btn" onClick={handleTestRun} disabled={testRunning}>
              {testRunning ? 'Running…' : 'Run tests'}
            </button>
          </div>
          <Editor
            height="260px"
            language={refLanguage === 'python' ? 'python' : 'javascript'}
            theme="vs-dark"
            value={refCode}
            onChange={(v) => setRefCode(v ?? '')}
          />

          {testRunResult && (
            <div className="result-panel">
              <h3 className={testRunResult.allPassed ? 'verdict-pass' : 'verdict-fail'}>
                {testRunResult.passedCount}/{testRunResult.totalCount} test cases passed
              </h3>
              <ul className="test-results">
                {testRunResult.results.map((r, i) => (
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
            </div>
          )}
        </section>
      )}
    </div>
  );
}
