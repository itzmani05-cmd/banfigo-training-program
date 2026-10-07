import { useEffect, useState } from 'react';
import Dashboard from './components/Dashboard';
import CustomerSection from './components/CustomerSection';
import AccountSection from './components/AccountSection';
import BeneficiarySection from './components/BeneficiarySection';
import TransactionSection from './components/TransactionSection';
import TransferSection from './components/TransferSection';
import ConsentSection from './components/ConsentSection';
import Icon from './components/icons';
import { getUser, logout } from './auth';

const TABS = [
  { key: 'dashboard', label: 'Dashboard', icon: 'dashboard', component: Dashboard },
  { key: 'customers', label: 'Customers', icon: 'customers', component: CustomerSection },
  { key: 'accounts', label: 'Accounts', icon: 'accounts', component: AccountSection },
  { key: 'transactions', label: 'Transactions', icon: 'transactions', component: TransactionSection },
  { key: 'transfers', label: 'Transfers', icon: 'transfers', component: TransferSection },
  { key: 'beneficiaries', label: 'Beneficiaries', icon: 'beneficiaries', component: BeneficiarySection },
  { key: 'consents', label: 'Consents', icon: 'consents', component: ConsentSection },
];

// Only the app's own roles, not Keycloak defaults like offline_access
const APP_ROLES = ['ADMIN', 'MAKER', 'CHECKER'];

// Logo sits on a white tile so its dark-blue parts stay visible on the black background
function Brand({ large = false }) {
  return (
    <div className="flex items-center gap-3">
      <span className={`grid shrink-0 place-items-center rounded-xl bg-paper p-1.5 ${large ? 'size-12' : 'size-10'}`}>
        <img src="/logo.png" alt="Banfigo logo" className="size-full object-contain" />
      </span>
      <div className="leading-tight">
        <div className={`font-bold text-paper ${large ? 'text-title' : ''}`}>Banfigo</div>
        <div className="text-caption text-paper/60">Banking Console</div>
      </div>
    </div>
  );
}

function NavItems({ active, onSelect }) {
  return (
    <ul className="space-y-1">
      {TABS.map((tab) => {
        const selected = active === tab.key;
        return (
          <li key={tab.key}>
            <button
              onClick={() => onSelect(tab.key)}
              aria-current={selected ? 'page' : undefined}
              className={
                'flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-left font-bold transition-colors ' +
                (selected ? 'bg-brand text-paper shadow-sm' : 'text-paper/70 hover:bg-paper/10 hover:text-paper')
              }
            >
              <Icon name={tab.icon} />
              {tab.label}
            </button>
          </li>
        );
      })}
    </ul>
  );
}

function UserCard({ username, roles }) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-paper/5 p-3">
      <span className="grid size-9 shrink-0 place-items-center rounded-full bg-paper text-body font-bold text-ink uppercase">
        {username?.[0] || '?'}
      </span>
      <div className="min-w-0 flex-1">
        <div className="truncate font-bold text-paper">{username}</div>
        <div className="truncate text-caption text-paper/60">{roles.length ? roles.join(' · ') : 'View only'}</div>
      </div>
      <button
        onClick={logout}
        title="Log out"
        aria-label="Log out"
        className="grid size-9 shrink-0 cursor-pointer place-items-center rounded-lg text-paper/70 transition-colors hover:bg-paper/10 hover:text-paper"
      >
        <Icon name="logout" className="size-4" />
      </button>
    </div>
  );
}

function SidebarContent({ active, onSelect, user, roles }) {
  return (
    <div className="flex h-full flex-col gap-6 p-4">
      <div className="px-2 pt-2">
        <Brand large />
      </div>
      <nav className="flex-1 overflow-y-auto" aria-label="Main">
        <NavItems active={active} onSelect={onSelect} />
      </nav>
      <UserCard username={user.username} roles={roles} />
    </div>
  );
}

function App() {
  const [activeTab, setActiveTab] = useState(TABS[0].key);
  const [menuOpen, setMenuOpen] = useState(false);
  const current = TABS.find((t) => t.key === activeTab);
  const ActiveComponent = current.component;
  const user = getUser();
  const roles = user.roles.filter((r) => APP_ROLES.includes(r));

  const select = (key) => {
    setActiveTab(key);
    setMenuOpen(false);
    window.scrollTo({ top: 0 });
  };

  // Close the mobile menu with Escape
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKey = (e) => e.key === 'Escape' && setMenuOpen(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  return (
    <div className="min-h-screen lg:pl-64">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 bg-ink lg:block">
        <SidebarContent active={activeTab} onSelect={select} user={user} roles={roles} />
      </aside>

      {/* Mobile / tablet top bar */}
      <header className="sticky top-0 z-20 flex items-center justify-between gap-3 bg-ink px-4 py-3 lg:hidden">
        <Brand />
        <button
          onClick={() => setMenuOpen(true)}
          aria-label="Open menu"
          aria-expanded={menuOpen}
          className="grid size-10 cursor-pointer place-items-center rounded-lg text-paper hover:bg-paper/10"
        >
          <Icon name="menu" />
        </button>
      </header>

      {/* Mobile / tablet slide-out menu */}
      <div className={`fixed inset-0 z-40 lg:hidden ${menuOpen ? '' : 'pointer-events-none'}`} inert={!menuOpen}>
        <div
          onClick={() => setMenuOpen(false)}
          className={`absolute inset-0 bg-ink/50 transition-opacity ${menuOpen ? 'opacity-100' : 'opacity-0'}`}
        />
        <aside
          className={
            'absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-ink shadow-xl transition-transform duration-200 ' +
            (menuOpen ? 'translate-x-0' : '-translate-x-full')
          }
        >
          <button
            onClick={() => setMenuOpen(false)}
            aria-label="Close menu"
            className="absolute top-4 right-3 grid size-9 cursor-pointer place-items-center rounded-lg text-paper/70 hover:bg-paper/10 hover:text-paper"
          >
            <Icon name="close" />
          </button>
          <SidebarContent active={activeTab} onSelect={select} user={user} roles={roles} />
        </aside>
      </div>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 sm:py-8 lg:px-10 lg:py-10">
        <ActiveComponent />
      </main>
    </div>
  );
}

export default App;
