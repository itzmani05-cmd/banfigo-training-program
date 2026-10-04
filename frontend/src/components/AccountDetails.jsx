import { useEffect, useState } from 'react';
import { AccountApi, TransactionApi } from '../api';

export default function AccountDetails({ accountId, onBack }) {
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setError('');
      setLoading(true);
      try {
        const [accountData, transactionData] = await Promise.all([
          AccountApi.get(accountId),
          TransactionApi.listByAccount(accountId),
        ]);
        setAccount(accountData);
        setTransactions(transactionData);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [accountId]);

  return (
    <div>
      <div className="section-title">
        <h2>Account Details</h2>
        <button onClick={onBack}>Back to Accounts</button>
      </div>

      {error && <div className="message">{error}</div>}

      {loading ? (
        <p className="empty">Loading...</p>
      ) : account && (
        <>
          <table>
            <tbody>
              <tr>
                <th>ID</th>
                <td>{account.id}</td>
              </tr>
              <tr>
                <th>Account Number</th>
                <td>{account.accountNumber}</td>
              </tr>
              <tr>
                <th>Type</th>
                <td>{account.accountType}</td>
              </tr>
              <tr>
                <th>Balance</th>
                <td>{account.balance}</td>
              </tr>
              <tr>
                <th>Customer</th>
                <td>
                  {account.customer
                    ? `${account.customer.name} (ID ${account.customer.id})`
                    : '-'}
                </td>
              </tr>
              <tr>
                <th>Customer Email</th>
                <td>{account.customer?.email || '-'}</td>
              </tr>
            </tbody>
          </table>

          <hr />

          <h3>Transaction History</h3>
          {transactions.length === 0 ? (
            <p className="empty">No transactions for this account.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t) => (
                  <tr key={t.id}>
                    <td>{t.id}</td>
                    <td>{t.transactionType}</td>
                    <td>{t.amount}</td>
                    <td>{t.description}</td>
                    <td>{t.transactionDate}</td>
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
