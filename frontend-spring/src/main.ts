import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores/pinia'
import './styles/main.css'

const globalScope = globalThis as typeof globalThis & { global?: typeof globalThis }
if (!globalScope.global) {
  globalScope.global = globalScope
}

const app = createApp(App)

app.use(pinia)
app.use(router)
app.mount('#app')
