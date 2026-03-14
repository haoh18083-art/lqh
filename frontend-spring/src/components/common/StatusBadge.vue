<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <span v-if="tone === 'success'" class="status-badge__dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--default {
  background: var(--color-gray-100);
  color: var(--text-secondary);
}

.status-badge--success {
  background: var(--color-success-soft);
  color: #059669;
}

.status-badge--warning {
  background: var(--color-warning-soft);
  color: #d97706;
}

.status-badge--danger {
  background: var(--color-danger-soft);
  color: #dc2626;
}

.status-badge--info {
  background: var(--color-info-soft);
  color: #4f46e5;
}
</style>
