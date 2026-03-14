import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useStudentAssistantStore = defineStore('studentAssistant', () => {
  const isOpen = ref(false)

  const openPanel = (): void => {
    isOpen.value = true
  }

  const closePanel = (): void => {
    isOpen.value = false
  }

  const togglePanel = (): void => {
    isOpen.value = !isOpen.value
  }

  return {
    isOpen,
    openPanel,
    closePanel,
    togglePanel
  }
})
