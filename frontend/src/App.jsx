import { useState } from 'react';
import './App.css';
import CustomerSection from './components/CustomerSection';
import AccountSection from './components/AccountSection';
import BeneficiarySection from './components/BeneficiarySection';
import TransactionSection from './components/TransactionSection';
import { getUser, logout } from './auth';

const TABS = [
  { key: 'customers', label: 'Customers', component: CustomerSection },
  { key: 'accounts', label: 'Accounts', component: AccountSection },
  { key: 'transactions', label: 'Transactions', component: TransactionSection },
  { key: 'beneficiaries', label: 'Beneficiaries', component: BeneficiarySection },
];

function App() {
  const [activeTab, setActiveTab] = useState(TABS[0].key);
  const ActiveComponent = TABS.find((t) => t.key === activeTab).component;
  const user = getUser();

  return (
    <>
      <header className="app-header">
        <div className="header-top">
          <h1>Banking API Console</h1>
          <div className="user-info">
            <span>{user.username}</span>
            <button onClick={logout}>Log out</button>
          </div>
        </div>
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
