export interface LLMSettings {
  base_url: string
  model: string
  has_api_key: boolean
  api_key_masked?: string | null
  is_configured: boolean
  last_test_status: 'unknown' | 'success' | 'failed'
  last_test_message?: string | null
  last_tested_at?: string | null
  updated_at?: string | null
  updated_by?: number | null
}
