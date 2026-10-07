import { useEffect, useState } from 'react';
import client from '../api/client.js';

const levelColor = { LOW: '#2ecc71', MEDIUM: '#f1c40f', HIGH: '#e74c3c' };

export default function Dashboard() {
  const [risks, setRisks] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [riskRes, transferRes] = await Promise.all([
          client.get('/api/dashboard/expiry-risk'),
          client.get('/api/transfers/suggestions'),
        ]);
        setRisks(riskRes.data);
        setTransfers(transferRes.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function acceptTransfer(id) {
    await client.post(`/api/transfers/${id}/accept`);
    refreshTransfers();
  }

  async function approveTransfer(id) {
    await client.post(`/api/transfers/${id}/approve`);
    refreshTransfers();
  }

  async function refreshTransfers() {
    const res = await client.get('/api/transfers/suggestions');
    setTransfers(res.data);
  }

  if (loading) return <p>Loading dashboard...</p>;

  return (
    <div>
      <div className="card">
        <h2>Expiry Risk</h2>
        <table>
          <thead>
            <tr><th>Medicine</th><th>Batch</th><th>Qty</th><th>Days to Expiry</th><th>Est. Surplus</th><th>Risk</th></tr>
          </thead>
          <tbody>
            {risks.map((r) => (
              <tr key={r.batch.id}>
                <td>{r.batch.medicine.name}</td>
                <td>{r.batch.batchNumber}</td>
                <td>{r.batch.quantity}</td>
                <td>{r.batch.daysToExpiry}</td>
                <td>{r.estimatedSurplus.toFixed(0)}</td>
                <td><span className="badge" style={{ background: levelColor[r.level] }}>{r.level}</span></td>
              </tr>
            ))}
            {risks.length === 0 && <tr><td colSpan="6">No batches yet — add stock to see risk data.</td></tr>}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Transfer Suggestions</h2>
        {transfers.length === 0 && <p className="hint">No high-risk surplus batches with a matching nearby pharmacy right now.</p>}
        {transfers.map((t) => (
          <div key={t.id} className="transfer-row">
            <span>{t.medicine.name}: {t.suggestedQuantity} units → {t.toPharmacy.name}</span>
            <span className="badge">{t.status}</span>
            {t.status === 'SUGGESTED' && <button onClick={() => acceptTransfer(t.id)}>Accept (receiving pharmacy)</button>}
            {t.status === 'ACCEPTED' && <button onClick={() => approveTransfer(t.id)}>Approve (owner)</button>}
          </div>
        ))}
      </div>
    </div>
  );
}
