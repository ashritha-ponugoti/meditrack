import { useState } from 'react';
import client from '../api/client.js';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('owner');
  const [password, setPassword] = useState('owner123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await client.post('/api/auth/login', { username, password });
      onLogin(res.data.token);
    } catch (err) {
      setError('Login failed. Check your username and password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="centered">
      <form className="card" onSubmit={handleSubmit}>
        <h1>MediTrack</h1>
        <p className="subtitle">Smart Medicine Inventory & Expiry Management</p>

        <label>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} />

        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={loading}>{loading ? 'Logging in...' : 'Login'}</button>

        <p className="hint">Demo accounts: owner / owner123  or  pharmacist / pharma123</p>
      </form>
    </div>
  );
}
