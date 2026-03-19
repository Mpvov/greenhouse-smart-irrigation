import { Routes, Route, NavLink, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import History from './pages/History.jsx';
import Settings from './pages/Settings.jsx';
import Login from './pages/Login.jsx';
import { useAuth } from './store/AuthContext.jsx';
import './App.css';

// ─── Stub: Sidebar ──────────────────────────────────────────────────────────
// Navigation Rail linking the 3 main sections of the application.
// Will be extracted to components/Sidebar.jsx in a later iteration.
function Sidebar() {
  const navLinkClass = ({ isActive }) =>
    `sidebar__link ${isActive ? 'sidebar__link--active' : ''}`;

  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__logo">🌿</span>
        <span className="sidebar__title">IrriSmart</span>
      </div>
      <nav className="sidebar__nav">
        <NavLink to="/" end className={navLinkClass}>
          📊 Dashboard
        </NavLink>
        <NavLink to="/history" className={navLinkClass}>
          📈 History
        </NavLink>
        <NavLink to="/settings" className={navLinkClass}>
          ⚙️ Settings
        </NavLink>
      </nav>
    </aside>
  );
}

// ─── Stub: Header ───────────────────────────────────────────────────────────
// Top bar with app title and future auth controls (avatar, notifications).
// Will be extracted to components/Header.jsx in a later iteration.
function Header({ logout }) {
  return (
    <header className="header">
      <h1 className="header__title">Smart Irrigation System</h1>
      <div className="header__actions">
        {/* TODO: Add user avatar, notification bell */}
        <button onClick={logout} style={{background:'none', border:'none', color:'#ccc', cursor:'pointer'}}>Logout</button>
      </div>
    </header>
  );
}

// ─── Root App Layout ─────────────────────────────────────────────────────────
function App() {
  const { token, logout } = useAuth();
  
  if (!token) {
    return <Login />;
  }

  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-layout__main">
        <Header logout={logout} />
        <main className="app-layout__content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/history" element={<History />} />
            <Route path="/settings" element={<Settings />} />
            {/* Catch-all: redirect unknown routes to Dashboard */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
