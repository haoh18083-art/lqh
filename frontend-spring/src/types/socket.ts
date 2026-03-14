export type SocketStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error'

export interface AlertEnvelope<T = Record<string, unknown>> {
  topic: string
  payload: T
  received_at: string
}
