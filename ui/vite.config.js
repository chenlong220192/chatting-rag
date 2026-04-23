import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import checker from 'vite-plugin-checker'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8001'

  // Validate required env vars
  if (!env.VITE_API_BASE_URL) {
    throw new Error(`Missing required env var: VITE_API_BASE_URL (mode: ${mode})`)
  }

  return {
    plugins: [
      react(),
      checker({ typescript: true }),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: Number(env.VITE_DEV_SERVER_PORT) || 8000,
      proxy: {
        // Note: all 4 entries exist to support switching between environments
        // on the same dev machine. Only the entry matching VITE_API_BASE_URL
        // is active at runtime (Vite only loads one .env file per mode).
        [env.VITE_API_BASE_URL]: {
          target: apiTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp('^' + env.VITE_API_BASE_URL), '/api'),
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
    },
  }
})
