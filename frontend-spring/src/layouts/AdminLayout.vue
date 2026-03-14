<template>
  <AppLayout :items="adminNavigation" />
</template>

<script setup lang="ts">
import AppLayout from '@/components/common/AppLayout.vue'
import { adminNavigation } from '@/router/navigation'
</script>
