import { useState } from 'react';
import client from '../api/client.js';

const EMPTY_FORM = {
  medicineName: '', manufacturer: '', batchNumber: '',
  quantity: '', manufactureDate: '', expiryDate: '', supplier: '',
};

export default function StockIntake() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [status, setStatus] = useState(null); // { type: 'success' | 'error', text }
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return; // ignore a second click while the first request is running
    setSubmitting(true);
    setStatus(null);
    try {
      const res = await client.post('/api/batches', { ...form, quantity: Number(form.quantity) });
      setStatus({
        type: 'success',
        text: `Batch ${res.data.batchNumber} of ${form.medicineName.trim()} was added successfully (${res.data.quantity} units).`,
      });
      setForm(EMPTY_FORM);
    } catch (err) {
      setStatus({
        type: 'error',
        text: err.response?.data?.message || 'Failed to add batch. Check the form and try again.',
      });
    } finally {
      setSubmitting(false);
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
        <input type="number" min="1" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} required />

        <label>Manufacture Date</label>
        <input type="date" value={form.manufactureDate} onChange={(e) => update('manufactureDate', e.target.value)} required />

        <label>Expiry Date</label>
        <input type="date" value={form.expiryDate} onChange={(e) => update('expiryDate', e.target.value)} required />

        <label>Supplier</label>
        <input value={form.supplier} onChange={(e) => update('supplier', e.target.value)} />

        <button type="submit" disabled={submitting}>{submitting ? 'Adding...' : 'Add Batch'}</button>
      </form>

      {status && (
        <p className={status.type === 'success' ? 'success-msg' : 'error'}>{status.text}</p>
      )}
    </div>
  );
}
