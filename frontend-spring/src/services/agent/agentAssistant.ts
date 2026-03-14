import { tokenManager } from '../http/session'
import type {
  AssistantActionResponse,
  AssistantStatePayload,
  ChatMessageItem,
  ChatSessionItem
} from '@/types/agent'

const baseURL = import.meta.env.VITE_AGENT_API_BASE_URL || '/agent-api/api/v1'

const authHeaders = (): HeadersInit => ({
  'Content-Type': 'application/json',
  ...(tokenManager.getToken() ? { Authorization: `Bearer ${tokenManager.getToken()}` } : {})
})

const extractErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  try {
    const payload = await response.json()
    if (typeof payload?.detail === 'string') {
      return payload.detail
    }
    if (Array.isArray(payload?.detail) && payload.detail.length > 0) {
      return payload.detail.map((item: { msg?: string }) => item.msg || '请求参数无效').join('; ')
    }
    if (typeof payload?.message === 'string') {
      return payload.message
    }
  } catch {
    // Ignore malformed JSON and use the fallback message.
  }

  try {
    const text = await response.text()
    return text || fallback
  } catch {
    return fallback
  }
}

const parseSseChunk = (chunk: string): Array<{ event: string; data: string }> => {
  const events: Array<{ event: string; data: string }> = []
  for (const block of chunk.split('\n\n').filter(Boolean)) {
    const lines = block.split('\n')
    let event = 'message'
    const dataLines: string[] = []
    for (const line of lines) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
    events.push({ event, data: dataLines.join('\n') })
  }
  return events
}

export const agentAssistantService = {
  async streamChat(
    payload: { public_session_id?: string; message: string; context?: Record<string, unknown> },
    handlers: {
      onToken?: (token: string) => void
      onState?: (state: AssistantStatePayload) => void
      onDone?: (payload: Record<string, unknown>) => void
      onError?: (message: string) => void
    }
  ): Promise<void> {
    const response = await fetch(`${baseURL}/assistant/chat/stream`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    })

    if (!response.ok || !response.body) {
      throw new Error(await extractErrorMessage(response, 'AI 对话请求失败'))
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const boundary = buffer.lastIndexOf('\n\n')
      if (boundary === -1) {
        continue
      }

      const complete = buffer.slice(0, boundary + 2)
      buffer = buffer.slice(boundary + 2)

      for (const event of parseSseChunk(complete)) {
        try {
          const data = event.data ? JSON.parse(event.data) : {}
          if (event.event === 'token') {
            handlers.onToken?.(String(data.text || ''))
          } else if (event.event === 'state') {
            handlers.onState?.(data as AssistantStatePayload)
          } else if (event.event === 'done') {
            handlers.onDone?.(data as Record<string, unknown>)
          } else if (event.event === 'error') {
            handlers.onError?.(String(data.message || 'AI 服务异常'))
          }
        } catch {
          handlers.onError?.('解析 SSE 数据失败')
        }
      }
    }
  },

  async executeAction(payload: {
    public_session_id: string
    action_type: 'confirm_appointment' | 'create_medicine_order'
    payload: Record<string, unknown>
  }): Promise<AssistantActionResponse> {
    const response = await fetch(`${baseURL}/assistant/actions/execute`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '执行助手动作失败'))
    }

    return response.json() as Promise<AssistantActionResponse>
  },

  async listSessions(): Promise<ChatSessionItem[]> {
    const response = await fetch(`${baseURL}/assistant/sessions?page=1&page_size=30`, {
      headers: authHeaders()
    })

    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '获取历史会话失败'))
    }

    const payload = (await response.json()) as { items?: ChatSessionItem[] }
    return payload.items || []
  },

  async listMessages(publicSessionId: string): Promise<ChatMessageItem[]> {
    const response = await fetch(`${baseURL}/assistant/sessions/${publicSessionId}/messages`, {
      headers: authHeaders()
    })

    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '获取历史消息失败'))
    }

    const payload = (await response.json()) as { items?: ChatMessageItem[] }
    return payload.items || []
  }
}
