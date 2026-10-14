import { useState, useEffect } from 'react';
import client from '../api/client.js';

// Dates normally arrive as "2027-02-28"; handle the [2027, 2, 28] array form too.
function formatDate(value) {
  if (!value) return '-';
  if (Array.isArray(value)) {
    const [y, m, d] = value;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }
  return String(value);
}

export default function Dispense() {
  const [medicineName, setMedicineName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [knownMedicines, setKnownMedicines] = useState([]);
  const [batchInfo, setBatchInfo] = useState(null);
  const [loadingBatches, setLoadingBatches] = useState(false);
  const [batchError, setBatchError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  // Medicine names that currently have stock (used for the search suggestions).
  async function loadMedicines() {
    try {
      const res = await client.get('/api/dispense/medicines');
      setKnownMedicines(res.data);
    } catch (err) {
      // Suggestions are optional - typing a name still works.
    }
  }

  useEffect(() => { loadMedicines(); }, []);

  // Whenever the medicine changes (or after a dispense), ask the backend for its eligible batches.
  useEffect(() => {
    let cancelled = false;
    const name = medicineName.trim();
    setBatchError('');

    if (!name) {
      setBatchInfo(null);
      setLoadingBatches(false);
      return undefined;
    }

    const timer = setTimeout(async () => {
      setLoadingBatches(true);
      try {
        const res = await client.get('/api/dispense/batches', { params: { medicine: name } });
        if (!cancelled) setBatchInfo(res.data);
      } catch (err) {
        if (!cancelled) {
          setBatchInfo(null);
          setBatchError(err.response?.data?.message || 'Could not load the batches for this medicine.');
        }
      } finally {
        if (!cancelled) setLoadingBatches(false);
      }
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [medicineName, refreshKey]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return;
    setError('');
    setResult(null);

    const qty = Number(quantity);
    if (!medicineName.trim()) {
      setError('Select or type a medicine name.');
      return;
    }
    if (!Number.isInteger(qty) || qty <= 0) {
      setError('Enter a whole number greater than zero.');
      return;
    }

    setSubmitting(true);
    try {
      // The backend decides which batches to use. The page only displays what it returns.
      const res = await client.post('/api/dispense', { medicineName: medicineName.trim(), quantity: qty });
      setResult(res.data);
      setQuantity('');
      loadMedicines();
    } catch (err) {
      setError(err.response?.data?.message || 'Dispense failed. Please try again.');
    } finally {
      setSubmitting(false);
      setRefreshKey((k) => k + 1); // always re-read the real stock afterwards
    }
  }

  const batches = batchInfo ? batchInfo.batches : [];
  const requested = Number(quantity);
  const tooMuch = batchInfo && batches.length > 0 && requested > batchInfo.totalAvailable;

  return (
    <div className="card">
      <h2>Smart Dispensing</h2>
      <p className="subtitle">
        Choose a medicine to see its available batches. The system dispenses First Expiry, First Out (FEFO):
        the earliest-expiring batch is used first, then the next one if more is needed.
      </p>

      <form onSubmit={handleSubmit} className="form-grid">
        <label>Medicine Name</label>
        <input
          list="known-medicines"
          value={medicineName}
          onChange={(e) => { setMedicineName(e.target.value); setResult(null); setError(''); }}
          placeholder="Start typing, e.g. Paracetamol 500mg"
          autoComplete="off"
        />
        <datalist id="known-medicines">
          {knownMedicines.map((name) => <option key={name} value={name} />)}
        </datalist>

        <label>Quantity</label>
        <input type="number" min="1" step="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} />

        {tooMuch && (
          <p className="warning">
            Only {batchInfo.totalAvailable} units are available in unexpired batches. Dispensing will be rejected.
          </p>
        )}

        <button type="submit" disabled={submitting || !medicineName.trim() || !quantity}>
          {submitting ? 'Dispensing...' : 'Dispense'}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      {result && (
        <div className="result-box">
          <h3>Dispensed successfully</h3>
          <p className="success-msg">
            {result.medicineName}: {result.totalDispensed} units dispensed.
          </p>
          <table>
            <thead>
              <tr><th>Batch used</th><th>Expiry date</th><th>Deducted</th><th>Remaining in batch</th></tr>
            </thead>
            <tbody>
              {result.deductions.map((d) => (
                <tr key={d.batchId ?? d.batchNumber}>
                  <td>{d.batchNumber}</td>
                  <td>{formatDate(d.expiryDate)}</td>
                  <td>{d.quantityTaken}</td>
                  <td>{d.remainingQuantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {result.anomalyFlagged && (
            <p className="warning">
              Unusual dispensing pattern detected (Z-score: {result.anomalyScore?.toFixed(2)}) - flagged for review.
            </p>
          )}
        </div>
      )}

      <h3 className="section-title">Available batches</h3>
      {!medicineName.trim() && <p className="hint">Enter a medicine name to see its batches.</p>}
      {medicineName.trim() && loadingBatches && <p className="hint">Loading batches...</p>}
      {batchError && <p className="error">{batchError}</p>}

      {batchInfo && batches.length === 0 && !loadingBatches && <p className="hint">{batchInfo.message}</p>}

      {batchInfo && batches.length > 0 && (
        <>
          <p className="recommend-msg">{batchInfo.message}</p>
          <table>
            <thead>
              <tr>
                <th>Batch</th><th>Manufacturer</th><th>Expiry date</th>
                <th>Days left</th><th>Available</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              {batches.map((b) => (
                <tr key={b.batchId} className={b.recommended ? 'recommended-row' : ''}>
                  <td>{b.batchNumber}</td>
                  <td>{b.manufacturer || '-'}</td>
                  <td>{formatDate(b.expiryDate)}</td>
                  <td>{b.daysToExpiry}</td>
                  <td>{b.quantity}</td>
                  <td>{b.recommended && <span className="badge badge-recommended">Recommended</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="hint">Total available in unexpired batches: {batchInfo.totalAvailable} units.</p>
        </>
      )}
    </div>
  );
}
