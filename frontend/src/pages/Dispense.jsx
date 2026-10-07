import { useState } from 'react';
import client from '../api/client.js';

export default function Dispense() {
  const [medicineName, setMedicineName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setResult(null);
    try {
      const res = await client.post('/api/dispense', { medicineName, quantity: Number(quantity) });
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Dispense failed - check medicine name and available stock.');
    }
  }

  return (
    <div className="card">
      <h2>Dispense Medicine</h2>
      <p className="subtitle">The system automatically picks the batch with the earliest expiry date (FEFO).</p>

      <form onSubmit={handleSubmit} className="form-grid">
        <label>Medicine Name</label>
        <input value={medicineName} onChange={(e) => setMedicineName(e.target.value)} required />

        <label>Quantity</label>
        <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />

        <button type="submit">Dispense</button>
      </form>

      {error && <p className="error">{error}</p>}

      {result && (
        <div className="result-box">
          <h3>Dispensed from:</h3>
          <ul>
            {result.deductions.map((d, i) => (
              <li key={i}>Batch {d.batchNumber}: {d.quantityTaken} units</li>
            ))}
          </ul>
          {result.anomalyFlagged && (
            <p className="warning">⚠ Unusual dispensing pattern detected (Z-score: {result.anomalyScore?.toFixed(2)}) — flagged for review.</p>
          )}
        </div>
      )}
    </div>
  );
}
