<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Doctor / Dashboard"
        title="医生接诊台"
        description="真实对接医生端预约、历史病历和诊断提交接口；监控图表保留现有前端的展示型职责。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadData">刷新接诊数据</button>
          <RouterLink class="button" to="/doctor/schedule">查看我的排班</RouterLink>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="stat-grid">
        <article class="glass-card stat-card stat-card--waiting">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--waiting">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">待诊人数</div>
              <div class="stat-card__value">{{ waitingAppointments.length }}</div>
              <div class="stat-card__trend">
                <span class="trend-badge trend-badge--waiting">需处理</span>
              </div>
            </div>
          </div>
          <div ref="waitingChartRef" class="stat-card__chart"></div>
        </article>

        <article class="glass-card stat-card stat-card--completed">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--completed">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">已完成接诊</div>
              <div class="stat-card__value">{{ completedAppointments.length }}</div>
              <div class="stat-card__trend">
                <span class="trend-badge trend-badge--success">+{{ Math.floor(Math.random() * 5 + 2) }} 今日</span>
              </div>
            </div>
          </div>
          <div ref="completedChartRef" class="stat-card__chart"></div>
        </article>

        <article class="glass-card stat-card stat-card--total">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--total">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">今日总量</div>
              <div class="stat-card__value">{{ waitingAppointments.length + completedAppointments.length }}</div>
              <div class="stat-card__trend">
                <span class="trend-badge">接诊中</span>
              </div>
            </div>
          </div>
          <div ref="totalChartRef" class="stat-card__chart stat-card__chart--bar"></div>
        </article>

        <article class="glass-card stat-card" :class="{ 'stat-card--warning': lowStockMedicines.length > 0 }">
          <div class="stat-card__content">
            <div class="stat-card__icon" :class="lowStockMedicines.length > 0 ? 'stat-card__icon--warning' : 'stat-card__icon--safe'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">低库存药品</div>
              <div class="stat-card__value">{{ lowStockMedicines.length }}</div>
              <div class="stat-card__trend">
                <span v-if="lowStockMedicines.length > 0" class="trend-badge trend-badge--warning">需补货</span>
                <span v-else class="trend-badge trend-badge--safe">库存充足</span>
              </div>
            </div>
          </div>
          <div ref="stockChartRef" class="stat-card__chart stat-card__chart--gauge"></div>
        </article>
      </section>

      <section class="split-grid">
        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>实时诊疗监控</h3>
              <p class="dashboard-muted">保留原系统监控看板，图表数据为展示态，业务动作走真实接口。</p>
            </div>
          </div>
          <BaseChart :option="chartOption" />
          <div class="notice">
            低库存预警：
            <span v-if="lowStockMedicines.length">
              {{ lowStockMedicines.map((item) => `${item.name} (${item.stock})`).join('、') }}
            </span>
            <span v-else> 当前没有低库存药品。</span>
          </div>
        </article>

        <article class="glass-card panel stack">
          <div class="toolbar">
            <button
              v-for="tab in doctorTabs"
              :key="tab.key"
              class="button"
              :class="activeTab === tab.key ? '' : 'button--secondary'"
              @click="activeTab = tab.key"
            >
              {{ tab.label }} ({{ tab.count }})
            </button>
          </div>

          <table v-if="currentList.length" class="data-table">
            <thead>
              <tr>
                <th>队列号</th>
                <th>学生</th>
                <th>时段</th>
                <th>主诉</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in currentList" :key="item.appointment_id">
                <td>{{ item.queue_no || '待分配' }}</td>
                <td>{{ item.student_name }}</td>
                <td>{{ item.time_slot }}</td>
                <td>{{ item.symptoms || '-' }}</td>
                <td>
                  <button
                    v-if="activeTab === 'waiting'"
                    class="link-button"
                    @click="openConsultation(item)"
                  >
                    开始接诊
                  </button>
                  <button
                    v-else
                    class="link-button"
                    @click="openConsultation(item, true)"
                  >
                    查看历史
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else-if="loading" class="empty-state">接诊数据加载中...</div>
          <div v-else class="empty-state">当前列表为空。</div>
        </article>
      </section>
    </div>

    <BaseModal
      :open="Boolean(selectedAppointment)"
      :title="historyOnly ? '查看病历' : '接诊处理'"
      :eyebrow="historyOnly ? 'Medical Record' : 'Doctor Consultation'"
      size="lg"
      @close="closeConsultation"
    >
      <div v-if="selectedAppointment" class="consultation-modal">
        <!-- 患者信息头部 -->
        <div class="patient-header">
          <div class="patient-info">
            <div class="patient-avatar">
              {{ selectedAppointment.student_name.charAt(0) }}
            </div>
            <div class="patient-details">
              <h3 class="patient-name">{{ selectedAppointment.student_name }}</h3>
              <div class="patient-meta">
                <span class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                  </svg>
                  {{ selectedAppointment.gender || '未知' }}
                </span>
                <span class="meta-divider">·</span>
                <span class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  {{ selectedAppointment.time_slot }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="selectedAppointment.symptoms" class="symptom-tag">
            <span class="tag-label">主诉</span>
            <span class="tag-value">{{ selectedAppointment.symptoms }}</span>
          </div>
        </div>

        <div class="consultation-body">
          <!-- 左侧：病历历史 -->
          <div class="history-panel">
            <div class="panel-section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
              病历历史
            </div>

            <div v-if="historyLoading" class="panel-loading">
              <div class="loading-spinner"></div>
              加载历史中...
            </div>

            <div v-else-if="patientHistory" class="history-content">
              <!-- 既往病史 -->
              <div class="history-section">
                <h5 class="section-subtitle">既往病史</h5>
                <div v-if="patientHistory.medical_history.length" class="history-tags">
                  <div
                    v-for="item in patientHistory.medical_history"
                    :key="`${item.condition}-${item.date}`"
                    class="history-tag"
                  >
                    <span class="tag-condition">{{ item.condition }}</span>
                    <span class="tag-date">{{ formatDateTime(item.date) }}</span>
                    <span v-if="item.notes" class="tag-notes">{{ item.notes }}</span>
                  </div>
                </div>
                <div v-else class="empty-hint">暂无既往病史</div>
              </div>

              <!-- 就诊记录 -->
              <div class="history-section">
                <h5 class="section-subtitle">近期就诊</h5>
                <div v-if="patientHistory.medical_records.length" class="record-list">
                  <div
                    v-for="record in patientHistory.medical_records"
                    :key="record.id"
                    class="record-item"
                  >
                    <div class="record-header">
                      <span class="record-date">{{ record.visit_date }}</span>
                      <span class="record-fee">{{ formatMoney(record.fee_total) }}</span>
                    </div>
                    <div class="record-diagnosis">
                      {{ record.diagnosis_summary?.conclusion || '无诊断记录' }}
                    </div>
                  </div>
                </div>
                <div v-else class="empty-hint">暂无就诊记录</div>
              </div>
            </div>

            <div v-else class="empty-hint">暂无历史数据</div>
          </div>

          <!-- 右侧：诊断录入 -->
          <div class="diagnosis-panel">
            <div class="panel-section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              {{ historyOnly ? '诊断详情' : '诊断录入' }}
            </div>

            <div v-if="!historyOnly" class="diagnosis-form">
              <div class="form-row">
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">确诊疾病</span>
                    <span class="label-required">*</span>
                  </label>
                  <input
                    v-model.trim="diagnosisForm.category"
                    class="field-input"
                    placeholder="如：上呼吸道感染"
                    :disabled="historyOnly"
                  />
                </div>
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">体征</span>
                  </label>
                  <input
                    v-model.trim="diagnosisForm.signs"
                    class="field-input"
                    placeholder="如：38.5℃，咽部充血"
                    :disabled="historyOnly"
                  />
                </div>
              </div>

              <div class="form-field">
                <label class="field-label">
                  <span class="label-text">诊断结论</span>
                  <span class="label-required">*</span>
                </label>
                <textarea
                  v-model.trim="diagnosisForm.conclusion"
                  class="field-textarea"
                  rows="3"
                  placeholder="详细描述诊断结论..."
                  :disabled="historyOnly"
                />
              </div>

              <div class="form-field">
                <label class="field-label">
                  <span class="label-text">处方药品</span>
                  <span v-if="selectedMedicineIds.length" class="label-badge">{{ selectedMedicineIds.length }}</span>
                </label>
                <div class="medicine-grid">
                  <label
                    v-for="medicine in medicines"
                    :key="medicine.id"
                    class="medicine-card"
                    :class="{
                      'is-selected': selectedMedicineIds.includes(medicine.id),
                      'is-low-stock': medicine.stock <= 10
                    }"
                  >
                    <input
                      type="checkbox"
                      class="medicine-checkbox"
                      :checked="selectedMedicineIds.includes(medicine.id)"
                      :disabled="historyOnly"
                      @change="toggleMedicine(medicine.id)"
                    />
                    <div class="medicine-info">
                      <span class="medicine-name">{{ medicine.name }}</span>
                      <span
                        class="medicine-stock"
                        :class="medicine.stock <= 10 ? 'stock-warning' : 'stock-normal'"
                      >
                        库存 {{ medicine.stock }}
                      </span>
                    </div>
                  </label>
                </div>
              </div>

              <div class="form-actions">
                <button
                  class="btn-submit"
                  :disabled="actionLoading || !diagnosisForm.conclusion.trim()"
                  @click="submitDiagnosis"
                >
                  <svg v-if="actionLoading" class="btn-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/>
                  </svg>
                  <span>{{ actionLoading ? '提交中...' : '提交病历' }}</span>
                </button>
              </div>
            </div>

            <!-- 只读模式显示 -->
            <div v-else class="diagnosis-readonly">
              <div class="readonly-item">
                <span class="readonly-label">确诊疾病</span>
                <span class="readonly-value">{{ diagnosisForm.category || '未填写' }}</span>
              </div>
              <div class="readonly-item">
                <span class="readonly-label">体征</span>
                <span class="readonly-value">{{ diagnosisForm.signs || '未填写' }}</span>
              </div>
              <div class="readonly-item">
                <span class="readonly-label">诊断结论</span>
                <p class="readonly-text">{{ diagnosisForm.conclusion || '未填写' }}</p>
              </div>
              <div v-if="selectedMedicineIds.length" class="readonly-item">
                <span class="readonly-label">处方药品</span>
                <div class="readonly-tags">
                  <span
                    v-for="id in selectedMedicineIds"
                    :key="id"
                    class="readonly-tag"
                  >
                    {{ medicines.find(m => m.id === id)?.name }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption, ECharts } from 'echarts'
import PageHero from '@/components/common/PageHero.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { doctorAppointmentsService } from '@/services/http/doctorAppointments'
import type { DoctorAppointmentHistoryResponse, DoctorAppointmentItem } from '@/types/doctorAppointments'
import { formatDateTime, formatMoney } from '@/utils/format'

type DoctorQueueStatus = 'confirmed' | 'in_progress' | 'completed'
type DoctorTabKey = 'waiting' | 'finished'

interface DoctorQueueItem extends DoctorAppointmentItem {
  workflow_status: DoctorQueueStatus
}

interface MedicineOption {
  id: number
  name: string
  stock: number
}

const loading = ref(false)
const actionLoading = ref(false)
const historyLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref<DoctorTabKey>('waiting')
const waitingAppointments = ref<DoctorQueueItem[]>([])
const completedAppointments = ref<DoctorQueueItem[]>([])
const medicines = ref<MedicineOption[]>([])
const selectedAppointment = ref<DoctorQueueItem | null>(null)
const patientHistory = ref<DoctorAppointmentHistoryResponse | null>(null)
const historyOnly = ref(false)
const selectedMedicineIds = ref<number[]>([])

// 卡片图表 refs
const waitingChartRef = ref<HTMLDivElement | null>(null)
const completedChartRef = ref<HTMLDivElement | null>(null)
const totalChartRef = ref<HTMLDivElement | null>(null)
const stockChartRef = ref<HTMLDivElement | null>(null)
let waitingChart: ECharts | null = null
let completedChart: ECharts | null = null
let totalChart: ECharts | null = null
let stockChart: ECharts | null = null

const diagnosisForm = reactive({
  category: '',
  signs: '',
  conclusion: ''
})

const lowStockMedicines = computed(() => medicines.value.filter((item) => item.stock <= 10).slice(0, 5))
const currentList = computed(() => activeTab.value === 'waiting' ? waitingAppointments.value : completedAppointments.value)
const doctorTabs = computed<Array<{ key: DoctorTabKey; label: string; count: number }>>(() => [
  { key: 'waiting', label: '待诊', count: waitingAppointments.value.length },
  { key: 'finished', label: '已诊', count: completedAppointments.value.length }
])

const chartOption: EChartsOption = {
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      data: [12, 18, 25, 22, 20, 14, 10],
      lineStyle: { color: '#275850' },
      areaStyle: { color: 'rgba(39, 88, 80, 0.12)' },
      itemStyle: { color: '#d37a52' }
    }
  ]
}

// 待诊人数 - 环形图
const getWaitingChartOption = (value: number): EChartsOption => ({
  series: [
    {
      type: 'pie',
      radius: ['60%', '75%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { scale: false },
      data: [
        { value, itemStyle: { color: '#3b82f6' } },
        { value: Math.max(20 - value, 0), itemStyle: { color: '#e5e7eb' } }
      ]
    }
  ]
})

// 已完成 - 趋势折线图
const getCompletedChartOption = (value: number): EChartsOption => ({
  grid: { left: 0, right: 0, top: 5, bottom: 5 },
  xAxis: { type: 'category', show: false, data: ['1', '2', '3', '4', '5', '6', '7'] },
  yAxis: { type: 'value', show: false },
  series: [
    {
      type: 'line',
      smooth: true,
      symbol: 'none',
      data: [value * 0.5, value * 0.7, value * 0.6, value * 0.8, value * 0.9, value],
      lineStyle: { color: '#10b981', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
          { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
        ])
      }
    }
  ]
})

// 今日总量 - 柱状图
const getTotalChartOption = (waiting: number, completed: number): EChartsOption => ({
  grid: { left: 0, right: 0, top: 5, bottom: 5 },
  xAxis: { type: 'category', show: false, data: ['待诊', '已完成'] },
  yAxis: { type: 'value', show: false },
  series: [
    {
      type: 'bar',
      barWidth: '50%',
      data: [
        { value: waiting, itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] } },
        { value: completed, itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] } }
      ]
    }
  ]
})

// 低库存 - 仪表盘
const getStockChartOption = (value: number): EChartsOption => ({
  series: [
    {
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 10,
      radius: '90%',
      center: ['50%', '70%'],
      splitNumber: 5,
      itemStyle: { color: value > 0 ? '#ef4444' : '#10b981' },
      progress: { show: true, width: 8 },
      pointer: { show: false },
      axisLine: { lineStyle: { width: 8, color: [[1, '#e5e7eb']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      detail: { show: false },
      data: [{ value }]
    }
  ]
})

const renderCardCharts = () => {
  if (waitingChartRef.value) {
    waitingChart ??= echarts.init(waitingChartRef.value)
    waitingChart.setOption(getWaitingChartOption(waitingAppointments.value.length))
  }
  if (completedChartRef.value) {
    completedChart ??= echarts.init(completedChartRef.value)
    completedChart.setOption(getCompletedChartOption(completedAppointments.value.length))
  }
  if (totalChartRef.value) {
    totalChart ??= echarts.init(totalChartRef.value)
    totalChart.setOption(getTotalChartOption(waitingAppointments.value.length, completedAppointments.value.length))
  }
  if (stockChartRef.value) {
    stockChart ??= echarts.init(stockChartRef.value)
    stockChart.setOption(getStockChartOption(lowStockMedicines.value.length))
  }
}

const handleResize = () => {
  waitingChart?.resize()
  completedChart?.resize()
  totalChart?.resize()
  stockChart?.resize()
}

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadData = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const [confirmed, inProgress, completed, medicinePayload] = await Promise.all([
      doctorAppointmentsService.list({ status: 'confirmed' }),
      doctorAppointmentsService.list({ status: 'in_progress' }),
      doctorAppointmentsService.list({ status: 'completed' }),
      doctorAppointmentsService.medicines()
    ])

    waitingAppointments.value = [
      ...confirmed.items.map((item) => ({ ...item, workflow_status: 'confirmed' as const })),
      ...inProgress.items.map((item) => ({ ...item, workflow_status: 'in_progress' as const }))
    ]
    completedAppointments.value = completed.items.map((item) => ({ ...item, workflow_status: 'completed' as const }))
    medicines.value = medicinePayload.items.map((item) => ({
      id: item.id,
      name: item.name,
      stock: item.stock
    }))

    // 渲染卡片图表
    renderCardCharts()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取医生端数据失败'
  } finally {
    loading.value = false
  }
}

const loadHistory = async (appointmentId: number): Promise<void> => {
  historyLoading.value = true
  try {
    patientHistory.value = await doctorAppointmentsService.history(appointmentId, {
      page: 1,
      page_size: 6
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取学生历史失败'
    patientHistory.value = null
  } finally {
    historyLoading.value = false
  }
}

const openConsultation = async (item: DoctorQueueItem, readonly = false): Promise<void> => {
  resetMessages()
  selectedAppointment.value = item
  historyOnly.value = readonly
  selectedMedicineIds.value = []
  diagnosisForm.category = ''
  diagnosisForm.signs = ''
  diagnosisForm.conclusion = ''

  if (!readonly && item.workflow_status === 'confirmed') {
    try {
      await doctorAppointmentsService.start(item.appointment_id)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '开始接诊失败'
      selectedAppointment.value = null
      return
    }
  }

  await loadHistory(item.appointment_id)
  if (!readonly) {
    successMessage.value = item.workflow_status === 'confirmed'
      ? '已进入接诊状态，可填写病历。'
      : '已恢复正在进行中的接诊记录。'
    await loadData()
  }
}

const toggleMedicine = (medicineId: number): void => {
  if (selectedMedicineIds.value.includes(medicineId)) {
    selectedMedicineIds.value = selectedMedicineIds.value.filter((value) => value !== medicineId)
  } else {
    selectedMedicineIds.value = [...selectedMedicineIds.value, medicineId]
  }
}

const submitDiagnosis = async (): Promise<void> => {
  if (!selectedAppointment.value) return
  if (!diagnosisForm.conclusion.trim()) {
    errorMessage.value = '请填写诊断结论'
    return
  }

  actionLoading.value = true
  resetMessages()
  try {
    await doctorAppointmentsService.submitDiagnosis(selectedAppointment.value.appointment_id, {
      category: diagnosisForm.category || null,
      signs: diagnosisForm.signs || null,
      conclusion: diagnosisForm.conclusion.trim(),
      items: selectedMedicineIds.value.map((medicineId) => ({
        medicine_id: medicineId,
        quantity: 1
      }))
    })
    successMessage.value = '病历已提交'
    closeConsultation()
    await loadData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交病历失败'
  } finally {
    actionLoading.value = false
  }
}

const closeConsultation = (): void => {
  selectedAppointment.value = null
  patientHistory.value = null
  historyOnly.value = false
}

onMounted(() => {
  void loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  waitingChart?.dispose()
  completedChart?.dispose()
  totalChart?.dispose()
  stockChart?.dispose()
})

// 监听数据变化更新图表
watch([() => waitingAppointments.value.length, () => completedAppointments.value.length, () => lowStockMedicines.value.length], () => {
  renderCardCharts()
}, { immediate: true })
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

.history-list {
  margin: 0;
  padding-left: 20px;
  color: var(--muted);
  line-height: 1.8;
}

.medicine-selector {
  display: grid;
  gap: 10px;
  max-height: 220px;
  overflow: auto;
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 16px;
}

.medicine-selector__item {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* Stat Cards - 新版统计卡片样式 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  min-height: 120px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-card__content {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon svg {
  width: 24px;
  height: 24px;
}

.stat-card__icon--waiting {
  background: rgba(59, 130, 246, 0.12);
  color: #3b82f6;
}

.stat-card__icon--completed {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.stat-card__icon--total {
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

.stat-card__icon--warning {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.stat-card__icon--safe {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.stat-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-card__label {
  font-size: 13px;
  color: var(--muted);
  font-weight: 500;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-card__trend {
  margin-top: 4px;
}

.trend-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.trend-badge--waiting {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.trend-badge--success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.trend-badge--warning {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.trend-badge--safe {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.stat-card__chart {
  width: 80px;
  height: 60px;
  flex-shrink: 0;
}

.stat-card__chart--bar {
  width: 60px;
  height: 50px;
}

.stat-card__chart--gauge {
  width: 70px;
  height: 50px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .stat-card {
    min-height: 100px;
  }
}

/* ==================== 接诊处理弹窗样式 ==================== */
.consultation-modal {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 患者信息头部 */
.patient-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.patient-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.patient-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
}

.patient-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-divider {
  color: #cbd5e1;
}

.symptom-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: white;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
}

.tag-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.tag-value {
  font-size: 14px;
  color: #dc2626;
  font-weight: 600;
}

/* 主体内容区 */
.consultation-body {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 20px;
  min-height: 400px;
}

.history-panel,
.diagnosis-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.panel-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-section-title svg {
  color: #3b82f6;
}

/* 加载状态 */
.panel-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #64748b;
  font-size: 14px;
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

/* 历史内容 */
.history-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  max-height: 380px;
}

.history-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-subtitle {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 病史标签 */
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  border-left: 3px solid #f59e0b;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.tag-condition {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.tag-date {
  font-size: 11px;
  color: #64748b;
}

.tag-notes {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

/* 就诊记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.record-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.record-date {
  font-size: 12px;
  color: #64748b;
}

.record-fee {
  font-size: 13px;
  font-weight: 600;
  color: #059669;
}

.record-diagnosis {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
}

.empty-hint {
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  background: white;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
}

/* 诊断表单 */
.diagnosis-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.label-required {
  color: #dc2626;
}

.label-badge {
  margin-left: auto;
  padding: 2px 8px;
  background: #3b82f6;
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
}

.field-input,
.field-textarea {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
}

.field-input:focus,
.field-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.field-input:disabled,
.field-textarea:disabled {
  background: #f3f4f6;
  color: #6b7280;
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

/* 药品选择网格 */
.medicine-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 4px;
}

.medicine-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.medicine-card:hover {
  border-color: #3b82f6;
}

.medicine-card.is-selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.medicine-card.is-low-stock {
  border-left: 3px solid #ef4444;
}

.medicine-checkbox {
  position: absolute;
  opacity: 0;
}

.medicine-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.medicine-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

.medicine-stock {
  font-size: 11px;
}

.stock-normal {
  color: #10b981;
}

.stock-warning {
  color: #ef4444;
  font-weight: 600;
}

/* 提交按钮 */
.form-actions {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.btn-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

/* 只读模式 */
.diagnosis-readonly {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.readonly-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.readonly-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.readonly-value {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.readonly-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

.readonly-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.readonly-tag {
  padding: 4px 10px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 500;
  border-radius: 999px;
}

/* 弹窗响应式 */
@media (max-width: 900px) {
  .consultation-body {
    grid-template-columns: 1fr;
  }

  .patient-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .medicine-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
