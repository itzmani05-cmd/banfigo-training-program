import { useState } from 'react';
import './App.css';
import CustomerSection from './components/CustomerSection';
import AccountSection from './components/AccountSection';
import BeneficiarySection from './components/BeneficiarySection';
import TransactionSection from './components/TransactionSection';

const TABS = [
  { key: 'customers', label: 'Customers', component: CustomerSection },
  { key: 'accounts', label: 'Accounts', component: AccountSection },
  { key: 'transactions', label: 'Transactions', component: TransactionSection },
  { key: 'beneficiaries', label: 'Beneficiaries', component: BeneficiarySection },
];

function App() {
  const [activeTab, setActiveTab] = useState(TABS[0].key);
  const ActiveComponent = TABS.find((t) => t.key === activeTab).component;

  return (
    <>
      <header className="app-header">
        <h1>Banking API Console</h1>
        <div className="tabs">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              className={activeTab === tab.key ? 'active' : ''}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </header>

      <ActiveComponent />
    </>
  );
}

export default App;
