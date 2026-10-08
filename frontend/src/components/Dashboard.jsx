import { useEffect, useState } from 'react';
import { DashboardApi } from '../api';
import MonthlyFlowChart from './MonthlyFlowChart';
import { money } from '../format';
import { isCustomer } from '../permissions';
import { Alert, Button, Card, Empty, PageHeader, StatTile, Table, Td, TransactionTypeBadge } from './ui';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const customer = isCustomer();

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
      <PageHeader
        title="Dashboard"
        description={customer ? 'Your balances and recent activity.' : 'Overview of balances and recent activity across the bank.'}
      >
        <Button icon="refresh" onClick={load}>Refresh</Button>
      </PageHeader>

      <Alert>{error}</Alert>

      {!data ? (
        !error && <Empty>Loading...</Empty>
      ) : (
        <>
          <div className={`mb-6 grid gap-4 sm:grid-cols-2 ${customer ? '' : 'xl:grid-cols-3'}`}>
            <StatTile
              label="Total balance"
              value={money(data.totalBalance)}
              icon="wallet"
              highlight
              className={customer ? '' : 'sm:col-span-2 xl:col-span-1'}
            />
            <StatTile label="Accounts" value={data.accountCount} icon="accounts" />
            {!customer && <StatTile label="Customers" value={data.customerCount} icon="customers" />}
          </div>

          <Card title="Money in vs money out" subtitle={`Last ${data.monthlyFlows.length} months`}>
            <MonthlyFlowChart flows={data.monthlyFlows} />
          </Card>

          <Card title="Recent transactions" subtitle={`The 10 latest across ${customer ? 'your' : 'all'} accounts`}>
            {data.recentTransactions.length === 0 ? (
              <Empty>No transactions yet.</Empty>
            ) : (
              <Table columns={['Date', 'Account', 'Type', 'Description', '>Amount']}>
                {data.recentTransactions.map((t) => (
                  <tr key={t.id} className="hover:bg-wash/60">
                    <Td className="whitespace-nowrap text-muted">{t.transactionDate.replace('T', ' ').slice(0, 16)}</Td>
                    <Td className="tabular-nums">{t.accountNumber}</Td>
                    <Td><TransactionTypeBadge type={t.transactionType} /></Td>
                    <Td>{t.description || '-'}</Td>
                    <Td className="text-right font-bold tabular-nums">{money(t.amount)}</Td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
