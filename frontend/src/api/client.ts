import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || '/api';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Request interceptor — attach JWT
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('manhaji_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      // Clear auth and redirect to login
      localStorage.removeItem('manhaji_token');
      localStorage.removeItem('manhaji_user');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default apiClient;
