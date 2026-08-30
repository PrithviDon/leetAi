import { googleLoginUrl } from '../api/client.js';

/**
 * Both buttons hit the exact same Google login endpoint — this is presentation
 * only. The backend (CustomOAuth2UserService) is the only thing that decides
 * whether someone is actually an admin, based on ADMIN_EMAILS. Clicking
 * "Continue as Admin" without being on that list just lands you in the
 * regular user view — it can't grant admin access.
 */
export default function LoginChoice() {
  return (
    <div className="page login-choice-page">
      <h1 className="page-title">Welcome to LeetAI</h1>
      <p className="hint-text">Sign in to get started.</p>

      <div className="login-choice-cards">
        <a href={googleLoginUrl()} className="login-choice-card">
          <h3>Continue as User</h3>
          <p>Solve problems, get AI feedback on your approach and code.</p>
          <span className="submit-btn">Log in with Google</span>
        </a>

        <a href={googleLoginUrl()} className="login-choice-card">
          <h3>Continue as Admin</h3>
          <p>Create, edit, and publish problems.</p>
          <span className="submit-btn">Log in with Google</span>
        </a>
      </div>

      <p className="hint-text login-choice-footnote">
        Your actual access level is determined by your account, not by which
        button you click — only approved admin accounts get admin access.
      </p>
    </div>
  );
}
