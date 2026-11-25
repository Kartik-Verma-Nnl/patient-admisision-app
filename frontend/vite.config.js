import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api calls are proxied to the backend (Java later, mock for now).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
