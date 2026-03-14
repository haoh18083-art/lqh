<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Settings"
        title="系统设置"
        description="真实对接 LLM 配置读取、测试和保存接口，保持现有 OpenAI 兼容配置流程。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadSettings">刷新配置</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="split-grid">
        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>模型连接参数</h3>
              <p class="dashboard-muted">保存前可先做连通性测试，API Key 留空表示保留现有密钥。</p>
            </div>
          </div>
          <div class="form-grid">
            <div class="field">
              <label>API Base URL</label>
              <input v-model.trim="form.base_url" placeholder="https://api.openai.com/v1" />
            </div>
            <div class="field">
              <label>模型名称</label>
              <input v-model.trim="form.model" placeholder="gpt-4o-mini / deepseek-chat" />
            </div>
            <div class="field">
              <label>API Key</label>
              <input v-model.trim="form.api_key" type="password" placeholder="sk-..." />
            </div>
            <div class="notice">
              当前密钥状态：{{ settings.has_api_key ? settings.api_key_masked || '已配置' : '未配置' }}
            </div>
            <div class="toolbar">
              <button class="button button--secondary" :disabled="testing" @click="testConnection">
                {{ testing ? '测试中...' : '测试连接' }}
              </button>
              <button class="button" :disabled="saving" @click="saveSettings">
                {{ saving ? '保存中...' : '保存配置' }}
              </button>
            </div>
          </div>
        </article>

        <article class="glass-card panel stack">
          <h3>当前状态</h3>
          <div class="stat-grid">
            <article class="glass-card stat-card">
              <div class="stat-card__label">配置状态</div>
              <div class="stat-card__value">{{ settings.is_configured ? 'Ready' : 'Pending' }}</div>
            </article>
            <article class="glass-card stat-card">
              <div class="stat-card__label">最近测试</div>
              <div class="stat-card__value">{{ effectiveTestStatus }}</div>
            </article>
          </div>
          <div class="notice">
            <strong>测试结果</strong>
            <p>{{ effectiveTestMessage }}</p>
          </div>
          <div class="notice">
            <strong>最近更新时间</strong>
            <p>{{ settings.updated_at ? formatDateTime(settings.updated_at) : '暂无更新记录' }}</p>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import { systemSettingsService } from '@/services/http/systemSettings'
import type { LLMSettings } from '@/types/systemSettings'
import { formatDateTime } from '@/utils/format'

const defaultState: LLMSettings = {
  base_url: '',
  model: '',
  has_api_key: false,
  api_key_masked: null,
  is_configured: false,
  last_test_status: 'unknown',
  last_test_message: null,
  last_tested_at: null,
  updated_at: null,
  updated_by: null
}

const settings = reactive<LLMSettings>({ ...defaultState })
const form = reactive({
  base_url: '',
  model: '',
  api_key: ''
})

const saving = ref(false)
const testing = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const lastTestResult = ref<{ success: boolean; message: string } | null>(null)

const effectiveTestStatus = computed(() => {
  if (lastTestResult.value) return lastTestResult.value.success ? 'success' : 'failed'
  return settings.last_test_status
})

const effectiveTestMessage = computed(() => {
  if (lastTestResult.value) return lastTestResult.value.message
  return settings.last_test_message || '尚未执行连接测试'
})

const syncState = (payload: LLMSettings): void => {
  Object.assign(settings, payload)
  form.base_url = payload.base_url
  form.model = payload.model
  form.api_key = ''
}

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadSettings = async (): Promise<void> => {
  resetMessages()
  try {
    const response = await systemSettingsService.getLLMSettings()
    syncState(response)
    lastTestResult.value = null
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取系统设置失败'
  }
}

const testConnection = async (): Promise<void> => {
  resetMessages()
  testing.value = true
  try {
    const response = await systemSettingsService.testLLMSettings({
      base_url: form.base_url.trim(),
      model: form.model.trim(),
      api_key: form.api_key.trim() || undefined
    })
    lastTestResult.value = response as { success: boolean; message: string }
    successMessage.value = response.success ? '连接测试成功，可直接保存配置。' : '连接测试返回失败结果。'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '连接测试失败'
  } finally {
    testing.value = false
  }
}

const saveSettings = async (): Promise<void> => {
  resetMessages()
  saving.value = true
  try {
    const response = await systemSettingsService.updateLLMSettings({
      base_url: form.base_url.trim(),
      model: form.model.trim(),
      api_key: form.api_key.trim() || undefined
    }) as LLMSettings
    syncState(response)
    successMessage.value = '配置已保存'
    lastTestResult.value = null
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存配置失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadSettings()
})
</script>

<style scoped>
.dashboard-muted {
  color: var(--muted);
  margin: 8px 0 0;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
</style>
