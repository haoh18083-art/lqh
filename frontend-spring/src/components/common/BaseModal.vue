<template>
  <Teleport to="body">
    <div v-if="open" class="modal-overlay" @click.self="$emit('close')">
      <section class="modal-panel glass-card" :class="sizeClass">
        <header class="modal-panel__header">
          <div>
            <p v-if="eyebrow" class="pill">{{ eyebrow }}</p>
            <h3>{{ title }}</h3>
          </div>
          <button type="button" class="modal-panel__close" @click="$emit('close')">关闭</button>
        </header>
        <div class="modal-panel__content">
          <slot />
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  eyebrow?: string
  size?: 'sm' | 'md' | 'lg'
}>()

defineEmits<{
  (event: 'close'): void
}>()

const sizeClass = computed(() => `modal-panel--${props.size || 'md'}`)
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(13, 26, 24, 0.44);
  backdrop-filter: blur(8px);
}

.modal-panel {
  width: min(100%, 760px);
  max-height: min(88vh, 960px);
  overflow: auto;
  padding: 24px;
}

.modal-panel--sm {
  max-width: 520px;
}

.modal-panel--md {
  max-width: 760px;
}

.modal-panel--lg {
  max-width: 1040px;
}

.modal-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.modal-panel__header h3 {
  margin: 10px 0 0;
}

.modal-panel__close {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  background: rgba(24, 49, 45, 0.08);
  color: #18312d;
  cursor: pointer;
}
</style>
