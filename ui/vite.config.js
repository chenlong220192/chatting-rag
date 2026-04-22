import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8001'

  return {
    plugins: [react()],
    server: {
      port: Number(env.VITE_DEV_SERVER_PORT) || 8000,
      proxy: {
        '/dev-api': { target: apiTarget, changeOrigin: true, rewrite: (path) => path.replace(/^\/dev-api/, '/api') },
        '/staging-api': { target: apiTarget, changeOrigin: true, rewrite: (path) => path.replace(/^\/staging-api/, '/api') },
        '/prod-api': { target: apiTarget, changeOrigin: true, rewrite: (path) => path.replace(/^\/prod-api/, '/api') },
        '/api': { target: apiTarget, changeOrigin: true }
      }
    },
    build: {
      outDir: 'dist'
    }
  }
})
