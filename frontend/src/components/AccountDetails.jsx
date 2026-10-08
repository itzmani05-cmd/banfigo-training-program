import { useEffect, useState } from 'react';
import { AccountApi, StatementApi, TransactionApi } from '../api';
import { money } from '../format';
import {
  Alert, Button, Card, DetailList, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, TransactionTypeBadge, inputClass,
} from './ui';

const PAGE_SIZE = 10;
const EMPTY_FILTERS = { from: '', to: '', type: '' };

// yyyy-MM-dd in local time (toISOString would use UTC and can be off by a day)
const isoDate = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

const defaultStatementRange = () => {
  const today = new Date();
  return { from: isoDate(new Date(today.getFullYear(), today.getMonth(), 1)), to: isoDate(today) };
};

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
  const [statementRange, setStatementRange] = useState(defaultStatementRange);
  const [downloading, setDownloading] = useState('');

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

  const handleStatementChange = (field) => (e) => setStatementRange({ ...statementRange, [field]: e.target.value });

  const downloadStatement = async (format) => {
    setError('');
    setDownloading(format);
    try {
      await StatementApi.download(accountId, { ...statementRange, format });
    } catch (err) {
      setError(err.message);
    } finally {
      setDownloading('');
    }
  };

  const clearFilters = () => {
    setDraftFilters(EMPTY_FILTERS);
    setFilters(EMPTY_FILTERS);
    setPage(0);
  };

  const statementDisabled = !statementRange.from || !statementRange.to || downloading !== '';

  return (
    <div>
      <PageHeader title="Account Details">
        <Button icon="back" onClick={onBack}>Back to Accounts</Button>
      </PageHeader>

      <Alert>{error}</Alert>

      {loading ? (
        <Empty>Loading...</Empty>
      ) : account && (
        <>
          <Card>
            <div className="-mx-4 -mt-4 mb-6 flex flex-col gap-4 bg-brand px-4 py-6 text-paper sm:-mx-6 sm:-mt-6 sm:flex-row sm:items-end sm:justify-between sm:px-6">
              <div className="min-w-0">
                <div className="text-label text-paper/75">{account.accountType} account</div>
                <div className="mt-1 text-title font-bold tracking-wider tabular-nums">{account.accountNumber}</div>
              </div>
              <div className="sm:text-right">
                <div className="text-label text-paper/75">Available balance</div>
                <div className="mt-1 text-stat font-bold tabular-nums">{money(account.balance)}</div>
              </div>
            </div>
            <DetailList
              items={[
                ['Account ID', account.id],
                ['Customer', account.customerName ? `${account.customerName} (ID ${account.customerId})` : `ID ${account.customerId}`],
                ['Customer Email', account.customerEmail || '-'],
              ]}
            />
          </Card>

          <Card title="Download Statement" subtitle="Opening and closing balance with every transaction in the period">
            <FormGrid>
              <Field label="From">
                <input className={inputClass} type="date" value={statementRange.from} onChange={handleStatementChange('from')} />
              </Field>
              <Field label="To">
                <input className={inputClass} type="date" value={statementRange.to} onChange={handleStatementChange('to')} />
              </Field>
              <FormActions>
                <Button variant="primary" icon="download" onClick={() => downloadStatement('pdf')} disabled={statementDisabled}>
                  {downloading === 'pdf' ? 'Preparing...' : 'Download PDF'}
                </Button>
                <Button icon="download" onClick={() => downloadStatement('csv')} disabled={statementDisabled}>
                  {downloading === 'csv' ? 'Preparing...' : 'Download CSV'}
                </Button>
              </FormActions>
            </FormGrid>
          </Card>

          <Card title="Transaction History">
            <form onSubmit={applyFilters} className="mb-5 grid gap-4 sm:grid-cols-3 lg:grid-cols-[1fr_1fr_1fr_auto] sm:items-end">
              <Field label="From">
                <input className={inputClass} type="date" value={draftFilters.from} onChange={handleFilterChange('from')} />
              </Field>
              <Field label="To">
                <input className={inputClass} type="date" value={draftFilters.to} onChange={handleFilterChange('to')} />
              </Field>
              <Field label="Type">
                <select className={inputClass} value={draftFilters.type} onChange={handleFilterChange('type')}>
                  <option value="">All</option>
                  <option value="DEPOSIT">DEPOSIT</option>
                  <option value="WITHDRAWAL">WITHDRAWAL</option>
                </select>
              </Field>
              <div className="flex gap-2 sm:col-span-3 lg:col-span-1">
                <Button type="submit" variant="primary">Apply</Button>
                <Button onClick={clearFilters}>Clear</Button>
              </div>
            </form>

            {txLoading ? (
              <Empty>Loading transactions...</Empty>
            ) : transactions.length === 0 ? (
              <Empty>No transactions found.</Empty>
            ) : (
              <>
                <Table columns={['ID', 'Type', 'Description', 'Date', 'By', '>Amount']}>
                  {transactions.map((t) => (
                    <tr key={t.id} className="hover:bg-wash/60">
                      <Td className="text-muted">{t.id}</Td>
                      <Td><TransactionTypeBadge type={t.transactionType} /></Td>
                      <Td>{t.description || '-'}</Td>
                      <Td className="whitespace-nowrap text-muted">{t.transactionDate.replace('T', ' ').slice(0, 16)}</Td>
                      <Td className="text-muted">{t.createdBy || '-'}</Td>
                      <Td className="text-right font-bold tabular-nums">{money(t.amount)}</Td>
                    </tr>
                  ))}
                </Table>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                  <span className="text-label text-muted">
                    Page {pageInfo.page + 1} of {pageInfo.totalPages} · {pageInfo.totalElements} transactions
                  </span>
                  <div className="flex gap-2">
                    <Button size="sm" onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</Button>
                    <Button size="sm" onClick={() => setPage(page + 1)} disabled={page + 1 >= pageInfo.totalPages}>Next</Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
