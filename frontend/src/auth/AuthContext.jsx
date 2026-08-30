import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { fetchMe } from '../api/client.js';

const AuthContext = createContext(null);

const TOKEN_KEY = 'leetai_token';

export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    // base64url -> base64, then restore the padding JWTs strip per spec —
    // atob() throws on unpadded input, which was silently discarding valid
    // tokens right after login.
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4 !== 0) base64 += '=';
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const clearAuth = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const login = useCallback((newToken) => {
    const claims = decodeJwt(newToken);
    if (!claims || claims.exp * 1000 < Date.now()) {
      clearAuth();
      return;
    }
    localStorage.setItem(TOKEN_KEY, newToken);
    setToken(newToken);
  }, [clearAuth]);

  const logout = useCallback(() => {
    clearAuth();
  }, [clearAuth]);

  useEffect(() => {
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    const claims = decodeJwt(token);
    if (!claims || claims.exp * 1000 < Date.now()) {
      clearAuth();
      setLoading(false);
      return;
    }
    fetchMe(token)
      .then(setUser)
      .catch(() => clearAuth())
      .finally(() => setLoading(false));
  }, [token, clearAuth]);

  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ token, user, isAdmin, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
