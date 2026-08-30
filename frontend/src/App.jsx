import { Routes, Route, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext.jsx';
import RequireAdmin from './auth/RequireAdmin.jsx';
import Home from './pages/Home.jsx';
import ProblemPage from './pages/ProblemPage.jsx';
import OAuth2RedirectPage from './pages/OAuth2RedirectPage.jsx';
import AdminProblemList from './pages/admin/AdminProblemList.jsx';
import AdminProblemForm from './pages/admin/AdminProblemForm.jsx';
import AdminSubmissions from './pages/admin/AdminSubmissions.jsx';
import { googleLoginUrl } from './api/client.js';

function Header() {
  const { user, isAdmin, logout, loading } = useAuth();

  return (
    <header className="topbar">
      <Link to="/" className="brand">
        <span className="brand-mark">{'</>'}</span> LeetAI
      </Link>
      <div className="topbar-actions">
        {!loading && isAdmin && <Link to="/admin/problems" className="admin-link">Admin</Link>}
        {!loading && isAdmin && <Link to="/admin/submissions" className="admin-link">Submissions</Link>}
        {!loading && user && (
          <span className="user-chip">
            {user.avatarUrl && <img src={user.avatarUrl} alt="" className="avatar" />}
            {user.name || user.email}
          </span>
        )}
        {!loading && user && <button className="link-btn" onClick={logout}>Log out</button>}
        {!loading && !user && <a href={googleLoginUrl()} className="submit-btn">Log in with Google</a>}
      </div>
    </header>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <div className="app-shell">
        <Header />
        <main>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/problems/:slug" element={<ProblemPage />} />
            <Route path="/oauth2/redirect" element={<OAuth2RedirectPage />} />
            <Route path="/admin/problems" element={<RequireAdmin><AdminProblemList /></RequireAdmin>} />
            <Route path="/admin/problems/new" element={<RequireAdmin><AdminProblemForm /></RequireAdmin>} />
            <Route path="/admin/problems/:slug/edit" element={<RequireAdmin><AdminProblemForm /></RequireAdmin>} />
            <Route path="/admin/submissions" element={<RequireAdmin><AdminSubmissions /></RequireAdmin>} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
  );
}
