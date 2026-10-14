import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  // sockjs-client (used for live updates) expects a Node-style "global" variable.
  define: {
    global: 'globalThis',
  },
})
