<template>
  <AppLayout :items="doctorNavigation" />
</template>

<script setup lang="ts">
import AppLayout from '@/components/common/AppLayout.vue'
import { doctorNavigation } from '@/router/navigation'
</script>
