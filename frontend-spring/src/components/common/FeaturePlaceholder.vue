<template>
  <div class="page-shell">
    <div class="page-shell__content section-grid">
      <section class="glass-card feature">
        <p class="pill">{{ badge }}</p>
        <h2>{{ title }}</h2>
        <p class="feature__description">{{ description }}</p>
      </section>

      <section class="section-grid section-grid--two">
        <article class="glass-card feature__card">
          <h3>迁移模块</h3>
          <ul>
            <li v-for="item in modules" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article class="glass-card feature__card">
          <h3>对接重点</h3>
          <ul>
            <li v-for="item in integrations" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>

      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  badge: string
  title: string
  description: string
  modules: string[]
  integrations: string[]
}>()
</script>

<style scoped>
.feature {
  padding: 28px;
}

.feature__description {
  color: #496963;
  line-height: 1.7;
  margin-bottom: 0;
}

.feature__card {
  padding: 24px;
}

.feature__card ul {
  margin: 12px 0 0;
  padding-left: 18px;
  line-height: 1.8;
  color: #456660;
}
</style>
