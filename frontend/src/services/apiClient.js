import axios from 'axios';

/**
 * apiClient — Configured Axios instance for all REST API calls.
 *
 * Base URL is driven by the Vite environment variable VITE_API_BASE_URL.
 * Falls back to the Nginx reverse proxy path '/api' in production,
 * or direct backend URL during local development.
 *
 * Usage:
 *   import apiClient from '@/services/apiClient';
 *   const data = await apiClient.get('/greenhouses');
 */
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
    timeout: 10_000,
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    },
});

// =============================================================================
// REQUEST INTERCEPTOR
// Intercepts every outgoing request before it is sent.
// TODO (Auth): Attach the Bearer token from localStorage/auth store here.
// =============================================================================
apiClient.interceptors.request.use(
    (config) => {
        // TODO: const token = authStore.getState().token;
        // TODO: if (token) { config.headers.Authorization = `Bearer ${token}`; }
        return config;
    },
    (error) => {
        console.error('[apiClient] Request error:', error);
        return Promise.reject(error);
    }
);

// =============================================================================
// RESPONSE INTERCEPTOR
// Intercepts every incoming response.
// =============================================================================
apiClient.interceptors.response.use(
    (response) => {
        // Unwrap the BaseResponse envelope and return only the `data` payload
        return response;
    },
    (error) => {
        const status = error.response?.status;
        const message = error.response?.data?.message || error.message;

        if (status === 401) {
            // TODO (Auth): Redirect to login page or trigger token refresh
            console.warn('[apiClient] 401 Unauthorized — redirecting to login...');
        } else if (status === 500) {
            console.error('[apiClient] 500 Internal Server Error:', message);
        } else {
            console.warn(`[apiClient] HTTP ${status}:`, message);
        }

        return Promise.reject(error);
    }
);

export default apiClient;
