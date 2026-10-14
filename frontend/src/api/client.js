import axios from 'axios';

// Set VITE_API_URL in your deployment env (e.g. Vercel) to your Render backend URL.
// Falls back to localhost for local development.
const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const client = axios.create({ baseURL });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('meditrack_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the server rejects a saved token (expired, or the account no longer exists), forget it
// and open the login popup instead of leaving the user on a broken page.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    if ((status === 401 || status === 403) && !url.includes('/api/auth/') && localStorage.getItem('meditrack_token')) {
      localStorage.removeItem('meditrack_token');
      localStorage.removeItem('meditrack_pharmacy_id');
      window.location.assign('/login');
    }
    return Promise.reject(error);
  }
);

export default client;
