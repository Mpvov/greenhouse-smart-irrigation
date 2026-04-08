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
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
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
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        console.error('[apiClient] Request error:', error);
        return Promise.reject(error);
    }
);

// Greenhouse CRUD methods
export const greenhouseApi = {
    getAll: () => apiClient.get('/v1/greenhouses').then(res => res.data.data),
    getById: (id) => apiClient.get(`/v1/greenhouses/${id}`).then(res => res.data.data),
    create: (data) => apiClient.post('/v1/greenhouses', data).then(res => res.data.data),
    update: (id, data) => apiClient.put(`/v1/greenhouses/${id}`, data).then(res => res.data.data),
    delete: (id) => apiClient.delete(`/v1/greenhouses/${id}`).then(res => res.data.data),
};

export const zoneApi = {
    getByGreenhouse: (ghId) => apiClient.get(`/v1/zones/greenhouse/${ghId}`).then(res => res.data.data),
    create: (data) => apiClient.post('/v1/zones', data).then(res => res.data.data),
    update: (id, data) => apiClient.put(`/v1/zones/${id}`, data).then(res => res.data.data),
    delete: (id) => apiClient.delete(`/v1/zones/${id}`).then(res => res.data.data),
};

export const rowApi = {
    getByZone: (zoneId) => apiClient.get(`/v1/rows/zone/${zoneId}`).then(res => res.data.data),
    create: (data) => apiClient.post('/v1/rows', data).then(res => res.data.data),
    update: (id, data) => apiClient.put(`/v1/rows/${id}`, data).then(res => res.data.data),
    updateMode: (id, currentMode) => apiClient.put(`/v1/devices/irrigate/${id}/mode`, { currentMode }).then(res => res.data),
    updateThreshold: (id, thresholdMin, thresholdMax) => apiClient.put(`/v1/devices/irrigate/${id}/threshold`, { thresholdMin, thresholdMax }).then(res => res.data),
    controlPump: (id, action = 'TOGGLE') => apiClient.post(`/v1/devices/irrigate/${id}/control`, { action }).then(res => res.data),
    delete: (id) => apiClient.delete(`/v1/rows/${id}`).then(res => res.data.data),
};

export const deviceApi = {
    getByGreenhouse: (ghId) => apiClient.get(`/v1/devices/greenhouse/${ghId}`).then(res => res.data.data),
    create: (data) => apiClient.post('/v1/devices', data).then(res => res.data.data),
    delete: (id) => apiClient.delete(`/v1/devices/${id}`).then(res => res.data.data),
};

export const scheduleApi = {
    getByRow: (rowId) => apiClient.get(`/v1/schedules/row/${rowId}`).then(res => res.data.data),
    create: (data) => apiClient.post('/v1/schedules', data).then(res => res.data.data),
    delete: (id) => apiClient.delete(`/v1/schedules/${id}`).then(res => res.data.data),
};

export const alertApi = {
    getAll: () => apiClient.get('/v1/alerts').then(res => res.data.data),
    dismiss: (id) => apiClient.delete(`/v1/alerts/${id}`).then(res => res.data.data),
};

// =============================================================================
// RESPONSE INTERCEPTOR
// Intercepts every incoming response.
// =============================================================================
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        const status = error.response?.status;
        const message = error.response?.data?.message || error.message;

        if (status === 401) {
            console.warn('[apiClient] 401 Unauthorized — clearing token...');
            localStorage.removeItem('token');
            window.location.href = '/login';
        } else if (status === 500) {
            console.error('[apiClient] 500 Internal Server Error:', message);
        } else {
            console.warn(`[apiClient] HTTP ${status}:`, message);
        }

        return Promise.reject(error);
    }
);

export default apiClient;
