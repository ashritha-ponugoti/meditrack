import { useState } from 'react';
import client from '../api/client.js';

export default function StockIntake() {
  const [form, setForm] = useState({
    medicineName: '', manufacturer: '', batchNumber: '',
    quantity: '', manufactureDate: '', expiryDate: '', supplier: ''
  });
  const [message, setMessage] = useState('');

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage('');
    try {
      await client.post('/api/batches', { ...form, quantity: Number(form.quantity) });
      setMessage('Batch added successfully.');
      setForm({ medicineName: '', manufacturer: '', batchNumber: '', quantity: '', manufactureDate: '', expiryDate: '', supplier: '' });
    } catch (err) {
      setMessage('Failed to add batch. Check the form and try again.');
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

        <button type="submit">Add Batch</button>
      </form>
      {message && <p className="hint">{message}</p>}
    </div>
  );
}
