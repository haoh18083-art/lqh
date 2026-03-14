import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { AlertEnvelope, SocketStatus } from '@/types/socket'
import { alertSocketClient } from '@/services/ws/alertSocketClient'

export const useSocketStore = defineStore('socket', () => {
  const status = ref<SocketStatus>('idle')
  const messages = ref<AlertEnvelope[]>([])

  const connect = (): void => {
    alertSocketClient.connect(
      (message) => {
        messages.value = [message, ...messages.value].slice(0, 20)
      },
      (nextStatus) => {
        status.value = nextStatus as SocketStatus
      }
    )
  }

  const disconnect = (): void => {
    alertSocketClient.disconnect()
    status.value = 'disconnected'
  }

  return {
    status,
    messages,
    connect,
    disconnect
  }
})
