<template>
  <div class="student-layout">
    <AppSidebar
      :items="studentNavigation"
      :user="authStore.user"
      @logout="handleLogout"
    />

    <main class="student-layout__main">
      <RouterView />

      <!-- AI Assistant Toggle Button -->
      <button
        v-if="isDashboard"
        class="ai-toggle-btn"
        :class="{ 'ai-toggle-btn--active': assistantStore.isOpen }"
        @click="assistantStore.togglePanel"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
        </svg>
        <span>AI助手</span>
      </button>
    </main>

    <!-- AI Assistant Drawer -->
    <AIAssistantDrawer
      :open="assistantStore.isOpen"
      @close="assistantStore.closePanel"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/common/AppSidebar.vue'
import AIAssistantDrawer from '@/components/student/AIAssistantDrawer.vue'
import { useAuthStore } from '@/stores/auth'
import { useStudentAssistantStore } from '@/stores/studentAssistant'
import { studentNavigation } from '@/router/navigation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const assistantStore = useStudentAssistantStore()

const isDashboard = computed(() => route.path === '/student/dashboard')

const handleLogout = async (): Promise<void> => {
  assistantStore.closePanel()
  await authStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.student-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-secondary);
}

.student-layout__main {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  position: relative;
}

.ai-toggle-btn {
  position: fixed;
  right: var(--space-4);
  bottom: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-lg);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  z-index: 50;
}

.ai-toggle-btn:hover {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-white);
}

.ai-toggle-btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-white);
}

.ai-toggle-btn svg {
  width: 20px;
  height: 20px;
}

@media (max-width: 768px) {
  .student-layout__main {
    margin-left: 0;
  }

  .ai-toggle-btn span {
    display: none;
  }
}
</style>
