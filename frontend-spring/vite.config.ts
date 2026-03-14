import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://backend-spring:8080'
  const agentProxyTarget = env.VITE_AGENT_PROXY_TARGET || 'http://langgraph-app:8001'
  const wsProxyTarget = env.VITE_WS_PROXY_TARGET || apiProxyTarget

  return {
    plugins: [vue()],
    define: {
      global: 'globalThis'
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    optimizeDeps: {
      esbuildOptions: {
        define: {
          global: 'globalThis'
        }
      }
    },
    server: {
      host: '0.0.0.0',
      port: 3000,
      allowedHosts: ['frontend-spring', 'campus-medical-frontend-spring', 'localhost', '127.0.0.1'],
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true
        },
        '/agent-api': {
          target: agentProxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/agent-api/, '')
        },
        '/ws': {
          target: wsProxyTarget,
          changeOrigin: true,
          ws: true
        }
      }
    },
    preview: {
      host: '0.0.0.0',
      port: 4173
    }
  }
})
