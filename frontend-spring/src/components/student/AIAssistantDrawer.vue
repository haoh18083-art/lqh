<template>
  <teleport to="body">
    <!-- Overlay -->
    <transition name="fade">
      <div
        v-if="open"
        class="ai-drawer-overlay"
        @click="$emit('close')"
      />
    </transition>

    <!-- Drawer -->
    <transition name="slide">
      <aside v-if="open" class="ai-drawer">
        <!-- Header -->
        <header class="ai-drawer__header">
          <div class="ai-drawer__title">
            <div class="ai-drawer__icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              </svg>
            </div>
            <div class="ai-drawer__title-text">
              <h3>AI 问诊助手</h3>
              <p>智能分诊与购药建议</p>
            </div>
          </div>
          <div class="ai-drawer__actions">
            <button
              class="ai-drawer__action-btn"
              title="新建对话"
              @click="startNewChat"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14"/>
              </svg>
            </button>
            <button
              class="ai-drawer__action-btn"
              title="历史对话"
              @click="showHistory = true"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </button>
            <button
              class="ai-drawer__close"
              title="关闭"
              @click="$emit('close')"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>
        </header>

        <!-- Notice -->
        <transition name="fade">
          <div v-if="feedbackMessage" class="ai-drawer__notice" :class="`ai-drawer__notice--${feedbackTone}`">
            {{ feedbackMessage }}
          </div>
        </transition>

        <!-- Messages -->
        <div ref="scrollContainer" class="ai-drawer__messages">
          <!-- Empty State -->
          <div v-if="messages.length === 0" class="ai-drawer__empty">
            <div class="ai-drawer__empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
              </svg>
            </div>
            <h4>开始新的对话</h4>
            <p>描述你的症状，AI助手将为你推荐合适的科室和医生</p>
            <div class="ai-drawer__suggestions">
              <button
                v-for="suggestion in quickSuggestions"
                :key="suggestion"
                class="ai-drawer__suggestion-chip"
                @click="inputValue = suggestion; handleSend()"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <!-- Message List -->
          <template v-else>
            <div
              v-for="message in messages"
              :key="message.id"
              class="ai-drawer__message"
              :class="`ai-drawer__message--${message.role}`"
            >
              <div class="ai-drawer__message-avatar">
                <template v-if="message.role === 'assistant'">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                  </svg>
                </template>
                <template v-else>
                  {{ userInitials }}
                </template>
              </div>
              <div class="ai-drawer__message-content">
                <div class="ai-drawer__message-bubble" :class="{ 'ai-drawer__message-bubble--streaming': message.streaming }">
                  {{ message.content || (message.streaming ? '思考中...' : '') }}
                </div>
                <span class="ai-drawer__message-time">{{ message.time }}</span>
              </div>
            </div>

            <!-- Cards -->
            <div v-if="doctorCards.length || medicineCards.length" class="ai-drawer__cards">
              <!-- Doctor Cards -->
              <div v-if="doctorCards.length" class="ai-drawer__card">
                <div class="ai-drawer__card-header">
                  <h4>推荐医生</h4>
                  <span class="ai-drawer__card-badge">挂号</span>
                </div>
                <div class="ai-drawer__card-list">
                  <div
                    v-for="doctor in doctorCards"
                    :key="doctor.doctor_id"
                    class="ai-drawer__doctor-item"
                  >
                    <div class="ai-drawer__doctor-info">
                      <strong>{{ doctor.doctor_name }}</strong>
                      <span>{{ doctor.department }} · {{ doctor.title }}</span>
                    </div>
                    <p class="ai-drawer__doctor-reason">{{ doctor.recommend_reason }}</p>
                    <div class="ai-drawer__slots">
                      <button
                        v-for="slot in doctor.slot_candidates.slice(0, 3)"
                        :key="`${doctor.doctor_id}-${slot.date}-${slot.time}`"
                        class="ai-drawer__slot-btn"
                        @click="handleDoctorAppointment(doctor, slot)"
                      >
                        {{ formatSlot(slot) }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Medicine Cards -->
              <div v-if="medicineCards.length" class="ai-drawer__card">
                <div class="ai-drawer__card-header">
                  <h4>推荐药品</h4>
                  <span class="ai-drawer__card-badge ai-drawer__card-badge--warning">购药</span>
                </div>
                <div class="ai-drawer__card-list">
                  <div
                    v-for="medicine in medicineCards"
                    :key="medicine.medicine_id"
                    class="ai-drawer__medicine-item"
                  >
                    <label class="ai-drawer__medicine-check">
                      <input
                        type="checkbox"
                        :checked="selectedMeds.includes(medicine.medicine_id)"
                        @change="toggleMedicine(medicine.medicine_id, $event)"
                      >
                      <div class="ai-drawer__medicine-info">
                        <strong>{{ medicine.name }}</strong>
                        <span>{{ medicine.spec || medicine.unit }}</span>
                      </div>
                    </label>
                    <p class="ai-drawer__medicine-reason">{{ medicine.recommend_reason }}</p>
                    <div class="ai-drawer__medicine-meta">
                      <span>库存: {{ medicine.stock }}</span>
                      <span class="ai-drawer__medicine-price">¥{{ medicine.price.toFixed(2) }}</span>
                    </div>
                    <div v-if="selectedMeds.includes(medicine.medicine_id)" class="ai-drawer__medicine-qty">
                      <label>
                        数量:
                        <input
                          type="number"
                          min="1"
                          :max="medicine.max_quantity"
                          :value="quantities[medicine.medicine_id] || 1"
                          @input="updateQuantity(medicine.medicine_id, $event)"
                        >
                      </label>
                    </div>
                  </div>
                </div>
                <button
                  class="ai-drawer__order-btn"
                  :disabled="selectedMeds.length === 0"
                  @click="handlePharmacyAction"
                >
                  确认下单 ({{ selectedMeds.length }}项)
                </button>
              </div>
            </div>
          </template>
        </div>

        <!-- Input -->
        <footer class="ai-drawer__input">
          <div class="ai-drawer__input-wrapper">
            <textarea
              v-model="inputValue"
              :disabled="isStreaming"
              placeholder="描述你的症状或问题..."
              rows="3"
              @keydown.enter.prevent="handleSend"
            />
            <button
              class="ai-drawer__send-btn"
              :disabled="!canSend"
              @click="handleSend"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
              </svg>
            </button>
          </div>
          <p class="ai-drawer__hint">按 Enter 发送，Shift + Enter 换行</p>
        </footer>
      </aside>
    </transition>

    <!-- History Drawer -->
    <transition name="slide-left">
      <aside v-if="showHistory" class="ai-history-drawer">
        <header class="ai-history-drawer__header">
          <h3>历史对话</h3>
          <button class="ai-history-drawer__close" @click="showHistory = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </header>
        <div class="ai-history-drawer__content">
          <div v-if="historyLoading" class="ai-history-drawer__loading">
            加载中...
          </div>
          <div v-else-if="sessions.length === 0" class="ai-history-drawer__empty">
            暂无历史对话
          </div>
          <button
            v-for="session in sessions"
            :key="session.public_session_id"
            class="ai-history-drawer__item"
            :class="{ 'ai-history-drawer__item--active': publicSessionId === session.public_session_id }"
            @click="loadSession(session)"
          >
            <strong>{{ session.title || '未命名对话' }}</strong>
            <span>{{ formatDate(session.last_message_at) }}</span>
          </button>
        </div>
      </aside>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { agentAssistantService } from '@/services/agent/agentAssistant'
import type {
  AssistantStatePayload,
  ChatMessageItem,
  ChatSessionItem,
  DoctorCard,
  MedicineCard
} from '@/types/agent'

interface Props {
  open: boolean
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  time: string
  streaming?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

const authStore = useAuthStore()

// State
const inputValue = ref('')
const isStreaming = ref(false)
const messages = ref<ChatMessage[]>([])
const publicSessionId = ref<string>()
const doctorCards = ref<DoctorCard[]>([])
const medicineCards = ref<MedicineCard[]>([])
const selectedMeds = ref<number[]>([])
const quantities = ref<Record<number, number>>({})
const feedbackMessage = ref('')
const feedbackTone = ref<'default' | 'success' | 'error'>('default')
const scrollContainer = ref<HTMLElement>()
const showHistory = ref(false)
const historyLoading = ref(false)
const sessions = ref<ChatSessionItem[]>([])

// Quick suggestions
const quickSuggestions = [
  '最近有点发热咳嗽',
  '头痛应该挂什么科',
  '肠胃不舒服怎么办'
]

// Computed
const canSend = computed(() => inputValue.value.trim().length > 0 && !isStreaming.value)
const userInitials = computed(() => {
  const name = authStore.user?.full_name || authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// Helpers - All times in Asia/Shanghai timezone
const SH_TIMEZONE = 'Asia/Shanghai'

const formatTime = (date = new Date()): string => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: SH_TIMEZONE
  })
}

const formatDate = (iso: string): string => {
  const date = new Date(iso)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: SH_TIMEZONE
  })
}

const formatSlot = (slot: { date: string; time: string }): string => {
  const date = new Date(slot.date)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day} ${slot.time}`
}

const scrollToBottom = async (): Promise<void> => {
  await nextTick()
  if (scrollContainer.value) {
    scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
  }
}

const showFeedback = (message: string, tone: 'default' | 'success' | 'error' = 'default'): void => {
  feedbackMessage.value = message
  feedbackTone.value = tone
  setTimeout(() => {
    feedbackMessage.value = ''
  }, 3000)
}

// Actions
const startNewChat = (): void => {
  messages.value = []
  publicSessionId.value = undefined
  doctorCards.value = []
  medicineCards.value = []
  selectedMeds.value = []
  quantities.value = {}
  inputValue.value = ''
}

const handleSend = async (): Promise<void> => {
  const text = inputValue.value.trim()
  if (!text || isStreaming.value) return

  // Add user message
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: text,
    time: formatTime()
  })

  // Add assistant placeholder
  const assistantId = `assistant-${Date.now()}`
  messages.value.push({
    id: assistantId,
    role: 'assistant',
    content: '',
    time: formatTime(),
    streaming: true
  })

  inputValue.value = ''
  isStreaming.value = true
  doctorCards.value = []
  medicineCards.value = []

  await scrollToBottom()

  try {
    await agentAssistantService.streamChat(
      {
        public_session_id: publicSessionId.value,
        message: text,
        context: { current_page: '/student/dashboard' }
      },
      {
        onToken: (token) => {
          const msg = messages.value.find(m => m.id === assistantId)
          if (msg) {
            msg.content += token
          }
          scrollToBottom()
        },
        onState: (state: AssistantStatePayload) => {
          publicSessionId.value = state.public_session_id
          doctorCards.value = state.doctor_cards || []
          medicineCards.value = state.medicine_cards || []

          // Auto-select medicines
          if (state.medicine_cards) {
            selectedMeds.value = state.medicine_cards.map(m => m.medicine_id)
            quantities.value = {}
            state.medicine_cards.forEach(m => {
              quantities.value[m.medicine_id] = m.default_quantity || 1
            })
          }

          const msg = messages.value.find(m => m.id === assistantId)
          if (msg && !msg.content && state.answer) {
            msg.content = state.answer
          }
        },
        onDone: () => {
          const msg = messages.value.find(m => m.id === assistantId)
          if (msg) {
            msg.streaming = false
          }
          isStreaming.value = false
        },
        onError: (error) => {
          showFeedback(error, 'error')
          const msg = messages.value.find(m => m.id === assistantId)
          if (msg) {
            msg.streaming = false
          }
          isStreaming.value = false
        }
      }
    )
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '发送失败', 'error')
    isStreaming.value = false
  }
}

const handleDoctorAppointment = async (doctor: DoctorCard, slot: { date: string; time: string }): Promise<void> => {
  if (!publicSessionId.value) {
    showFeedback('请先发送消息开始对话', 'error')
    return
  }

  try {
    const result = await agentAssistantService.executeAction({
      public_session_id: publicSessionId.value,
      action_type: 'confirm_appointment',
      payload: {
        doctor_id: doctor.doctor_id,
        date: slot.date,
        time: slot.time
      }
    })

    showFeedback(result.message || '预约成功', result.success ? 'success' : 'error')

    if (result.success) {
      messages.value.push({
        id: `action-${Date.now()}`,
        role: 'assistant',
        content: `✓ 预约成功：${doctor.doctor_name} - ${formatSlot(slot)}`,
        time: formatTime()
      })
    }
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '预约失败', 'error')
  }
}

const toggleMedicine = (id: number, event: Event): void => {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedMeds.value.push(id)
  } else {
    selectedMeds.value = selectedMeds.value.filter(m => m !== id)
  }
}

const updateQuantity = (id: number, event: Event): void => {
  const value = parseInt((event.target as HTMLInputElement).value, 10)
  quantities.value[id] = Math.max(1, value || 1)
}

const handlePharmacyAction = async (): Promise<void> => {
  if (!publicSessionId.value) {
    showFeedback('请先发送消息开始对话', 'error')
    return
  }

  const items = selectedMeds.value.map(id => ({
    medicine_id: id,
    quantity: quantities.value[id] || 1
  }))

  try {
    const result = await agentAssistantService.executeAction({
      public_session_id: publicSessionId.value,
      action_type: 'create_medicine_order',
      payload: { items }
    })

    showFeedback(result.message || '下单成功', result.success ? 'success' : 'error')

    if (result.success) {
      messages.value.push({
        id: `action-${Date.now()}`,
        role: 'assistant',
        content: `✓ 订单已创建，共 ${items.length} 种药品`,
        time: formatTime()
      })
    }
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '下单失败', 'error')
  }
}

// History
watch(showHistory, async (value) => {
  if (value) {
    historyLoading.value = true
    try {
      sessions.value = await agentAssistantService.listSessions()
    } catch (error) {
      showFeedback('加载历史会话失败', 'error')
    } finally {
      historyLoading.value = false
    }
  }
})

const loadSession = async (session: ChatSessionItem): Promise<void> => {
  if (isStreaming.value) {
    showFeedback('请等待当前回复完成', 'error')
    return
  }

  try {
    const items = await agentAssistantService.listMessages(session.public_session_id)
    messages.value = items.map((item: ChatMessageItem) => ({
      id: `${item.role}-${item.seq_no}`,
      role: item.role === 'user' ? 'user' : 'assistant',
      content: item.text || '',
      time: formatTime(new Date(item.created_at))
    }))
    publicSessionId.value = session.public_session_id
    doctorCards.value = []
    medicineCards.value = []
    showHistory.value = false
  } catch (error) {
    showFeedback('加载会话失败', 'error')
  }
}

// Watch for open prop
watch(() => props.open, (value) => {
  if (value) {
    scrollToBottom()
  }
})
</script>

<style scoped>
/* Overlay */
.ai-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 100;
}

/* Main Drawer */
.ai-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 420px;
  background: var(--color-white);
  border-left: 1px solid var(--border-color);
  box-shadow: var(--shadow-drawer);
  z-index: 101;
  display: flex;
  flex-direction: column;
}

.ai-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
  background: var(--color-gray-50);
}

.ai-drawer__title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.ai-drawer__icon {
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: var(--color-white);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-drawer__icon svg {
  width: 24px;
  height: 24px;
}

.ai-drawer__title-text h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.ai-drawer__title-text p {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.ai-drawer__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.ai-drawer__action-btn,
.ai-drawer__close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.ai-drawer__action-btn:hover {
  background: var(--color-gray-200);
  color: var(--text-primary);
}

.ai-drawer__close:hover {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.ai-drawer__action-btn svg,
.ai-drawer__close svg {
  width: 18px;
  height: 18px;
}

/* Notice */
.ai-drawer__notice {
  padding: var(--space-3) var(--space-4);
  font-size: 14px;
  animation: slideDown 0.2s ease;
}

.ai-drawer__notice--success {
  background: var(--color-success-soft);
  color: #059669;
}

.ai-drawer__notice--error {
  background: var(--color-danger-soft);
  color: #dc2626;
}

/* Messages */
.ai-drawer__messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
}

/* Empty State */
.ai-drawer__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-10) var(--space-6);
  color: var(--text-secondary);
}

.ai-drawer__empty-icon {
  width: 64px;
  height: 64px;
  color: var(--color-gray-300);
  margin-bottom: var(--space-4);
}

.ai-drawer__empty h4 {
  margin: 0 0 var(--space-2);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.ai-drawer__empty p {
  margin: 0 0 var(--space-5);
  font-size: 14px;
}

.ai-drawer__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: center;
}

.ai-drawer__suggestion-chip {
  padding: var(--space-2) var(--space-3);
  background: var(--color-gray-100);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.ai-drawer__suggestion-chip:hover {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Message Items */
.ai-drawer__message {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.ai-drawer__message--user {
  flex-direction: row-reverse;
}

.ai-drawer__message-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-gray-200);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.ai-drawer__message--assistant .ai-drawer__message-avatar {
  background: var(--color-primary);
  color: var(--color-white);
}

.ai-drawer__message-avatar svg {
  width: 18px;
  height: 18px;
}

.ai-drawer__message-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  max-width: 80%;
}

.ai-drawer__message-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--color-gray-100);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.ai-drawer__message--user .ai-drawer__message-bubble {
  background: var(--color-primary);
  color: var(--color-white);
}

.ai-drawer__message-bubble--streaming::after {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-left: 4px;
  background: currentColor;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

.ai-drawer__message-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* Cards */
.ai-drawer__cards {
  margin: var(--space-4) 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ai-drawer__card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.ai-drawer__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--color-gray-50);
  border-bottom: 1px solid var(--border-color);
}

.ai-drawer__card-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.ai-drawer__card-badge {
  padding: var(--space-1) var(--space-2);
  background: var(--color-primary);
  color: var(--color-white);
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--radius-sm);
}

.ai-drawer__card-badge--warning {
  background: var(--color-warning);
}

.ai-drawer__card-list {
  padding: var(--space-3);
}

/* Doctor Item */
.ai-drawer__doctor-item {
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-2);
}

.ai-drawer__doctor-item:last-child {
  margin-bottom: 0;
}

.ai-drawer__doctor-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.ai-drawer__doctor-info strong {
  font-size: 14px;
  color: var(--text-primary);
}

.ai-drawer__doctor-info span {
  font-size: 12px;
  color: var(--text-secondary);
}

.ai-drawer__doctor-reason {
  margin: 0 0 var(--space-3);
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.ai-drawer__slots {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.ai-drawer__slot-btn {
  padding: var(--space-1) var(--space-3);
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.ai-drawer__slot-btn:hover {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Medicine Item */
.ai-drawer__medicine-item {
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-2);
}

.ai-drawer__medicine-item:last-child {
  margin-bottom: 0;
}

.ai-drawer__medicine-check {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.ai-drawer__medicine-check input {
  margin-top: 2px;
}

.ai-drawer__medicine-info strong {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
}

.ai-drawer__medicine-info span {
  font-size: 12px;
  color: var(--text-secondary);
}

.ai-drawer__medicine-reason {
  margin: var(--space-2) 0;
  padding-left: var(--space-6);
  font-size: 13px;
  color: var(--text-secondary);
}

.ai-drawer__medicine-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-left: var(--space-6);
  margin-bottom: var(--space-2);
  font-size: 12px;
  color: var(--text-secondary);
}

.ai-drawer__medicine-price {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.ai-drawer__medicine-qty {
  padding-left: var(--space-6);
}

.ai-drawer__medicine-qty label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: 13px;
  color: var(--text-secondary);
}

.ai-drawer__medicine-qty input {
  width: 60px;
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 13px;
}

.ai-drawer__order-btn {
  width: calc(100% - var(--space-6));
  margin: var(--space-3);
  padding: var(--space-3);
  background: var(--color-primary);
  color: var(--color-white);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.ai-drawer__order-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.ai-drawer__order-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Input */
.ai-drawer__input {
  padding: var(--space-4);
  border-top: 1px solid var(--border-color);
  background: var(--color-white);
}

.ai-drawer__input-wrapper {
  position: relative;
}

.ai-drawer__input-wrapper textarea {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  padding-right: 48px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  background: var(--color-gray-50);
  font-size: 14px;
  resize: none;
  transition: all var(--transition-fast);
}

.ai-drawer__input-wrapper textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-white);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.ai-drawer__input-wrapper textarea::placeholder {
  color: var(--text-tertiary);
}

.ai-drawer__send-btn {
  position: absolute;
  right: var(--space-2);
  bottom: var(--space-2);
  width: 36px;
  height: 36px;
  background: var(--color-primary);
  color: var(--color-white);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.ai-drawer__send-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.ai-drawer__send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-drawer__send-btn svg {
  width: 18px;
  height: 18px;
}

.ai-drawer__hint {
  margin: var(--space-2) 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: center;
}

/* History Drawer */
.ai-history-drawer {
  position: fixed;
  top: 0;
  right: 420px;
  bottom: 0;
  width: 320px;
  background: var(--color-white);
  border-left: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
  z-index: 102;
  display: flex;
  flex-direction: column;
}

.ai-history-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.ai-history-drawer__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.ai-history-drawer__close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
}

.ai-history-drawer__close:hover {
  background: var(--color-gray-100);
  color: var(--text-primary);
}

.ai-history-drawer__close svg {
  width: 18px;
  height: 18px;
}

.ai-history-drawer__content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3);
}

.ai-history-drawer__loading,
.ai-history-drawer__empty {
  padding: var(--space-6);
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.ai-history-drawer__item {
  width: 100%;
  padding: var(--space-3);
  text-align: left;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-2);
  background: var(--color-white);
  transition: all var(--transition-fast);
}

.ai-history-drawer__item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.ai-history-drawer__item--active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.ai-history-drawer__item strong {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-history-drawer__item span {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform 0.2s ease;
}

.slide-left-enter-from,
.slide-left-leave-to {
  transform: translateX(100%);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .ai-drawer {
    width: 100%;
  }

  .ai-history-drawer {
    right: 0;
    z-index: 103;
  }
}
</style>
