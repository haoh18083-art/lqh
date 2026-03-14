<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Student / Appointments"
        title="预约管理"
        description="真实对接学生预约接口，支持查看详情、文档下载、取消和改期。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadAppointments">刷新列表</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="dashboard__stats">
        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--primary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">总预约数</span>
            <strong class="stat-card__value">{{ appointments.length }}</strong>
            <span class="stat-card__trend">累计所有预约</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--info">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">即将就诊</span>
            <strong class="stat-card__value">{{ grouped.upcoming.length }}</strong>
            <span class="stat-card__trend" :class="{ 'stat-card__trend--up': grouped.upcoming.length > 0 }">
              待处理预约
            </span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--success">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">已完成</span>
            <strong class="stat-card__value">{{ grouped.completed.length }}</strong>
            <span class="stat-card__trend">成功就诊</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--warning">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">已取消/改期</span>
            <strong class="stat-card__value">{{ grouped.cancelled.length }}</strong>
            <span class="stat-card__trend">变动记录</span>
          </div>
        </article>
      </section>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="button"
            :class="activeTab === tab.key ? '' : 'button--secondary'"
            @click="activeTab = tab.key"
          >
            {{ tab.label }} ({{ tab.count }})
          </button>
        </div>

        <table v-if="currentAppointments.length" class="data-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>科室</th>
              <th>医生</th>
              <th>主诉</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in currentAppointments" :key="item.id">
              <td>{{ item.date }} {{ item.time }}</td>
              <td>{{ item.department.name }}</td>
              <td>{{ item.doctor.full_name }}</td>
              <td>{{ item.symptoms || '-' }}</td>
              <td><StatusBadge :label="statusLabel(item.status)" :tone="statusTone(item.status)" /></td>
              <td>
                <div class="action-stack">
                  <button class="link-button" @click="openDetails(item)">查看详情</button>
                  <button
                    v-if="canReschedule(item.status)"
                    class="link-button"
                    @click="openReschedule(item)"
                  >
                    改期
                  </button>
                  <button
                    v-if="canCancel(item.status)"
                    class="link-button"
                    @click="openCancel(item)"
                  >
                    取消
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">预约数据加载中...</div>
        <div v-else class="empty-state">当前分组下暂无预约。</div>
      </section>
    </div>

    <BaseModal :open="Boolean(selectedAppointment && detailsOpen)" title="预约详情" eyebrow="Appointment Detail" size="lg" @close="closeModals">
      <div v-if="selectedAppointment" class="appointment-detail">
        <!-- 头部信息卡片 -->
        <div class="detail-header">
          <div class="header-card">
            <div class="header-icon" :class="`status-${selectedAppointment.status}`">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
            </div>
            <div class="header-content">
              <span class="header-label">就诊时间</span>
              <span class="header-value">{{ selectedAppointment.date }} {{ selectedAppointment.time }}</span>
            </div>
          </div>

          <div class="header-card">
            <div class="header-icon icon-doctor">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <div class="header-content">
              <span class="header-label">科室/医生</span>
              <span class="header-value">{{ selectedAppointment.department.name }} · {{ selectedAppointment.doctor.full_name }}</span>
            </div>
          </div>

          <div class="header-card">
            <div class="header-icon" :class="`status-${selectedAppointment.status}`">
              <svg v-if="selectedAppointment.status === 'completed'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <svg v-else-if="selectedAppointment.status === 'cancelled' || selectedAppointment.status === 'missed'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="header-content">
              <span class="header-label">当前状态</span>
              <span class="header-value status-badge" :class="`status-${selectedAppointment.status}`">
                {{ statusLabel(selectedAppointment.status) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 主诉 -->
        <div class="symptom-section">
          <div class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            主诉症状
          </div>
          <div class="symptom-content">
            {{ selectedAppointment.symptoms || '未填写主诉' }}
          </div>
        </div>

        <!-- 诊疗信息 -->
        <div class="medical-section">
          <div class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
            </svg>
            诊疗信息
          </div>

          <div v-if="medicalRecordLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>诊疗信息加载中...</span>
          </div>

          <div v-else-if="medicalRecordError" class="error-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ medicalRecordError }}
          </div>

          <div v-else-if="selectedMedicalRecord" class="medical-content">
            <!-- 诊断信息网格 -->
            <div class="diagnosis-grid">
              <div class="diagnosis-card">
                <div class="card-label">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <path d="M9 11l3 3L22 4"/>
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
                  </svg>
                  疾病分类
                </div>
                <div class="card-value">{{ selectedMedicalRecord.diagnosis_summary?.category || '未记录' }}</div>
              </div>

              <div class="diagnosis-card">
                <div class="card-label">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                  </svg>
                  体征
                </div>
                <div class="card-value">{{ selectedMedicalRecord.diagnosis_summary?.signs || '未记录' }}</div>
              </div>
            </div>

            <div class="diagnosis-fullwidth">
              <div class="diagnosis-card">
                <div class="card-label">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                  </svg>
                  诊断结果
                </div>
                <div class="card-value">{{ selectedMedicalRecord.diagnosis_summary?.conclusion || '未记录' }}</div>
              </div>

              <div class="diagnosis-card">
                <div class="card-label">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    <path d="M12 8v4"/>
                    <path d="M12 16h.01"/>
                  </svg>
                  医嘱建议
                </div>
                <div class="card-value advice">{{ selectedMedicalRecord.diagnosis_summary?.advice || '未记录' }}</div>
              </div>
            </div>

            <!-- 处方药品 -->
            <div class="prescription-section">
              <div class="subsection-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
                </svg>
                处方药品
              </div>

              <div v-if="selectedMedicalRecord.prescription_summary.length" class="medicine-list">
                <div
                  v-for="item in selectedMedicalRecord.prescription_summary"
                  :key="`${item.name}-${item.dosage || '-'}-${item.quantity}`"
                  class="medicine-item"
                >
                  <div class="medicine-info">
                    <div class="medicine-name">{{ item.name }}</div>
                    <div class="medicine-dosage">{{ item.dosage || '遵医嘱' }}</div>
                  </div>
                  <div class="medicine-quantity">
                    <span class="quantity-value">{{ item.quantity }}{{ item.unit || '' }}</span>
                    <span class="quantity-price">{{ formatMoney(item.total_price) }}</span>
                  </div>
                </div>
              </div>

              <div v-else class="empty-hint">当前预约未开具药品</div>
            </div>
          </div>

          <div v-else class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="48" height="48">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            <p>当前预约暂无诊断结果</p>
            <span>医生完成接诊后将在此显示诊疗信息</span>
          </div>
        </div>

        <!-- 文档列表 -->
        <div class="documents-section">
          <div class="section-header">
            <div class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              文档列表
            </div>
            <button class="btn-refresh" @click="loadDocuments">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <polyline points="23 4 23 10 17 10"/>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
              </svg>
              刷新
            </button>
          </div>

          <div v-if="documentsLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>文档加载中...</span>
          </div>

          <div v-else-if="documents.length" class="documents-list">
            <div
              v-for="document in documents"
              :key="document.id"
              class="document-item"
            >
              <div class="document-icon" :class="document.doc_type">
                <svg v-if="document.doc_type === 'diagnosis'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
                  <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
                </svg>
              </div>
              <div class="document-info">
                <div class="document-type">{{ document.doc_type === 'diagnosis' ? '诊断单' : '处方单' }}</div>
                <div class="document-name">{{ document.file_name }}</div>
                <div class="document-time">{{ formatDateTime(document.created_at) }}</div>
              </div>
              <button class="btn-download" @click="downloadDocument(document.doc_type, document.file_name)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                下载
              </button>
            </div>
          </div>

          <div v-else class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="48" height="48">
              <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
              <polyline points="13 2 13 9 20 9"/>
            </svg>
            <p>当前预约还没有生成文档</p>
            <span>诊断完成后可在此下载相关文档</span>
          </div>
        </div>
      </div>
    </BaseModal>

    <BaseModal :open="Boolean(selectedAppointment && cancelOpen)" title="取消预约" eyebrow="Cancel Appointment" size="sm" @close="closeModals">
      <div class="stack">
        <div class="field">
          <label>取消原因</label>
          <textarea v-model.trim="cancelReason" rows="4" placeholder="请输入取消原因"></textarea>
        </div>
        <button class="button button--danger" :disabled="actionLoading" @click="submitCancel">
          {{ actionLoading ? '提交中...' : '确认取消' }}
        </button>
      </div>
    </BaseModal>

    <BaseModal :open="Boolean(selectedAppointment && rescheduleOpen)" title="改期预约" eyebrow="Reschedule Appointment" size="sm" @close="closeModals">
      <div class="stack">
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>新的日期</label>
            <input v-model="rescheduleForm.new_date" type="date" />
          </div>
          <div class="field">
            <label>新的时间</label>
            <input v-model="rescheduleForm.new_time" type="time" />
          </div>
        </div>
        <div class="field">
          <label>改期原因</label>
          <textarea v-model.trim="rescheduleForm.reason" rows="4" placeholder="请输入改期原因"></textarea>
        </div>
        <button class="button" :disabled="actionLoading" @click="submitReschedule">
          {{ actionLoading ? '提交中...' : '确认改期' }}
        </button>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { appointmentsService } from '@/services/http/appointments'
import { medicalRecordsService } from '@/services/http/medicalRecords'
import type { AppointmentDocumentInfo, AppointmentResponse } from '@/types/appointment'
import type { MedicalRecordItem } from '@/types/medicalRecord'
import { formatDateTime, formatMoney } from '@/utils/format'
import { saveBlob } from '@/utils/download'

type AppointmentTabKey = 'upcoming' | 'completed' | 'cancelled'

const appointments = ref<AppointmentResponse[]>([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref<AppointmentTabKey>('upcoming')
const selectedAppointment = ref<AppointmentResponse | null>(null)
const selectedMedicalRecord = ref<MedicalRecordItem | null>(null)
const documents = ref<AppointmentDocumentInfo[]>([])
const documentsLoading = ref(false)
const medicalRecordLoading = ref(false)
const medicalRecordError = ref('')
const detailsOpen = ref(false)
const cancelOpen = ref(false)
const rescheduleOpen = ref(false)
const actionLoading = ref(false)
const cancelReason = ref('')
const rescheduleForm = reactive({
  new_date: '',
  new_time: '',
  reason: ''
})

const grouped = computed(() => ({
  upcoming: appointments.value.filter((item) => ['pending', 'confirmed', 'in_progress'].includes(item.status)),
  completed: appointments.value.filter((item) => item.status === 'completed'),
  cancelled: appointments.value.filter((item) => ['cancelled', 'rescheduled', 'missed'].includes(item.status))
}))

const currentAppointments = computed(() => grouped.value[activeTab.value])

const tabs = computed<Array<{ key: AppointmentTabKey; label: string; count: number }>>(() => [
  { key: 'upcoming', label: '即将就诊', count: grouped.value.upcoming.length },
  { key: 'completed', label: '历史记录', count: grouped.value.completed.length },
  { key: 'cancelled', label: '已取消/改期', count: grouped.value.cancelled.length }
])

const statusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    pending: '待确认',
    confirmed: '已确认',
    in_progress: '就诊中',
    completed: '已完成',
    cancelled: '已取消',
    missed: '已失约',
    rescheduled: '已改期'
  }
  return labels[status] || status
}

const statusTone = (status: string): 'default' | 'success' | 'warning' | 'danger' | 'info' => {
  if (status === 'completed') return 'success'
  if (status === 'confirmed' || status === 'in_progress') return 'info'
  if (status === 'pending' || status === 'rescheduled') return 'warning'
  if (status === 'cancelled' || status === 'missed') return 'danger'
  return 'default'
}

const canCancel = (status: string): boolean => ['pending', 'confirmed'].includes(status)
const canReschedule = (status: string): boolean => ['pending', 'confirmed'].includes(status)

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadAppointments = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const response = await appointmentsService.mine({ page: 1, page_size: 200 })
    appointments.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取预约列表失败'
  } finally {
    loading.value = false
  }
}

const loadDocuments = async (): Promise<void> => {
  if (!selectedAppointment.value) return
  documentsLoading.value = true
  try {
    const response = await appointmentsService.documents(selectedAppointment.value.id)
    documents.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取文档失败'
  } finally {
    documentsLoading.value = false
  }
}

const loadMedicalRecord = async (): Promise<void> => {
  if (!selectedAppointment.value) return
  medicalRecordLoading.value = true
  medicalRecordError.value = ''
  selectedMedicalRecord.value = null

  try {
    const response = await medicalRecordsService.mine({ page: 1, page_size: 200 })
    selectedMedicalRecord.value = response.items.find((item) => item.appointment_id === selectedAppointment.value?.id) || null
  } catch (error) {
    medicalRecordError.value = error instanceof Error ? error.message : '诊疗信息加载失败'
  } finally {
    medicalRecordLoading.value = false
  }
}

const openDetails = async (item: AppointmentResponse): Promise<void> => {
  selectedAppointment.value = item
  detailsOpen.value = true
  documents.value = []
  selectedMedicalRecord.value = null
  medicalRecordError.value = ''
  await Promise.all([loadDocuments(), loadMedicalRecord()])
}

const openCancel = (item: AppointmentResponse): void => {
  selectedAppointment.value = item
  cancelReason.value = ''
  cancelOpen.value = true
}

const openReschedule = (item: AppointmentResponse): void => {
  selectedAppointment.value = item
  rescheduleForm.new_date = item.date
  rescheduleForm.new_time = item.time
  rescheduleForm.reason = ''
  rescheduleOpen.value = true
}

const closeModals = (): void => {
  detailsOpen.value = false
  cancelOpen.value = false
  rescheduleOpen.value = false
  documents.value = []
  selectedMedicalRecord.value = null
  medicalRecordError.value = ''
}

const submitCancel = async (): Promise<void> => {
  if (!selectedAppointment.value || !cancelReason.value.trim()) {
    errorMessage.value = '请输入取消原因'
    return
  }

  actionLoading.value = true
  try {
    await appointmentsService.cancel(selectedAppointment.value.id, cancelReason.value.trim())
    successMessage.value = '预约已取消'
    closeModals()
    await loadAppointments()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '取消预约失败'
  } finally {
    actionLoading.value = false
  }
}

const submitReschedule = async (): Promise<void> => {
  if (!selectedAppointment.value || !rescheduleForm.new_date || !rescheduleForm.new_time || !rescheduleForm.reason.trim()) {
    errorMessage.value = '请完整填写改期信息'
    return
  }

  actionLoading.value = true
  try {
    await appointmentsService.reschedule(selectedAppointment.value.id, {
      new_date: rescheduleForm.new_date,
      new_time: rescheduleForm.new_time,
      reason: rescheduleForm.reason.trim()
    })
    successMessage.value = '预约已改期'
    closeModals()
    await loadAppointments()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '改期失败'
  } finally {
    actionLoading.value = false
  }
}

const downloadDocument = async (docType: 'diagnosis' | 'prescription', fileName: string): Promise<void> => {
  if (!selectedAppointment.value) return
  const blob = await appointmentsService.download(selectedAppointment.value.id, docType)
  saveBlob(blob, fileName)
}

onMounted(() => {
  void loadAppointments()
})
</script>

<style scoped>
/* Stats Grid - Matching Dashboard Design */
.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-base);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon svg {
  width: 24px;
  height: 24px;
}

.stat-card__icon--primary {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.stat-card__icon--success {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.stat-card__icon--warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.stat-card__icon--info {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.stat-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-card__label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-card__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-card__trend {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stat-card__trend--up {
  color: var(--color-success);
}

/* Responsive */
@media (max-width: 1200px) {
  .dashboard__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .dashboard__stats {
    grid-template-columns: 1fr;
  }
}

.action-stack {
  display: grid;
  gap: 8px;
}

/* ==================== 预约详情弹窗样式 ==================== */
.appointment-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 头部信息卡片 */
.detail-header {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.header-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.header-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.header-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-icon svg {
  width: 22px;
  height: 22px;
}

.header-icon.status-pending,
.header-icon.status-confirmed {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.header-icon.status-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.header-icon.status-cancelled,
.header-icon.status-missed {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.header-icon.icon-doctor {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.header-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-pending,
.status-badge.status-confirmed {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.status-badge.status-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-badge.status-cancelled,
.status-badge.status-missed {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

/* 章节标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.section-title svg {
  color: #3b82f6;
}

.subsection-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.subsection-title svg {
  color: #ec4899;
}

/* 主诉 */
.symptom-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.symptom-content {
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  border-left: 3px solid #3b82f6;
}

/* 诊疗信息 */
.medical-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
  color: #64748b;
}

.loading-state svg,
.error-state svg,
.empty-state svg {
  color: #94a3b8;
}

.error-state {
  color: #ef4444;
}

.error-state svg {
  color: #ef4444;
}

.empty-state p {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}

.empty-state span {
  font-size: 13px;
  color: #94a3b8;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 诊断信息网格 */
.medical-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diagnosis-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.diagnosis-fullwidth {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diagnosis-card {
  background: white;
  border-radius: 10px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.diagnosis-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

.card-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.card-label svg {
  color: #3b82f6;
}

.card-value {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
  line-height: 1.5;
}

.card-value.advice {
  color: #059669;
  font-style: italic;
}

/* 处方药品 */
.prescription-section {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.medicine-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.medicine-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: white;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.medicine-item:hover {
  border-color: #ec4899;
  box-shadow: 0 2px 8px rgba(236, 72, 153, 0.08);
}

.medicine-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.medicine-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.medicine-dosage {
  font-size: 12px;
  color: #6b7280;
}

.medicine-quantity {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.quantity-value {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.quantity-price {
  font-size: 13px;
  font-weight: 600;
  color: #059669;
}

.empty-hint {
  padding: 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  background: white;
  border-radius: 8px;
  border: 1px dashed #d1d5db;
}

/* 文档列表 */
.documents-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.documents-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: white;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.document-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

.document-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.document-icon.diagnosis {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.document-icon.prescription {
  background: rgba(236, 72, 153, 0.1);
  color: #ec4899;
}

.document-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.document-type {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.document-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.document-time {
  font-size: 12px;
  color: #9ca3af;
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-download:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 响应式 */
@media (max-width: 900px) {
  .detail-header {
    grid-template-columns: 1fr;
  }

  .diagnosis-grid {
    grid-template-columns: 1fr;
  }

  .medicine-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .medicine-quantity {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }
}
</style>
