import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import './index.css';

import { AuthProvider } from './store/AuthContext';
import { NotificationProvider } from './components/Notification';

/**
 * Application entry point.
 * BrowserRouter is mounted here (root level) so all child components
 * have access to routing context via useNavigate, useLocation, etc.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <NotificationProvider>
          <App />
        </NotificationProvider>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>
);
