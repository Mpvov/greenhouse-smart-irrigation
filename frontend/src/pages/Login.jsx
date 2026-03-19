import { useState } from 'react';
import { useAuth } from '../store/AuthContext';

function Login() {
  const { login, register } = useAuth();
  const [email, setEmail] = useState('demo@example.com');
  const [password, setPassword] = useState('password');
  const [isRegister, setIsRegister] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isRegister) {
        await register(email, password);
      } else {
        await login(email, password);
      }
    } catch (err) {
      setError(err.message || 'Authentication failed');
    }
  };

  return (
    <div className="login-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#1e1e1e' }}>
      <form onSubmit={handleSubmit} style={{ padding: '2rem', background: '#2c2c2c', borderRadius: '8px', color: '#fff', width: '300px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <h2>{isRegister ? 'Register' : 'Login'}</h2>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        <input 
          type="email" 
          placeholder="Email" 
          value={email} 
          onChange={(e) => setEmail(e.target.value)} 
          style={{ padding: '0.5rem', borderRadius: '4px', border: 'none' }}
        />
        <input 
          type="password" 
          placeholder="Password" 
          value={password} 
          onChange={(e) => setPassword(e.target.value)} 
          style={{ padding: '0.5rem', borderRadius: '4px', border: 'none' }}
        />
        <button type="submit" style={{ padding: '0.5rem', background: '#4CAF50', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          {isRegister ? 'Sign Up' : 'Sign In'}
        </button>
        <button type="button" onClick={() => setIsRegister(!isRegister)} style={{ background: 'none', border: 'none', color: '#4CAF50', cursor: 'pointer' }}>
          {isRegister ? 'Already have an account? Login' : 'Need an account? Register'}
        </button>
      </form>
    </div>
  );
}

export default Login;
