const BASE_URL = 'http://localhost:8080/api';
const TOKEN_KEY = 'leetai_token';

function authHeaders() {
  const token = localStorage.getItem(TOKEN_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function handle(res, fallbackMessage) {
  if (res.status === 401) {
    // Token missing/expired — clear it so the UI falls back to logged-out state.
    localStorage.removeItem(TOKEN_KEY);
    throw new Error('Please log in and try again.');
  }
  if (res.status === 403) {
    throw new Error("You don't have permission to do that.");
  }
  if (res.status === 429) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message || 'Too many submissions — please slow down.');
  }
  if (!res.ok) throw new Error(fallbackMessage);
  if (res.status === 204) return null;
  return res.json();
}

// --- Public ---

export async function listProblems({ search = '', difficulty = 'ALL', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page, size });
  if (search) params.set('search', search);
  if (difficulty && difficulty !== 'ALL') params.set('difficulty', difficulty);
  const res = await fetch(`${BASE_URL}/problems?${params}`, { headers: authHeaders() });
  return handle(res, 'Failed to load problems');
}

export async function getProblem(slug) {
  const res = await fetch(`${BASE_URL}/problems/${slug}`, { headers: authHeaders() });
  return handle(res, 'Failed to load problem');
}

export async function submitSolution(slug, { code, language, approach }) {
  const res = await fetch(`${BASE_URL}/problems/${slug}/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ code, language, approach })
  });
  return handle(res, 'Submission failed');
}

export async function getMySubmissions(slug) {
  const res = await fetch(`${BASE_URL}/problems/${slug}/submissions`, { headers: authHeaders() });
  return handle(res, 'Failed to load your submissions');
}

export async function getProgress() {
  const res = await fetch(`${BASE_URL}/problems/progress`, { headers: authHeaders() });
  return handle(res, 'Failed to load progress');
}

// --- Auth ---

export function googleLoginUrl() {
  return 'http://localhost:8080/oauth2/authorization/google';
}

export async function fetchMe(token) {
  const res = await fetch(`${BASE_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return handle(res, 'Failed to load current user');
}

// --- Admin ---

export async function adminListProblems() {
  const res = await fetch(`${BASE_URL}/admin/problems`, { headers: authHeaders() });
  return handle(res, 'Failed to load problems');
}

export async function adminGetProblem(slug) {
  const res = await fetch(`${BASE_URL}/admin/problems/${slug}`, { headers: authHeaders() });
  return handle(res, 'Failed to load problem');
}

export async function adminCreateProblem(payload) {
  const res = await fetch(`${BASE_URL}/problems`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload)
  });
  return handle(res, 'Failed to create problem');
}

export async function adminUpdateProblem(slug, payload) {
  const res = await fetch(`${BASE_URL}/problems/${slug}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload)
  });
  return handle(res, 'Failed to update problem');
}

export async function adminDeleteProblem(slug) {
  const res = await fetch(`${BASE_URL}/problems/${slug}`, {
    method: 'DELETE',
    headers: authHeaders()
  });
  return handle(res, 'Failed to delete problem');
}

export async function adminPublishProblem(slug) {
  const res = await fetch(`${BASE_URL}/problems/${slug}/publish`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  return handle(res, 'Failed to publish problem');
}

export async function adminUnpublishProblem(slug) {
  const res = await fetch(`${BASE_URL}/problems/${slug}/unpublish`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  return handle(res, 'Failed to unpublish problem');
}

export async function adminTestRun(slug, { code, language }) {
  const res = await fetch(`${BASE_URL}/admin/problems/${slug}/test-run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ code, language })
  });
  return handle(res, 'Test run failed');
}

// --- Admin: submissions ---

export async function adminListSubmissions({ slug = '', email = '', page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({ page, size });
  if (slug) params.set('slug', slug);
  if (email) params.set('email', email);
  const res = await fetch(`${BASE_URL}/admin/submissions?${params}`, { headers: authHeaders() });
  return handle(res, 'Failed to load submissions');
}

export async function adminMarkSolved(id) {
  const res = await fetch(`${BASE_URL}/admin/submissions/${id}/mark-solved`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  return handle(res, 'Failed to mark submission as solved');
}

export async function adminUnmarkSolved(id) {
  const res = await fetch(`${BASE_URL}/admin/submissions/${id}/unmark-solved`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  return handle(res, 'Failed to unmark submission');
}

// --- Admin: solved users (reset feature, keyed by user + problem rather
// than a single submission — a user can have multiple passing submissions
// for the same problem) ---

export async function adminGetSolvedUsers(slug) {
  const res = await fetch(`${BASE_URL}/admin/problems/${slug}/solved-users`, { headers: authHeaders() });
  return handle(res, 'Failed to load solved users');
}

export async function adminResetSolved(slug, userId) {
  const res = await fetch(`${BASE_URL}/admin/problems/${slug}/users/${userId}/reset-solved`, {
    method: 'POST',
    headers: authHeaders()
  });
  return handle(res, 'Failed to reset solved status');
}

export async function adminMarkUserSolved(slug, userId) {
  const res = await fetch(`${BASE_URL}/admin/problems/${slug}/users/${userId}/mark-solved`, {
    method: 'POST',
    headers: authHeaders()
  });
  return handle(res, 'Failed to mark user as solved');
}
