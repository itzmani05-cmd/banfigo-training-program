import { useEffect, useState } from 'react';
import { DashboardApi } from '../api';
import MonthlyFlowChart from './MonthlyFlowChart';
import { money } from '../format';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      setData(await DashboardApi.get());
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div>
      <div className="section-title">
        <h2>Dashboard</h2>
        <button onClick={load}>Refresh</button>
      </div>

      {error && <div className="message">{error}</div>}

      {!data ? (
        !error && <p className="empty">Loading...</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-tile">
              <div className="stat-label">Total balance</div>
              <div className="stat-value">{money(data.totalBalance)}</div>
            </div>
            <div className="stat-tile">
              <div className="stat-label">Accounts</div>
              <div className="stat-value">{data.accountCount}</div>
            </div>
            <div className="stat-tile">
              <div className="stat-label">Customers</div>
              <div className="stat-value">{data.customerCount}</div>
            </div>
          </div>

          <h3>Money in vs money out (last {data.monthlyFlows.length} months)</h3>
          <MonthlyFlowChart flows={data.monthlyFlows} />

          <h3>Recent transactions</h3>
          {data.recentTransactions.length === 0 ? (
            <p className="empty">No transactions yet.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Account</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {data.recentTransactions.map((t) => (
                  <tr key={t.id}>
                    <td>{t.transactionDate.replace('T', ' ').slice(0, 16)}</td>
                    <td>{t.accountNumber}</td>
                    <td>{t.transactionType}</td>
                    <td>{money(t.amount)}</td>
                    <td>{t.description || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}
