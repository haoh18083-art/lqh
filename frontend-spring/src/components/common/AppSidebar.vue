<template>
  <aside class="app-sidebar" :class="{ 'app-sidebar--collapsed': collapsed }">
    <div class="app-sidebar__header">
      <div class="app-sidebar__logo">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
        </svg>
        <span v-if="!collapsed" class="app-sidebar__title">校园医疗</span>
      </div>
    </div>

    <nav class="app-sidebar__nav">
      <RouterLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="app-sidebar__nav-item"
        :class="{ 'app-sidebar__nav-item--active': isActive(item.to) }"
        :title="collapsed ? item.label : undefined"
      >
        <span class="app-sidebar__nav-icon">
          <NavIcon :name="getIconName(item.label)" />
        </span>
        <span v-if="!collapsed" class="app-sidebar__nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="app-sidebar__footer">
      <div class="app-sidebar__user" :class="{ 'app-sidebar__user--collapsed': collapsed }">
        <div class="app-sidebar__avatar">
          {{ userInitials }}
        </div>
        <div v-if="!collapsed" class="app-sidebar__user-info">
          <span class="app-sidebar__user-name">{{ user?.full_name || user?.username || '用户' }}</span>
          <span class="app-sidebar__user-role">{{ roleLabel }}</span>
        </div>
      </div>
      <button
        v-if="!collapsed"
        class="app-sidebar__logout"
        title="退出登录"
        @click="handleLogout"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4m7 14l5-5m0 0l-5-5m5 5H9"/>
        </svg>
      </button>
    </div>

    <button class="app-sidebar__toggle" @click="toggleSidebar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path v-if="collapsed" d="M9 5l7 7-7 7"/>
        <path v-else d="M15 19l-7-7 7-7"/>
      </svg>
    </button>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { NavItem } from '@/router/navigation'
import type { UserInfo } from '@/types/auth'
import NavIcon from './NavIcon.vue'

interface Props {
  items: NavItem[]
  user: UserInfo | null
}

const props = defineProps<Props>()
const route = useRoute()
const router = useRouter()

const collapsed = ref(false)

const userInitials = computed(() => {
  const name = props.user?.full_name || props.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const roleMap: Record<string, string> = {
    student: '学生',
    doctor: '医生',
    admin: '管理员'
  }
  return roleMap[props.user?.role || ''] || '用户'
})

const isActive = (path: string): boolean => {
  return route.path === path || route.path.startsWith(`${path}/`)
}

const getIconName = (label: string): string => {
  const iconMap: Record<string, string> = {
    '健康总览': 'dashboard',
    '问诊助手': 'message-circle',
    '自助药房': 'shopping-bag',
    '预约管理': 'calendar',
    '购药记录': 'file-text',
    '接诊台': 'activity',
    '我的排班': 'clock',
    '运营总览': 'bar-chart',
    '排班管理': 'calendar-check',
    '医生管理': 'users',
    '学生管理': 'graduation-cap',
    '药品管理': 'pill',
    '系统设置': 'settings'
  }
  return iconMap[label] || 'circle'
}

const toggleSidebar = (): void => {
  collapsed.value = !collapsed.value
}

const handleLogout = async (): Promise<void> => {
  // Emit event to parent for logout handling
  emit('logout')
}

const emit = defineEmits<{
  logout: []
}>()
</script>

<style scoped>
.app-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 240px;
  background: var(--color-white);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  z-index: 50;
  transition: width var(--transition-base);
}

.app-sidebar--collapsed {
  width: 64px;
}

.app-sidebar__header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.app-sidebar__logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-primary);
}

.app-sidebar__logo svg {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.app-sidebar__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.app-sidebar__nav {
  flex: 1;
  padding: var(--space-3);
  overflow-y: auto;
}

.app-sidebar__nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-3);
  margin-bottom: var(--space-1);
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.app-sidebar__nav-item:hover {
  background: var(--color-gray-100);
  color: var(--text-primary);
}

.app-sidebar__nav-item--active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.app-sidebar__nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.app-sidebar__nav-label {
  white-space: nowrap;
}

.app-sidebar__footer {
  padding: var(--space-3);
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.app-sidebar__user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  min-width: 0;
}

.app-sidebar__user--collapsed {
  justify-content: center;
}

.app-sidebar__avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.app-sidebar__user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-sidebar__user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-sidebar__user-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.app-sidebar__logout {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.app-sidebar__logout:hover {
  background: var(--color-gray-100);
  color: var(--color-danger);
}

.app-sidebar__logout svg {
  width: 18px;
  height: 18px;
}

.app-sidebar__toggle {
  position: absolute;
  right: -12px;
  top: 72px;
  width: 24px;
  height: 24px;
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);
}

.app-sidebar__toggle:hover {
  background: var(--color-gray-50);
  color: var(--text-primary);
}

.app-sidebar__toggle svg {
  width: 14px;
  height: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .app-sidebar {
    transform: translateX(-100%);
  }

  .app-sidebar--open {
    transform: translateX(0);
  }
}
</style>
