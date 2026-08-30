import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth, decodeJwt } from '../auth/AuthContext.jsx';

export default function OAuth2RedirectPage() {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      login(token);
      // Role is decided entirely by the backend (ADMIN_EMAILS allowlist) —
      // this just routes to the right landing page based on that decision,
      // it doesn't grant anything.
      const claims = decodeJwt(token);
      navigate(claims?.role === 'ADMIN' ? '/admin/problems' : '/', { replace: true });
    } else {
      navigate('/?loginError=1', { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div className="page">Signing you in…</div>;
}
