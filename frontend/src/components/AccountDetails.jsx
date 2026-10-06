import { useEffect, useState } from 'react';
import { AccountApi, TransactionApi } from '../api';

const PAGE_SIZE = 10;
const EMPTY_FILTERS = { from: '', to: '', type: '' };

export default function AccountDetails({ accountId, onBack }) {
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [draftFilters, setDraftFilters] = useState(EMPTY_FILTERS);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [txLoading, setTxLoading] = useState(false);

  useEffect(() => {
    const loadAccount = async () => {
      setLoading(true);
      try {
        setAccount(await AccountApi.get(accountId));
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    loadAccount();
  }, [accountId]);

  useEffect(() => {
    const loadTransactions = async () => {
      setError('');
      setTxLoading(true);
      try {
        const data = await TransactionApi.listByAccount(accountId, {
          ...filters,
          page,
          size: PAGE_SIZE,
        });
        setTransactions(data.content);
        setPageInfo({ page: data.page, totalPages: data.totalPages, totalElements: data.totalElements });
      } catch (err) {
        setError(err.message);
      } finally {
        setTxLoading(false);
      }
    };
    loadTransactions();
  }, [accountId, filters, page]);

  const handleFilterChange = (field) => (e) => setDraftFilters({ ...draftFilters, [field]: e.target.value });

  const applyFilters = (e) => {
    e.preventDefault();
    setFilters(draftFilters);
    setPage(0);
  };

  const clearFilters = () => {
    setDraftFilters(EMPTY_FILTERS);
    setFilters(EMPTY_FILTERS);
    setPage(0);
  };

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
                  {account.customerName
                    ? `${account.customerName} (ID ${account.customerId})`
                    : `ID ${account.customerId}`}
                </td>
              </tr>
              <tr>
                <th>Customer Email</th>
                <td>{account.customerEmail || '-'}</td>
              </tr>
            </tbody>
          </table>

          <hr />

          <h3>Transaction History</h3>

          <form className="form-grid" onSubmit={applyFilters}>
            <div className="field">
              <label>From</label>
              <input type="date" value={draftFilters.from} onChange={handleFilterChange('from')} />
            </div>
            <div className="field">
              <label>To</label>
              <input type="date" value={draftFilters.to} onChange={handleFilterChange('to')} />
            </div>
            <div className="field">
              <label>Type</label>
              <select value={draftFilters.type} onChange={handleFilterChange('type')}>
                <option value="">All</option>
                <option value="DEPOSIT">DEPOSIT</option>
                <option value="WITHDRAWAL">WITHDRAWAL</option>
              </select>
            </div>
            <div className="row-actions">
              <button type="submit" className="primary">Apply</button>
              <button type="button" onClick={clearFilters}>Clear</button>
            </div>
          </form>

          {txLoading ? (
            <p className="empty">Loading transactions...</p>
          ) : transactions.length === 0 ? (
            <p className="empty">No transactions found.</p>
          ) : (
            <>
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

              <div className="row-actions" style={{ marginTop: 12, alignItems: 'center' }}>
                <button onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</button>
                <span>
                  Page {pageInfo.page + 1} of {pageInfo.totalPages} ({pageInfo.totalElements} total)
                </span>
                <button onClick={() => setPage(page + 1)} disabled={page + 1 >= pageInfo.totalPages}>Next</button>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
