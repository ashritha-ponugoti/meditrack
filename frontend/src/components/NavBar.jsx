import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/stock-intake', label: 'Add Stock' },
  { to: '/dispense', label: 'Dispense' },
];

export default function NavBar({ onLogout }) {
  const location = useLocation();

  return (
    <motion.nav
      className="navbar"
      initial={{ y: -40, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
    >
      <Link to="/dashboard" className="brand">MediTrack</Link>

      {links.map((l) => (
        <Link key={l.to} to={l.to} className="nav-link">
          {l.label}
          {location.pathname === l.to && (
            <motion.span className="nav-underline" layoutId="nav-underline" />
          )}
        </Link>
      ))}

      <motion.button
        className="logout-btn"
        onClick={onLogout}
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
      >
        Logout
      </motion.button>
    </motion.nav>
  );
}
