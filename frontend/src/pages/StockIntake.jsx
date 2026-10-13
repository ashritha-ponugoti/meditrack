import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import client from '../api/client.js';

export default function StockIntake() {
  const [form, setForm] = useState({
    medicineName: '', manufacturer: '', batchNumber: '',
    quantity: '', manufactureDate: '', expiryDate: '', supplier: ''
  });
  const [message, setMessage] = useState('');
  const [qrUrl, setQrUrl] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return; // ignore a second click while the first is running
    setSubmitting(true);
    setMessage('');
    setQrUrl(null);
    try {
      const res = await client.post('/api/batches', { ...form, quantity: Number(form.quantity) });
      setMessage(`Batch ${res.data.batchNumber} added successfully.`);
      await loadQr(res.data.id);
      setForm({ medicineName: '', manufacturer: '', batchNumber: '', quantity: '', manufactureDate: '', expiryDate: '', supplier: '' });
    } catch (err) {
      setMessage(err.response?.data?.message || 'Failed to add batch. Check the form and try again.');
    } finally {
      setSubmitting(false);
    }
  }

  async function loadQr(batchId) {
    try {
      const res = await client.get(`/api/batches/${batchId}/qrcode`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      setQrUrl(url);
    } catch (err) {
      // QR generation failing shouldn't block the batch-add flow itself
      console.error('QR generation failed', err);
    }
  }

  return (
      <div className="card">
        <h2>Stock Intake</h2>
        <form onSubmit={handleSubmit} className="form-grid">
          <label>Medicine Name</label>
          <input value={form.medicineName} onChange={(e) => update('medicineName', e.target.value)} required />

          <label>Manufacturer</label>
          <input value={form.manufacturer} onChange={(e) => update('manufacturer', e.target.value)} />

          <label>Batch Number</label>
          <input value={form.batchNumber} onChange={(e) => update('batchNumber', e.target.value)} required />

          <label>Quantity</label>
          <input type="number" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} required />

          <label>Manufacture Date</label>
          <input type="date" value={form.manufactureDate} onChange={(e) => update('manufactureDate', e.target.value)} required />

          <label>Expiry Date</label>
          <input type="date" value={form.expiryDate} onChange={(e) => update('expiryDate', e.target.value)} required />

          <label>Supplier</label>
          <input value={form.supplier} onChange={(e) => update('supplier', e.target.value)} />

          <button type="submit" disabled={submitting}>{submitting ? 'Adding...' : 'Add Batch'}</button>
        </form>
        {message && <p className="hint">{message}</p>}

        <AnimatePresence>
          {qrUrl && (
              <motion.div
                  className="qr-box"
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
              >
                <p className="hint">Batch QR code - print or download it to label this batch:</p>
                <img src={qrUrl} alt="Batch QR code" className="qr-image" />
                <a href={qrUrl} download="batch-qr.png" className="secondary-link">Download QR</a>
              </motion.div>
          )}
        </AnimatePresence>
      </div>
  );
}