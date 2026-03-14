import type { LLMSettings } from '@/types/systemSettings'
import { request } from './apiClient'

interface LLMTestResult {
  success: boolean
  message: string
  latency_ms?: number | null
}

export const systemSettingsService = {
  getLLMSettings(): Promise<LLMSettings> {
    return request.get<LLMSettings>('/admin/settings/llm').then((response) => {
      if (!response.data) {
        throw new Error('获取 LLM 配置失败')
      }
      return response.data
    })
  },
  updateLLMSettings(payload: { base_url: string; model: string; api_key?: string }): Promise<LLMSettings> {
    return request.put<LLMSettings>('/admin/settings/llm', payload).then((response) => {
      if (!response.data) {
        throw new Error('保存 LLM 配置失败')
      }
      return response.data
    })
  },
  testLLMSettings(payload: { base_url: string; model: string; api_key?: string }): Promise<LLMTestResult> {
    return request.post<LLMTestResult>('/admin/settings/llm/test', payload).then((response) => {
      if (!response.data) {
        throw new Error('测试 LLM 配置失败')
      }
      return response.data
    })
  }
}
