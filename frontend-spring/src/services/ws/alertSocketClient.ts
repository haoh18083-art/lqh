import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { AlertEnvelope } from '@/types/socket'

const endpoint = import.meta.env.VITE_WS_BASE_URL || '/ws/alerts'

export class AlertSocketClient {
  private client: Client | null = null

  connect(onMessage: (message: AlertEnvelope) => void, onStatus?: (status: string) => void): void {
    if (this.client?.active) {
      return
    }

    this.client = new Client({
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS(endpoint),
      onConnect: () => {
        onStatus?.('connected')
        this.client?.subscribe('/topic/alerts', (message) => {
          onMessage(this.toEnvelope('/topic/alerts', message))
        })
      },
      onWebSocketClose: () => onStatus?.('disconnected'),
      onStompError: () => onStatus?.('error')
    })

    onStatus?.('connecting')
    this.client.activate()
  }

  disconnect(): void {
    this.client?.deactivate()
    this.client = null
  }

  private toEnvelope(topic: string, message: IMessage): AlertEnvelope {
    let payload: Record<string, unknown> = {}
    try {
      payload = JSON.parse(message.body) as Record<string, unknown>
    } catch {
      payload = { raw: message.body }
    }

    return {
      topic,
      payload,
      received_at: new Date().toISOString()
    }
  }
}

export const alertSocketClient = new AlertSocketClient()
