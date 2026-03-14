<template>
  <div class="app-layout">
    <AppSidebar
      :items="items"
      :user="authStore.user"
      @logout="handleLogout"
    />
    <main class="app-layout__main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import { useAuthStore } from '@/stores/auth'
import type { NavItem } from '@/router/navigation'

interface Props {
  items: NavItem[]
}

defineProps<Props>()

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = async (): Promise<void> => {
  await authStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-secondary);
}

.app-layout__main {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
}

@media (max-width: 768px) {
  .app-layout__main {
    margin-left: 0;
  }
}
</style>
