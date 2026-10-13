import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { useState } from 'react';
import Landing from './pages/Landing.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import StockIntake from './pages/StockIntake.jsx';
import Dispense from './pages/Dispense.jsx';
import NavBar from './components/NavBar.jsx';
import PageTransition from './components/PageTransition.jsx';

function ProtectedLayout({ token, onLogout, children }) {
  if (!token) return <Navigate to="/login" replace />;
  return (
    <div>
      <NavBar onLogout={onLogout} />
      <div className="page">
        <PageTransition>{children}</PageTransition>
      </div>
    </div>
  );
}

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('meditrack_token'));
  const [pharmacyId, setPharmacyId] = useState(localStorage.getItem('meditrack_pharmacy_id'));
  const location = useLocation();

  function handleLogin(newToken, newPharmacyId) {
    localStorage.setItem('meditrack_token', newToken);
    if (newPharmacyId != null) {
      localStorage.setItem('meditrack_pharmacy_id', newPharmacyId);
      setPharmacyId(newPharmacyId);
    }
    setToken(newToken);
  }

  function handleLogout() {
    localStorage.removeItem('meditrack_token');
    localStorage.removeItem('meditrack_pharmacy_id');
    setToken(null);
    setPharmacyId(null);
  }

  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route path="/" element={<Landing />} />
        <Route
          path="/login"
          element={token ? <Navigate to="/dashboard" replace /> : <Login onLogin={handleLogin} />}
        />
        <Route
          path="/dashboard"
          element={
            <ProtectedLayout token={token} onLogout={handleLogout}>
              <Dashboard pharmacyId={pharmacyId} />
            </ProtectedLayout>
          }
        />
        <Route
          path="/stock-intake"
          element={<ProtectedLayout token={token} onLogout={handleLogout}><StockIntake /></ProtectedLayout>}
        />
        <Route
          path="/dispense"
          element={<ProtectedLayout token={token} onLogout={handleLogout}><Dispense /></ProtectedLayout>}
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AnimatePresence>
  );
}
