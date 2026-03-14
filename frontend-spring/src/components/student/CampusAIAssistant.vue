<template>
  <aside class="campus-ai-panel" :class="{ 'campus-ai-panel--open': open }" data-testid="assistant-panel">
    <div class="campus-ai-panel__frame glass-card">
      <header class="campus-ai-panel__header">
        <div>
          <p class="pill">校园 AI 问诊助手</p>
          <h3>问诊、挂号与购药建议</h3>
          <p class="campus-ai-panel__subtitle">基于 Agent 服务生成建议，医疗结论仅供校园初步分诊参考。</p>
        </div>
        <div class="campus-ai-panel__header-actions">
          <button type="button" class="button button--ghost campus-ai-panel__header-button" @click="openHistory">
            历史对话
          </button>
          <button type="button" class="campus-ai-panel__close" aria-label="关闭 AI 问诊助手" @click="$emit('close')">
            关闭
          </button>
        </div>
      </header>

      <div v-if="feedbackMessage" class="notice campus-ai-panel__notice" :class="noticeClass">
        {{ feedbackMessage }}
      </div>

      <div class="campus-ai-panel__body">
        <div ref="scrollViewport" class="campus-ai-panel__scroll">
          <div class="campus-ai-panel__system-chip">校园 AI 问诊助手 · 支持流式问答、推荐挂号与购药动作</div>

          <div v-if="messages.length === 0" class="campus-ai-panel__empty">
            <strong>先描述症状或你的当前需求。</strong>
            <p>例如：发热、咳嗽持续两天，想知道该挂哪个科室，以及是否需要先备药。</p>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="campus-ai-message"
            :class="`campus-ai-message--${message.role}`"
          >
            <span class="campus-ai-message__time">{{ message.time }}</span>
            <div class="campus-ai-message__bubble" :class="{ 'campus-ai-message__bubble--streaming': message.streaming }">
              {{ message.content || (message.streaming ? '正在生成建议...' : '（无文本内容）') }}
            </div>
          </article>

          <section v-if="doctorCards.length || medicineCards.length" class="campus-ai-suggestions">
            <article v-if="doctorCards.length" class="campus-ai-card">
              <div class="campus-ai-card__header">
                <div>
                  <h4>医生挂号建议</h4>
                  <p>根据当前会话推荐合适医生，点击时段后直接提交挂号。</p>
                </div>
                <span class="pill">挂号</span>
              </div>

              <div class="campus-ai-card__list">
                <article v-for="doctor in doctorCards" :key="doctor.doctor_id" class="campus-ai-recommendation">
                  <div class="campus-ai-recommendation__head">
                    <div>
                      <strong>{{ doctor.doctor_name }}</strong>
                      <p>{{ doctor.department }} · {{ doctor.title }}</p>
                    </div>
                    <span class="campus-ai-recommendation__tag">推荐</span>
                  </div>
                  <p class="campus-ai-recommendation__reason">{{ doctor.recommend_reason }}</p>
                  <div class="campus-ai-slot-list">
                    <button
                      v-for="slot in doctor.slot_candidates"
                      :key="`${doctor.doctor_id}-${slot.date}-${slot.time}`"
                      type="button"
                      class="button campus-ai-slot-button"
                      @click="handleDoctorAppointment(doctor, slot)"
                    >
                      {{ slot.date }} {{ slot.time }}
                    </button>
                  </div>
                </article>
              </div>
            </article>

            <article v-if="medicineCards.length" class="campus-ai-card">
              <div class="campus-ai-card__header">
                <div>
                  <h4>药品购买建议</h4>
                  <p>勾选药品并调整数量后，可直接调用 Agent 下单。</p>
                </div>
                <span class="pill">购药</span>
              </div>

              <div class="campus-ai-card__list">
                <article v-for="medicine in medicineCards" :key="medicine.medicine_id" class="campus-ai-recommendation">
                  <label class="campus-ai-medicine">
                    <input
                      :checked="selectedMeds.includes(medicine.medicine_id)"
                      type="checkbox"
                      @change="handleMedicineSelectionChange(medicine.medicine_id, $event)"
                    />
                    <div>
                      <strong>{{ medicine.name }}</strong>
                      <p>{{ medicine.spec || medicine.unit || '常规规格' }}</p>
                    </div>
                  </label>
                  <p class="campus-ai-recommendation__reason">{{ medicine.recommend_reason }}</p>
                  <div class="campus-ai-medicine__meta">
                    <span>库存 {{ medicine.stock }}</span>
                    <span>¥{{ medicine.price.toFixed(2) }}</span>
                  </div>
                  <div class="campus-ai-medicine__actions">
                    <label class="campus-ai-medicine__quantity">
                      <span>数量</span>
                      <input
                        :max="medicine.max_quantity"
                        :value="quantities[medicine.medicine_id] || medicine.default_quantity || 1"
                        min="1"
                        type="number"
                        @input="updateQuantity(medicine.medicine_id, $event)"
                      />
                    </label>
                  </div>
                </article>
              </div>

              <button
                type="button"
                class="button campus-ai-panel__action-button"
                :disabled="selectedMeds.length === 0"
                @click="handlePharmacyAction"
              >
                一键下单
              </button>
            </article>
          </section>
        </div>

        <footer class="campus-ai-panel__composer">
          <textarea
            v-model="inputValue"
            class="campus-ai-panel__textarea"
            :disabled="isStreaming"
            placeholder="描述你的症状、既往情况或希望解决的问题…"
            rows="3"
            @keydown.ctrl.enter.prevent="handleSendClick"
            @keydown.meta.enter.prevent="handleSendClick"
          />
          <div class="campus-ai-panel__composer-footer">
            <span class="campus-ai-panel__hint">发送快捷键：Ctrl/Cmd + Enter</span>
            <button type="button" class="button" :disabled="!canSend" @click="handleSendClick">发送</button>
          </div>
        </footer>
      </div>
    </div>

    <BaseModal :open="historyOpen" eyebrow="Student / Assistant" size="sm" title="历史对话" @close="historyOpen = false">
      <div class="campus-ai-history">
        <div v-if="historyLoading" class="notice">历史会话加载中...</div>
        <div v-else-if="sessions.length === 0" class="notice">暂无历史会话。</div>
        <button
          v-for="session in sessions"
          :key="session.public_session_id"
          type="button"
          class="campus-ai-history__item"
          :class="{ 'campus-ai-history__item--active': publicSessionId === session.public_session_id }"
          @click="loadSessionMessages(session)"
        >
          <strong>{{ formatSessionTitle(session) }}</strong>
          <p class="campus-ai-history__meta">最近活跃：{{ formatDateTime(session.last_message_at) }}</p>
          <p class="campus-ai-history__meta">创建时间：{{ formatDateTime(session.created_at) }}</p>
        </button>
      </div>
    </BaseModal>
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
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

defineEmits<{
  (event: 'close'): void
}>()

const inputValue = ref('')
const isStreaming = ref(false)
const publicSessionId = ref<string>()
const messages = ref<ChatMessage[]>([])
const doctorCards = ref<DoctorCard[]>([])
const medicineCards = ref<MedicineCard[]>([])
const selectedMeds = ref<number[]>([])
const quantities = ref<Record<number, number>>({})
const historyOpen = ref(false)
const historyLoading = ref(false)
const sessions = ref<ChatSessionItem[]>([])
const feedbackTone = ref<'default' | 'success' | 'error'>('default')
const feedbackMessage = ref('')
const scrollViewport = ref<HTMLElement | null>(null)

const canSend = computed(() => inputValue.value.trim().length > 0 && !isStreaming.value)
const noticeClass = computed(() => ({
  'notice--success': feedbackTone.value === 'success',
  'notice--error': feedbackTone.value === 'error'
}))

const buildTimeLabel = (date = new Date()): string => {
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${hours}:${minutes}`
}

const buildTimeLabelFromISO = (isoText: string): string => {
  const date = new Date(isoText)
  if (Number.isNaN(date.getTime())) {
    return buildTimeLabel()
  }
  return buildTimeLabel(date)
}

const formatDateTime = (isoText: string): string => {
  const date = new Date(isoText)
  if (Number.isNaN(date.getTime())) {
    return isoText
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const createId = (prefix: string): string => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

const setFeedback = (message: string, tone: 'default' | 'success' | 'error' = 'default'): void => {
  feedbackMessage.value = message
  feedbackTone.value = tone
}

const clearFeedback = (): void => {
  feedbackMessage.value = ''
  feedbackTone.value = 'default'
}

const queueScrollToBottom = async (): Promise<void> => {
  await nextTick()
  if (!scrollViewport.value) {
    return
  }
  scrollViewport.value.scrollTop = scrollViewport.value.scrollHeight
}

const getActionTypeText = (actionPayload: Record<string, unknown> | null | undefined): string => {
  if (!actionPayload) return '操作'
  const actionType = actionPayload.action_type
  if (actionType === 'confirm_appointment') return '挂号'
  if (actionType === 'create_medicine_order') return '购药'
  return '操作'
}

const mapHistoryMessageToChatMessage = (item: ChatMessageItem): ChatMessage => {
  let content = item.text || ''

  if (item.role === 'action') {
    const actionText = getActionTypeText(item.action_payload)
    const suffix = content.trim() || `${actionText}已执行`
    content = `【${actionText}结果】${suffix}`
  }

  if (!content.trim()) {
    content = item.message_kind === 'cards' ? '（已返回推荐卡片）' : '（无文本内容）'
  }

  return {
    id: createId(`history-${item.seq_no}`),
    role: item.role === 'user' ? 'user' : 'assistant',
    content,
    time: buildTimeLabelFromISO(item.created_at),
    streaming: false
  }
}

const extractCards = (
  cards: Record<string, unknown> | null | undefined
): { doctorCards: DoctorCard[]; medicineCards: MedicineCard[] } => {
  if (!cards) {
    return { doctorCards: [], medicineCards: [] }
  }

  return {
    doctorCards: Array.isArray(cards.doctor_cards) ? (cards.doctor_cards as DoctorCard[]) : [],
    medicineCards: Array.isArray(cards.medicine_cards) ? (cards.medicine_cards as MedicineCard[]) : []
  }
}

const initMedicineSelections = (cards: MedicineCard[]): void => {
  const nextQuantities: Record<number, number> = {}
  const nextSelected: number[] = []

  cards.forEach((card) => {
    nextQuantities[card.medicine_id] = card.default_quantity || 1
    nextSelected.push(card.medicine_id)
  })

  quantities.value = nextQuantities
  selectedMeds.value = nextSelected
}

const updateAssistantMessage = (assistantMessageId: string, updater: (message: ChatMessage) => ChatMessage): void => {
  messages.value = messages.value.map((message) => {
    if (message.id !== assistantMessageId) {
      return message
    }
    return updater(message)
  })
}

const appendActionResult = (label: string, content: string): void => {
  messages.value = [
    ...messages.value,
    {
      id: createId('action'),
      role: 'assistant',
      content: `【${label}结果】${content}`,
      time: buildTimeLabel(),
      streaming: false
    }
  ]
  void queueScrollToBottom()
}

const formatSessionTitle = (session: ChatSessionItem): string =>
  session.title?.trim() || session.public_session_id

const handleSendClick = async (): Promise<void> => {
  const trimmedInput = inputValue.value.trim()
  if (!trimmedInput || isStreaming.value) {
    return
  }

  clearFeedback()

  const assistantMessageId = createId('assistant')
  messages.value = [
    ...messages.value,
    {
      id: createId('user'),
      role: 'user',
      content: trimmedInput,
      time: buildTimeLabel()
    },
    {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      time: buildTimeLabel(),
      streaming: true
    }
  ]
  inputValue.value = ''
  isStreaming.value = true
  doctorCards.value = []
  medicineCards.value = []
  void queueScrollToBottom()

  try {
    await agentAssistantService.streamChat(
      {
        public_session_id: publicSessionId.value,
        message: trimmedInput,
        context: { current_page: '/student/dashboard' }
      },
      {
        onToken: (token) => {
          updateAssistantMessage(assistantMessageId, (message) => ({
            ...message,
            content: `${message.content}${token}`,
            streaming: true
          }))
          void queueScrollToBottom()
        },
        onState: (state: AssistantStatePayload) => {
          publicSessionId.value = state.public_session_id
          doctorCards.value = state.doctor_cards
          medicineCards.value = state.medicine_cards
          initMedicineSelections(state.medicine_cards)
          updateAssistantMessage(assistantMessageId, (message) => ({
            ...message,
            content: message.content || state.answer,
            streaming: true
          }))
          void queueScrollToBottom()
        },
        onDone: () => {
          updateAssistantMessage(assistantMessageId, (message) => ({
            ...message,
            streaming: false
          }))
        },
        onError: (message) => {
          setFeedback(message, 'error')
        }
      }
    )
  } catch (error) {
    setFeedback(error instanceof Error ? error.message : 'AI 对话失败', 'error')
  } finally {
    isStreaming.value = false
    updateAssistantMessage(assistantMessageId, (message) => ({
      ...message,
      streaming: false
    }))
  }
}

const handleDoctorAppointment = async (
  doctor: DoctorCard,
  slot: { date: string; time: string }
): Promise<void> => {
  if (!publicSessionId.value) {
    setFeedback('请先发起一次对话，再使用推荐挂号动作。', 'error')
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

    setFeedback(result.message || '挂号成功', result.success ? 'success' : 'error')
    appendActionResult('挂号', result.message || '挂号成功')
  } catch (error) {
    setFeedback(error instanceof Error ? error.message : '挂号失败', 'error')
  }
}

const toggleMedicineSelection = (medicineId: number, checked: boolean): void => {
  if (checked) {
    selectedMeds.value = [...new Set([...selectedMeds.value, medicineId])]
    return
  }
  selectedMeds.value = selectedMeds.value.filter((item) => item !== medicineId)
}

const handleMedicineSelectionChange = (medicineId: number, event: Event): void => {
  toggleMedicineSelection(medicineId, (event.target as HTMLInputElement).checked)
}

const updateQuantity = (medicineId: number, event: Event): void => {
  const target = event.target as HTMLInputElement
  quantities.value = {
    ...quantities.value,
    [medicineId]: Math.max(1, Number(target.value || 1))
  }
}

const handlePharmacyAction = async (): Promise<void> => {
  if (!publicSessionId.value) {
    setFeedback('请先发起一次对话，再使用推荐购药动作。', 'error')
    return
  }

  const items = selectedMeds.value
    .map((medicineId) => ({
      medicine_id: medicineId,
      quantity: quantities.value[medicineId] || 1
    }))
    .filter((item) => item.quantity > 0)

  if (items.length === 0) {
    setFeedback('请先选择至少一种药品。', 'error')
    return
  }

  try {
    const result = await agentAssistantService.executeAction({
      public_session_id: publicSessionId.value,
      action_type: 'create_medicine_order',
      payload: { items }
    })

    setFeedback(result.message || '购药下单成功', result.success ? 'success' : 'error')
    appendActionResult('购药', result.message || '购药下单成功')
  } catch (error) {
    setFeedback(error instanceof Error ? error.message : '购药失败', 'error')
  }
}

const openHistory = async (): Promise<void> => {
  historyOpen.value = true
  historyLoading.value = true
  clearFeedback()

  try {
    sessions.value = await agentAssistantService.listSessions()
  } catch (error) {
    setFeedback(error instanceof Error ? error.message : '加载历史会话失败', 'error')
  } finally {
    historyLoading.value = false
  }
}

const loadSessionMessages = async (session: ChatSessionItem): Promise<void> => {
  if (isStreaming.value) {
    setFeedback('请等待当前回复完成后再切换会话。', 'error')
    return
  }

  clearFeedback()

  try {
    const items = await agentAssistantService.listMessages(session.public_session_id)
    messages.value = items.map(mapHistoryMessageToChatMessage)
    publicSessionId.value = session.public_session_id

    const latestCardsMessage = [...items].reverse().find((item) => item.role === 'assistant' && item.cards)
    const restoredCards = extractCards(latestCardsMessage?.cards)
    doctorCards.value = restoredCards.doctorCards
    medicineCards.value = restoredCards.medicineCards
    initMedicineSelections(restoredCards.medicineCards)
    historyOpen.value = false
    void queueScrollToBottom()
  } catch (error) {
    setFeedback(error instanceof Error ? error.message : '加载历史消息失败', 'error')
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      void queueScrollToBottom()
    }
  }
)
</script>

<style scoped>
.campus-ai-panel {
  width: 100%;
  min-width: 0;
  opacity: 0;
  pointer-events: none;
  transform: translateX(24px);
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.campus-ai-panel--open {
  opacity: 1;
  pointer-events: auto;
  transform: translateX(0);
}

.campus-ai-panel__frame {
  position: sticky;
  top: 24px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  max-height: calc(100vh - 48px);
  overflow: hidden;
}

.campus-ai-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid rgba(24, 49, 45, 0.08);
}

.campus-ai-panel__header h3 {
  margin: 12px 0 10px;
}

.campus-ai-panel__subtitle {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.campus-ai-panel__header-actions {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.campus-ai-panel__header-button {
  min-height: 42px;
}

.campus-ai-panel__close {
  border: none;
  border-radius: 999px;
  padding: 11px 14px;
  background: rgba(24, 49, 45, 0.08);
  color: #18312d;
  cursor: pointer;
}

.campus-ai-panel__notice {
  margin: 16px 24px 0;
}

.campus-ai-panel__body {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-height: 0;
}

.campus-ai-panel__scroll {
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  display: grid;
  gap: 18px;
}

.campus-ai-panel__system-chip {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(39, 88, 80, 0.1);
  color: #275850;
  font-size: 13px;
}

.campus-ai-panel__empty {
  padding: 18px;
  border-radius: 20px;
  background: rgba(39, 88, 80, 0.06);
  color: #36514c;
}

.campus-ai-panel__empty p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.campus-ai-message {
  display: grid;
  gap: 8px;
}

.campus-ai-message--user {
  justify-items: end;
}

.campus-ai-message__time {
  font-size: 12px;
  color: var(--muted);
}

.campus-ai-message__bubble {
  max-width: 100%;
  padding: 14px 16px;
  border-radius: 18px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.campus-ai-message--user .campus-ai-message__bubble {
  background: linear-gradient(135deg, rgba(24, 49, 45, 0.95), rgba(39, 88, 80, 0.94));
  color: #fff;
  border-top-right-radius: 8px;
}

.campus-ai-message--assistant .campus-ai-message__bubble {
  background: rgba(24, 49, 45, 0.06);
  color: #18312d;
  border: 1px solid rgba(24, 49, 45, 0.08);
  border-top-left-radius: 8px;
}

.campus-ai-message__bubble--streaming {
  position: relative;
}

.campus-ai-message__bubble--streaming::after {
  content: '';
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-left: 8px;
  border-radius: 999px;
  background: var(--accent-warm);
  animation: assistant-pulse 0.9s ease-in-out infinite alternate;
}

.campus-ai-suggestions {
  display: grid;
  gap: 16px;
}

.campus-ai-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(211, 122, 82, 0.07);
  border: 1px solid rgba(211, 122, 82, 0.12);
}

.campus-ai-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.campus-ai-card__header h4 {
  margin: 0 0 8px;
}

.campus-ai-card__header p {
  margin: 0;
  color: #5f6f6c;
  line-height: 1.6;
}

.campus-ai-card__list {
  display: grid;
  gap: 12px;
}

.campus-ai-recommendation {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(24, 49, 45, 0.08);
}

.campus-ai-recommendation__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.campus-ai-recommendation__head p,
.campus-ai-recommendation__reason,
.campus-ai-medicine p {
  margin: 6px 0 0;
  color: #55736d;
  line-height: 1.6;
}

.campus-ai-recommendation__tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(24, 49, 45, 0.08);
  color: #275850;
  font-size: 12px;
  white-space: nowrap;
}

.campus-ai-slot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.campus-ai-slot-button {
  padding: 8px 12px;
  border-radius: 12px;
}

.campus-ai-medicine {
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 12px;
  align-items: start;
}

.campus-ai-medicine input[type='checkbox'] {
  margin-top: 4px;
}

.campus-ai-medicine__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: #456660;
  font-size: 13px;
}

.campus-ai-medicine__actions {
  margin-top: 14px;
}

.campus-ai-medicine__quantity {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #456660;
}

.campus-ai-medicine__quantity input {
  width: 88px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.92);
}

.campus-ai-panel__action-button {
  width: 100%;
  margin-top: 14px;
}

.campus-ai-panel__composer {
  padding: 18px 24px 24px;
  border-top: 1px solid rgba(24, 49, 45, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.44), rgba(255, 255, 255, 0.92));
}

.campus-ai-panel__textarea {
  width: 100%;
  resize: vertical;
  min-height: 96px;
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.84);
}

.campus-ai-panel__composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.campus-ai-panel__hint {
  color: var(--muted);
  font-size: 12px;
}

.campus-ai-history {
  display: grid;
  gap: 12px;
}

.campus-ai-history__item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(24, 49, 45, 0.08);
  border-radius: 18px;
  padding: 16px;
  background: rgba(24, 49, 45, 0.04);
  cursor: pointer;
}

.campus-ai-history__item--active {
  border-color: rgba(39, 88, 80, 0.24);
  background: rgba(39, 88, 80, 0.09);
}

.campus-ai-history__item strong {
  display: block;
  margin-bottom: 8px;
}

.campus-ai-history__meta {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

@keyframes assistant-pulse {
  from {
    opacity: 0.25;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: scale(1.05);
  }
}

@media (max-width: 1180px) {
  .campus-ai-panel__header {
    padding: 20px 20px 16px;
  }

  .campus-ai-panel__scroll,
  .campus-ai-panel__composer {
    padding-left: 20px;
    padding-right: 20px;
  }
}

@media (max-width: 960px) {
  .campus-ai-panel {
    position: fixed;
    inset: 16px;
    z-index: 60;
    transform: translateY(18px);
  }

  .campus-ai-panel__frame {
    top: 0;
    max-height: calc(100vh - 32px);
  }
}

@media (max-width: 640px) {
  .campus-ai-panel__header,
  .campus-ai-panel__composer-footer,
  .campus-ai-card__header,
  .campus-ai-recommendation__head {
    flex-direction: column;
  }

  .campus-ai-panel__header-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
