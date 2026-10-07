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

export default client;
