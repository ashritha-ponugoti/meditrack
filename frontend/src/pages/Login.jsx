import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import client from '../api/client.js';
import PageTransition from '../components/PageTransition.jsx';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  function fillDemo(role) {
    if (role === 'owner') {
      setUsername('owner');
      setPassword('owner123');
    } else {
      setUsername('pharmacist');
      setPassword('pharma123');
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await client.post('/api/auth/login', { username, password });
      setSuccess(true);
      // Brief, real pause so the success state is actually visible before
      // handing off to the dashboard - an instant jump reads as fake.
      setTimeout(() => onLogin(res.data.token, res.data.pharmacyId), 900);
    } catch (err) {
      setError('Login failed. Check your username and password.');
      setLoading(false);
    }
  }

  return (
    <PageTransition>
      <div className="centered">
        <div className="auth-bg">
          <div className="blob blob-1" />
          <div className="blob blob-2" />
        </div>

        <AnimatePresence mode="wait">
          {success ? (
            <motion.div
              key="success"
              className="login-success"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <motion.div
                className="success-check"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 260, damping: 18 }}
              >
                ✓
              </motion.div>
              <motion.p
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.15 }}
              >
                Welcome back, {username}
              </motion.p>
            </motion.div>
          ) : (
            <motion.form
              key="form"
              className="card auth-card"
              onSubmit={handleSubmit}
              initial={{ opacity: 0, y: 24, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.97 }}
              transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
            >
              <Link to="/" className="back-link">← Back</Link>
              <h1>MediTrack</h1>
              <p className="subtitle">Smart Medicine Inventory & Expiry Management</p>

              <label>Username</label>
              <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />

              <label>Password</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

              {error && (
                <motion.p
                  className="error"
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                >
                  {error}
                </motion.p>
              )}

              <motion.button
                type="submit"
                disabled={loading || !username || !password}
                whileHover={{ scale: loading ? 1 : 1.02 }}
                whileTap={{ scale: loading ? 1 : 0.98 }}
              >
                {loading ? 'Logging in...' : 'Login'}
              </motion.button>

              <div className="hint" style={{ marginTop: '0.9rem' }}>
                Demo access:
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.4rem' }}>
                  <button type="button" className="demo-fill-btn" onClick={() => fillDemo('owner')}>
                    Fill as Owner
                  </button>
                  <button type="button" className="demo-fill-btn" onClick={() => fillDemo('pharmacist')}>
                    Fill as Pharmacist
                  </button>
                </div>
              </div>
            </motion.form>
          )}
        </AnimatePresence>
      </div>
    </PageTransition>
  );
}
