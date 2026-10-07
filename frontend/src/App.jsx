import { Routes, Route, Navigate } from 'react-router-dom';
import { useState } from 'react';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import StockIntake from './pages/StockIntake.jsx';
import Dispense from './pages/Dispense.jsx';
import NavBar from './components/NavBar.jsx';

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('meditrack_token'));

  function handleLogin(newToken) {
    localStorage.setItem('meditrack_token', newToken);
    setToken(newToken);
  }

  function handleLogout() {
    localStorage.removeItem('meditrack_token');
    setToken(null);
  }

  if (!token) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div>
      <NavBar onLogout={handleLogout} />
      <div className="page">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/stock-intake" element={<StockIntake />} />
          <Route path="/dispense" element={<Dispense />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </div>
    </div>
  );
}
