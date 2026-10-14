import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import PageTransition from '../components/PageTransition.jsx';
import AuthModal from '../components/AuthModal.jsx';

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.12, delayChildren: 0.1 } },
};

const item = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] } },
};

const features = [
  {
    title: 'FEFO Dispensing',
    desc: 'Automatically dispenses the batch nearest to expiry first — no manual batch selection.',
    icon: '⏱',
  },
  {
    title: 'Expiry Risk Estimation',
    desc: 'Projects which batches will expire unsold, based on real usage rate, not static reminders.',
    icon: '📉',
  },
  {
    title: 'Anomaly Detection',
    desc: 'Flags unusual dispensing transactions using statistical analysis of historical patterns.',
    icon: '⚠',
  },
  {
    title: 'Inter-Pharmacy Transfers',
    desc: 'Matches surplus stock with nearby pharmacies running low, cutting down waste.',
    icon: '🔄',
  },
];

// authMode: 'login' or 'register' opens the popup immediately (used by the /login and /register routes).
export default function Landing({ token, authMode, onAuthenticated, onLogout }) {
  const navigate = useNavigate();
  const [modalTab, setModalTab] = useState(authMode || null); // null = popup closed

  function openAuth(tab) {
    setModalTab(tab);
  }

  function closeAuth() {
    setModalTab(null);
    if (authMode) navigate('/', { replace: true }); // leave the /login or /register address
  }

  function handleAuthenticated(newToken, pharmacyId) {
    onAuthenticated(newToken, pharmacyId);
    setModalTab(null);
    navigate('/dashboard');
  }

  return (
    <PageTransition>
      <div className="landing">
        <div className="landing-bg">
          <div className="blob blob-1" />
          <div className="blob blob-2" />
          <div className="blob blob-3" />
        </div>

        <nav className="landing-nav">
          <motion.span
            className="brand"
            initial={{ opacity: 0, x: -16 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5 }}
          >
            MediTrack
          </motion.span>
          <motion.div
            initial={{ opacity: 0, x: 16 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5 }}
          >
            <div className="nav-actions">
              {token ? (
                <>
                  <Link to="/dashboard" className="nav-login-link">Dashboard →</Link>
                  <button type="button" className="nav-login-link" onClick={onLogout}>Logout</button>
                </>
              ) : (
                <>
                  <button type="button" className="nav-login-link" onClick={() => openAuth('login')}>Login</button>
                  <button type="button" className="nav-register-btn" onClick={() => openAuth('register')}>Register</button>
                </>
              )}
            </div>
          </motion.div>
        </nav>

        <motion.div
          className="hero"
          variants={container}
          initial="hidden"
          animate="show"
        >
          <motion.span className="eyebrow" variants={item}>
            Smart Medicine Inventory & Expiry Management
          </motion.span>

          <motion.h1 variants={item}>
            Stop losing medicine
            <br />
            <span className="gradient-text">to expiry waste.</span>
          </motion.h1>

          <motion.p className="hero-sub" variants={item}>
            MediTrack helps small pharmacies dispense the right batch first, catch
            expiring stock before it's too late, and move surplus medicine to
            where it's actually needed.
          </motion.p>

          <motion.div className="hero-actions" variants={item}>
            <motion.button
              className="primary-btn"
              whileHover={{ scale: 1.04 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => (token ? navigate('/dashboard') : openAuth('login'))}
            >
              {token ? 'Open Dashboard' : 'Try the Demo'}
            </motion.button>
            <motion.a
              href="#features"
              className="secondary-link"
              whileHover={{ x: 4 }}
            >
              See how it works ↓
            </motion.a>
          </motion.div>
        </motion.div>

        <motion.div
          id="features"
          className="feature-grid"
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.3 }}
          variants={container}
        >
          {features.map((f) => (
            <motion.div key={f.title} className="feature-card" variants={item}
              whileHover={{ y: -6, boxShadow: '0 20px 40px rgba(0,0,0,0.12)' }}>
              <div className="feature-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </motion.div>
          ))}
        </motion.div>

        <motion.footer
          className="landing-footer"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <p>Built as a Java full-stack technical training project.</p>
        </motion.footer>

        <AuthModal
          open={modalTab !== null}
          initialTab={modalTab || 'login'}
          onClose={closeAuth}
          onAuthenticated={handleAuthenticated}
        />
      </div>
    </PageTransition>
  );
}
