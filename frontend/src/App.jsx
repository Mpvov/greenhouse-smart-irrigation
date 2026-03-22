import React from 'react';
import { Routes, Route, NavLink, Navigate, useNavigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import History from './pages/History.jsx';
import Settings from './pages/Settings.jsx';
import Login from './pages/Login.jsx';
import GreenhouseList from './pages/GreenhouseList.jsx';
import GreenhouseDetail from './pages/GreenhouseDetail.jsx';
import LandingPage from './pages/LandingPage.jsx';
import { useAuth } from './store/AuthContext.jsx';
import './App.css';

// ─── Sidebar ─────────────────────────────────────────────────────────────────
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
        <NavLink to="/" end className={navLinkClass}>📊 Dashboard</NavLink>
        <NavLink to="/history" className={navLinkClass}>📈 History</NavLink>
        <NavLink to="/greenhouses" className={navLinkClass}>🏡 Greenhouses</NavLink>
        <NavLink to="/settings" className={navLinkClass}>⚙️ Settings</NavLink>
      </nav>
    </aside>
  );
}

// ─── Header ───────────────────────────────────────────────────────────────────
function Header({ logout }) {
  return (
    <header className="header">
      <h1 className="header__title">Smart Irrigation System</h1>
      <div className="header__actions">
        <button onClick={logout} style={{background:'none', border:'none', color:'#ccc', cursor:'pointer'}}>Logout</button>
      </div>
    </header>
  );
}

// ─── Login/Register wrapper ────────────────────────────────────────────────────
function AuthPage({ mode }) {
  return <Login initialMode={mode} />;
}

// ─── Root App ─────────────────────────────────────────────────────────────────
function App() {
  const { token, logout } = useAuth();

  // Authenticated: full app layout
  if (token) {
    return (
      <div className="app-layout">
        <Sidebar />
        <div className="app-layout__main">
          <Header logout={logout} />
          <main className="app-layout__content">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/history" element={<History />} />
              <Route path="/greenhouses" element={<GreenhouseList />} />
              <Route path="/greenhouses/:id" element={<GreenhouseDetail />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </div>
    );
  }

  // Unauthenticated: landing + auth pages
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/register" element={<AuthPage mode="register" />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
