import { useEffect, useState, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, Scatter, ComposedChart,
} from 'recharts';
import client from '../api/client.js';
import usePharmacyEvents from '../hooks/usePharmacyEvents.js';

// The alert button sends real emails through Brevo. Turn it on only after you have
// set BREVO_API_KEY, EMAIL_FROM and DEMO_OWNER_EMAIL and confirmed an email arrives.
const SHOW_ALERT_BUTTON = false;

const levelColor = { LOW: '#5f7d62', MEDIUM: '#d6a24a', HIGH: '#bd6b3e' };
const CHART_COLORS = ['#bd6b3e', '#5f7d62', '#d6a24a', '#7a8fae', '#a66b8e'];

export default function Dashboard({ pharmacyId }) {
  const [risks, setRisks] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [alertStatus, setAlertStatus] = useState(null);
  const [checkingAlerts, setCheckingAlerts] = useState(false);
  const [liveFlash, setLiveFlash] = useState(false);

  const loadAll = useCallback(async () => {
    try {
      const [riskRes, transferRes, txRes] = await Promise.all([
        client.get('/api/dashboard/expiry-risk'),
        client.get('/api/transfers/suggestions'),
        client.get('/api/dashboard/transactions'),
      ]);
      setRisks(riskRes.data);
      setTransfers(transferRes.data);
      setTransactions(txRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadAll(); }, [loadAll]);

  // Live updates: when the backend signals a dispense or transfer change
  // for this pharmacy, silently re-fetch and show a brief "Live" flash.
  const handleEvent = useCallback(() => {
    loadAll();
    setLiveFlash(true);
    setTimeout(() => setLiveFlash(false), 1800);
  }, [loadAll]);

  usePharmacyEvents(pharmacyId, handleEvent);

  async function acceptTransfer(id) {
    await client.post(`/api/transfers/${id}/accept`);
    loadAll();
  }

  async function approveTransfer(id) {
    await client.post(`/api/transfers/${id}/approve`);
    loadAll();
  }

  async function runAlertCheck() {
    setCheckingAlerts(true);
    setAlertStatus(null);
    try {
      const res = await client.post('/api/alerts/check-now');
      const { riskAlertsSent, transferAlertsSent } = res.data;
      const total = riskAlertsSent + transferAlertsSent;
      setAlertStatus(
          total === 0
              ? 'Checked — nothing new to alert on right now.'
              : `Checked — sent ${riskAlertsSent} risk alert(s) and ${transferAlertsSent} transfer alert(s).`
      );
    } catch (err) {
      setAlertStatus('Could not run the alert check. Is the backend configured with a Brevo API key?');
    } finally {
      setCheckingAlerts(false);
    }
  }

  // Aggregate raw transactions into per-day, per-medicine totals for the chart.
  const chartData = useMemo(() => {
    const byDate = {};
    const medicines = new Set();

    transactions.forEach((tx) => {
      const day = tx.timestamp.slice(0, 10); // YYYY-MM-DD
      medicines.add(tx.medicineName);
      byDate[day] = byDate[day] || { date: day };
      byDate[day][tx.medicineName] = (byDate[day][tx.medicineName] || 0) + tx.quantity;
    });

    return {
      points: Object.values(byDate).sort((a, b) => a.date.localeCompare(b.date)),
      medicines: Array.from(medicines),
    };
  }, [transactions]);

  const anomalyPoints = useMemo(() =>
          transactions
              .filter((tx) => tx.anomalyFlagged)
              .map((tx) => ({ date: tx.timestamp.slice(0, 10), value: tx.quantity })),
      [transactions]
  );

  if (loading) return <p>Loading dashboard...</p>;

  return (
      <div>
        <div className="card">
          <div className="card-header-row">
            <h2>Expiry Risk</h2>
            <AnimatePresence>
              {liveFlash && (
                  <motion.span
                      className="live-badge"
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0 }}
                  >
                    ● Live update
                  </motion.span>
              )}
            </AnimatePresence>
          </div>
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
          <h2>Dispensing Trend</h2>
          <p className="subtitle">Daily quantity dispensed per medicine. Red dots mark transactions flagged as anomalous.</p>
          {chartData.points.length === 0 ? (
              <p className="hint">No dispensing history yet — use the Dispense page to see trends here.</p>
          ) : (
              <ResponsiveContainer width="100%" height={260}>
                <ComposedChart data={chartData.points}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5ddcf" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Legend />
                  {chartData.medicines.map((med, i) => (
                      <Line
                          key={med}
                          type="monotone"
                          dataKey={med}
                          stroke={CHART_COLORS[i % CHART_COLORS.length]}
                          strokeWidth={2}
                          dot={{ r: 3 }}
                      />
                  ))}
                  <Scatter data={anomalyPoints} dataKey="value" fill="#b7462c" shape="cross" />
                </ComposedChart>
              </ResponsiveContainer>
          )}
        </div>

        <div className="card">
          <div className="card-header-row">
            <h2>Transfer Suggestions</h2>
            {SHOW_ALERT_BUTTON && (
                <motion.button
                    className="alert-check-btn"
                    onClick={runAlertCheck}
                    disabled={checkingAlerts}
                    whileHover={{ scale: checkingAlerts ? 1 : 1.03 }}
                    whileTap={{ scale: checkingAlerts ? 1 : 0.97 }}
                >
                  {checkingAlerts ? 'Checking...' : '📧 Check for Alerts Now'}
                </motion.button>
            )}
          </div>
          {alertStatus && <p className="hint">{alertStatus}</p>}

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