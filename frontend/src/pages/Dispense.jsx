import { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import client from '../api/client.js';
import QrScanner from '../components/QrScanner.jsx';

const BATCH_QR_PREFIX = 'MEDITRACK-BATCH:';

// Camera scanning needs a working webcam and good lighting. It is switched off
// so the demo only shows features that are reliable. Set to true to re-enable.
const ENABLE_QR_SCAN = false;

export default function Dispense() {
  const [medicineName, setMedicineName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [scanning, setScanning] = useState(false);
  const [scanInfo, setScanInfo] = useState('');

  const handleScan = useCallback(async (decodedText) => {
    setScanning(false);
    setScanInfo('');
    setError('');

    if (!decodedText.startsWith(BATCH_QR_PREFIX)) {
      setError('That QR code is not a MediTrack batch code.');
      return;
    }

    const batchId = decodedText.substring(BATCH_QR_PREFIX.length);
    try {
      const res = await client.get(`/api/batches/${batchId}`);
      setMedicineName(res.data.medicine.name);
      setScanInfo(`Scanned batch ${res.data.batchNumber} — ${res.data.medicine.name}`);
    } catch (err) {
      setError('Could not find that batch. It may belong to a different pharmacy.');
    }
  }, []);

  const handleScanError = useCallback((msg) => {
    setScanning(false);
    setError(msg);
  }, []);

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

        {ENABLE_QR_SCAN && (
            <>
              <div className="scan-row">
                <button type="button" className="scan-btn" onClick={() => { setScanning((s) => !s); setScanInfo(''); }}>
                  {scanning ? 'Cancel Scan' : '📷 Scan Batch QR'}
                </button>
                {scanInfo && <span className="scan-info">{scanInfo}</span>}
              </div>

              <AnimatePresence>
                {scanning && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                    >
                      <QrScanner onScan={handleScan} onError={handleScanError} />
                    </motion.div>
                )}
              </AnimatePresence>
            </>
        )}

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