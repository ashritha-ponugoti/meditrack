import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import client from '../api/client.js';

const EMPTY = { username: '', password: '', confirm: '', pharmacyName: '', city: '' };

/**
 * Login / Register popup. It talks to the backend itself and, once the server has
 * accepted the credentials, hands the token to the parent through onAuthenticated.
 */
export default function AuthModal({ open, initialTab = 'login', onClose, onAuthenticated }) {
  const [tab, setTab] = useState(initialTab);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Start from a clean form every time the popup opens.
  useEffect(() => {
    if (open) {
      setTab(initialTab);
      setForm(EMPTY);
      setError('');
      setSuccess('');
      setLoading(false);
      setShowPassword(false);
    }
  }, [open, initialTab]);

  // Close on Escape and stop the page behind from scrolling while the popup is open.
  useEffect(() => {
    if (!open) return undefined;
    function onKey(e) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', onKey);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function switchTab(next) {
    setTab(next);
    setError('');
  }

  function fillDemo(role) {
    setForm((f) => ({
      ...f,
      username: role === 'owner' ? 'owner' : 'pharmacist',
      password: role === 'owner' ? 'owner123' : 'pharma123',
    }));
    setError('');
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (loading) return;
    setError('');

    const username = form.username.trim().toLowerCase();

    // Quick checks for a faster answer. The server repeats every one of these rules.
    if (tab === 'register') {
      if (!/^[a-z0-9_]{3,30}$/.test(username)) {
        setError('Username must be 3 to 30 characters: letters, numbers or underscore.');
        return;
      }
      if (form.pharmacyName.trim().length < 2) {
        setError('Enter your pharmacy name.');
        return;
      }
      if (form.password.length < 8) {
        setError('Password must be at least 8 characters.');
        return;
      }
      if (form.password !== form.confirm) {
        setError('The two passwords do not match.');
        return;
      }
    }

    setLoading(true);
    try {
      const res = tab === 'login'
        ? await client.post('/api/auth/login', { username, password: form.password })
        : await client.post('/api/auth/register', {
            username,
            password: form.password,
            pharmacyName: form.pharmacyName.trim(),
            city: form.city.trim(),
          });

      setSuccess(tab === 'login'
        ? `Welcome back, ${res.data.username}`
        : `Account created. Welcome, ${res.data.username}`);
      // A short pause so the confirmation is actually visible before moving on.
      setTimeout(() => onAuthenticated(res.data.token, res.data.pharmacyId), 800);
    } catch (err) {
      if (!err.response) {
        setError('Cannot reach the server. If it is hosted on a free plan it may be waking up - wait a minute and try again.');
      } else {
        setError(err.response.data?.message
          || (tab === 'login' ? 'Login failed. Check your username and password.' : 'Could not create the account.'));
      }
      setLoading(false);
    }
  }

  const isLogin = tab === 'login';

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="auth-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}
        >
          <motion.div
            className="auth-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 12, scale: 0.97 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            <button type="button" className="auth-close" aria-label="Close" onClick={onClose}>&times;</button>

            {success ? (
              <div className="auth-success">
                <div className="success-check">&#10003;</div>
                <p>{success}</p>
              </div>
            ) : (
              <>
                <h2 id="auth-title">MediTrack</h2>
                <p className="subtitle">Smart Medicine Inventory &amp; Expiry Management</p>

                <div className="auth-tabs" role="tablist">
                  <button type="button" role="tab" aria-selected={isLogin}
                    className={isLogin ? 'auth-tab active' : 'auth-tab'} onClick={() => switchTab('login')}>
                    Login
                  </button>
                  <button type="button" role="tab" aria-selected={!isLogin}
                    className={!isLogin ? 'auth-tab active' : 'auth-tab'} onClick={() => switchTab('register')}>
                    Register
                  </button>
                </div>

                <form onSubmit={handleSubmit} noValidate>
                  {!isLogin && (
                    <>
                      <label>Pharmacy name</label>
                      <input value={form.pharmacyName} onChange={(e) => update('pharmacyName', e.target.value)}
                        maxLength={60} autoFocus />

                      <label>City (optional)</label>
                      <input value={form.city} onChange={(e) => update('city', e.target.value)} maxLength={40} />
                    </>
                  )}

                  <label>Username</label>
                  <input value={form.username} onChange={(e) => update('username', e.target.value)}
                    autoComplete="username" autoCapitalize="none" maxLength={30} autoFocus={isLogin} />

                  <label>Password</label>
                  <div className="auth-password-row">
                    <input type={showPassword ? 'text' : 'password'} value={form.password}
                      onChange={(e) => update('password', e.target.value)}
                      autoComplete={isLogin ? 'current-password' : 'new-password'} maxLength={64} />
                    <button type="button" className="auth-toggle" onClick={() => setShowPassword((s) => !s)}>
                      {showPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>

                  {!isLogin && (
                    <>
                      <label>Confirm password</label>
                      <input type={showPassword ? 'text' : 'password'} value={form.confirm}
                        onChange={(e) => update('confirm', e.target.value)} autoComplete="new-password" maxLength={64} />
                      <p className="hint">At least 8 characters. Registering creates your own pharmacy with you as its owner.</p>
                    </>
                  )}

                  {error && <p className="error" role="alert">{error}</p>}

                  <button type="submit" className="auth-submit"
                    disabled={loading || !form.username || !form.password}>
                    {loading ? (isLogin ? 'Logging in...' : 'Creating account...') : (isLogin ? 'Login' : 'Create account')}
                  </button>
                </form>

                {isLogin && (
                  <div className="auth-demo">
                    <span className="hint">Demo access:</span>
                    <button type="button" className="demo-fill-btn" onClick={() => fillDemo('owner')}>Fill as Owner</button>
                    <button type="button" className="demo-fill-btn" onClick={() => fillDemo('pharmacist')}>Fill as Pharmacist</button>
                  </div>
                )}

                <p className="auth-switch">
                  {isLogin ? 'New here? ' : 'Already have an account? '}
                  <button type="button" className="auth-link" onClick={() => switchTab(isLogin ? 'register' : 'login')}>
                    {isLogin ? 'Create an account' : 'Login'}
                  </button>
                </p>
              </>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
