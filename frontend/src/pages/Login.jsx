import { useState } from 'react';
import { useAuth } from '../store/AuthContext';
import { useNavigate } from 'react-router-dom';

function Login({ initialMode = 'login' }) {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isRegister, setIsRegister] = useState(initialMode === 'register');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isRegister) {
        await register(email, password);
      } else {
        await login(email, password);
      }
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const containerStyle = {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #060d1a 0%, #0f172a 100%)',
    fontFamily: 'Inter, sans-serif',
  };

  const cardStyle = {
    padding: '2.5rem',
    background: 'rgba(255,255,255,0.04)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: '20px',
    color: '#f8fafc',
    width: '100%',
    maxWidth: '400px',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    backdropFilter: 'blur(20px)',
    boxShadow: '0 25px 50px rgba(0,0,0,0.5)',
  };

  const inputStyle = {
    padding: '0.8rem 1rem',
    borderRadius: '10px',
    border: '1px solid rgba(255,255,255,0.1)',
    background: 'rgba(255,255,255,0.05)',
    color: '#f8fafc',
    fontSize: '0.95rem',
    fontFamily: 'Inter, sans-serif',
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
  };

  const btnPrimary = {
    padding: '0.85rem',
    background: 'linear-gradient(135deg, #3b82f6, #6366f1)',
    color: '#fff',
    border: 'none',
    borderRadius: '10px',
    fontSize: '1rem',
    fontWeight: '700',
    cursor: loading ? 'not-allowed' : 'pointer',
    opacity: loading ? 0.7 : 1,
    fontFamily: 'Inter, sans-serif',
    boxShadow: '0 4px 15px rgba(59,130,246,0.35)',
  };

  const btnLink = {
    background: 'none',
    border: 'none',
    color: '#93c5fd',
    cursor: 'pointer',
    fontSize: '0.9rem',
    fontFamily: 'Inter, sans-serif',
    textDecoration: 'underline',
  };

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: '1.5rem', alignSelf: 'flex-start', marginLeft: 'auto', marginRight: 'auto', maxWidth: '400px', width: '100%' }}>
        <button style={{ ...btnLink, textDecoration: 'none', color: '#64748b', fontSize: '0.85rem' }} onClick={() => navigate('/')}>
          ← Back to home
        </button>
      </div>
      <form onSubmit={handleSubmit} style={cardStyle}>
        <div style={{ textAlign: 'center', marginBottom: '0.5rem' }}>
          <span style={{ fontSize: '2rem' }}>🌿</span>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, margin: '0.5rem 0 0', letterSpacing: '-0.02em' }}>
            {isRegister ? 'Create Account' : 'Welcome back'}
          </h2>
          <p style={{ color: '#64748b', fontSize: '0.875rem', margin: '0.25rem 0 0' }}>
            {isRegister ? 'Start managing your smart greenhouse' : 'Sign in to your dashboard'}
          </p>
        </div>

        {error && (
          <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5', padding: '0.75rem 1rem', borderRadius: '8px', fontSize: '0.875rem' }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
          <label style={{ fontSize: '0.78rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Email</label>
          <input
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={inputStyle}
            required
          />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
          <label style={{ fontSize: '0.78rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Password</label>
          <input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={inputStyle}
            required
          />
        </div>

        <button type="submit" style={btnPrimary} disabled={loading}>
          {loading ? 'Please wait...' : (isRegister ? 'Create Account' : 'Sign In')}
        </button>

        <div style={{ textAlign: 'center' }}>
          <button type="button" onClick={() => setIsRegister(!isRegister)} style={btnLink}>
            {isRegister ? 'Already have an account? Sign In' : "Don't have an account? Register"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default Login;
