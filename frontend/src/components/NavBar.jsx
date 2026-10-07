import { Link } from 'react-router-dom';

export default function NavBar({ onLogout }) {
  return (
    <nav className="navbar">
      <span className="brand">MediTrack</span>
      <Link to="/">Dashboard</Link>
      <Link to="/stock-intake">Add Stock</Link>
      <Link to="/dispense">Dispense</Link>
      <button className="logout-btn" onClick={onLogout}>Logout</button>
    </nav>
  );
}
