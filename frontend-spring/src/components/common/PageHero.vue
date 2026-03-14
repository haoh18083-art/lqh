<template>
  <header class="page-hero">
    <div class="page-hero__content">
      <span class="page-hero__eyebrow">{{ eyebrow }}</span>
      <h1 class="page-hero__title">{{ title }}</h1>
      <p v-if="description" class="page-hero__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-hero__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-6) 0;
  margin-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.page-hero__content {
  flex: 1;
}

.page-hero__eyebrow {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-primary);
  margin-bottom: var(--space-2);
}

.page-hero__title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.page-hero__description {
  margin: var(--space-2) 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.page-hero__actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .page-hero {
    flex-direction: column;
    gap: var(--space-4);
  }

  .page-hero__title {
    font-size: 24px;
  }
}
</style>
