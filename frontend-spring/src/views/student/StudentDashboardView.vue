<template>
  <div class="dashboard">
    <PageHero
      eyebrow="学生门户 / 仪表盘"
      title="健康总览"
      description="查看你的健康指标、预约状态和购药记录"
    >
      <template #actions>
        <RouterLink class="btn btn-secondary" to="/student/consultation">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
          预约挂号
        </RouterLink>
        <RouterLink class="btn btn-primary" to="/student/pharmacy">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
          </svg>
          自助药房
        </RouterLink>
      </template>
    </PageHero>

    <!-- Stats Grid -->
    <section class="dashboard__stats">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--primary">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
          </svg>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">BMI指数</span>
          <strong class="stat-card__value">{{ bmiValue }}</strong>
          <StatusBadge :label="bmiLabel" :tone="bmiTone" />
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">即将就诊</span>
          <strong class="stat-card__value">{{ upcomingAppointments.length }}</strong>
          <span class="stat-card__trend" :class="{ 'stat-card__trend--up': upcomingAppointments.length > 0 }">
            未来7天内
          </span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
          </svg>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">购药订单</span>
          <strong class="stat-card__value">{{ recentOrders.length }}</strong>
          <span class="stat-card__trend">最近30天</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">身高/体重</span>
          <strong class="stat-card__value">{{ form.height }}/{{ form.weight }}</strong>
          <span class="stat-card__trend">cm/kg</span>
        </div>
      </div>
    </section>

    <!-- Charts & Lists -->
    <section class="dashboard__content">
      <!-- Left Column -->
      <div class="dashboard__column">
        <!-- Health Chart -->
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3>健康趋势</h3>
            <StatusBadge :label="seasonalWarning.title" tone="warning" />
          </div>
          <p class="dashboard__card-desc">{{ seasonalWarning.description }}</p>
          <BaseChart :option="healthChartOption" style="height: 280px;" />
        </div>

        <!-- BMI Calculator -->
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3>身体指标录入</h3>
          </div>
          <div class="dashboard__form-grid">
            <div class="dashboard__field">
              <label>身高 (cm)</label>
              <input
                v-model.number="form.height"
                type="number"
                min="100"
                max="250"
                placeholder="175"
              >
            </div>
            <div class="dashboard__field">
              <label>体重 (kg)</label>
              <input
                v-model.number="form.weight"
                type="number"
                min="30"
                max="200"
                placeholder="65"
              >
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="dashboard__column">
        <!-- Upcoming Appointments -->
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3>即将就诊</h3>
            <RouterLink class="link-button" to="/student/appointments">查看全部</RouterLink>
          </div>
          <div v-if="appointmentsLoading" class="dashboard__loading">
            <div class="dashboard__spinner" />
            加载中...
          </div>
          <div v-else-if="upcomingAppointments.length === 0" class="dashboard__empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
            <p>暂无即将就诊的预约</p>
          </div>
          <div v-else class="dashboard__list">
            <div
              v-for="item in upcomingAppointments"
              :key="item.id"
              class="dashboard__list-item"
            >
              <div class="dashboard__list-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
              </div>
              <div class="dashboard__list-content">
                <strong>{{ item.department.name }}</strong>
                <span>{{ item.doctor.full_name }}</span>
              </div>
              <div class="dashboard__list-meta">
                <span class="dashboard__list-date">{{ formatDate(item.date) }}</span>
                <StatusBadge :label="appointmentStatusLabel(item.status)" :tone="appointmentStatusTone(item.status)" />
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Orders -->
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3>最近购药</h3>
            <RouterLink class="link-button" to="/student/medicine-records">查看全部</RouterLink>
          </div>
          <div v-if="ordersLoading" class="dashboard__loading">
            <div class="dashboard__spinner" />
            加载中...
          </div>
          <div v-else-if="recentOrders.length === 0" class="dashboard__empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
            </svg>
            <p>暂无购药记录</p>
          </div>
          <div v-else class="dashboard__list">
            <div
              v-for="order in recentOrders"
              :key="order.id"
              class="dashboard__list-item"
            >
              <div class="dashboard__list-icon dashboard__list-icon--warning">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>
              <div class="dashboard__list-content">
                <strong>{{ order.order_no }}</strong>
                <span>{{ formatDateTime(order.purchase_date) }}</span>
              </div>
              <div class="dashboard__list-meta">
                <span class="dashboard__list-price">¥{{ order.total_amount.toFixed(2) }}</span>
                <StatusBadge :label="orderStatusLabel(order.status)" :tone="orderStatusTone(order.status)" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import { appointmentsService } from '@/services/http/appointments'
import { pharmacyService, type PharmacyOrder } from '@/services/agent/pharmacy'
import type { AppointmentResponse } from '@/types/appointment'
import { formatDateTime } from '@/utils/format'

const STORAGE_KEY = 'student_dashboard_health_form'

const form = reactive({
  height: 175,
  weight: 65
})

const appointmentsLoading = ref(false)
const ordersLoading = ref(false)
const upcomingAppointments = ref<AppointmentResponse[]>([])
const recentOrders = ref<PharmacyOrder[]>([])

// BMI Calculations
const bmiValue = computed(() => {
  const heightInMeter = form.height / 100
  return Number((form.weight / (heightInMeter * heightInMeter)).toFixed(1))
})

const bmiMeta = computed(() => {
  if (bmiValue.value < 18.5) return { label: '偏瘦', tone: 'info' as const }
  if (bmiValue.value < 24) return { label: '正常', tone: 'success' as const }
  if (bmiValue.value < 28) return { label: '偏胖', tone: 'warning' as const }
  return { label: '肥胖', tone: 'danger' as const }
})

const bmiLabel = computed(() => bmiMeta.value.label)
const bmiTone = computed(() => bmiMeta.value.tone)

// Seasonal Warning
const seasonalWarning = computed(() => {
  const currentMonth = new Date().getMonth() + 1
  if (currentMonth >= 10 || currentMonth <= 3) {
    return {
      title: '冬季健康预警',
      description: '近期气温较低，注意保暖，预防呼吸道感染。建议适当增加维生素C摄入，保持室内通风。'
    }
  }
  if (currentMonth >= 6 && currentMonth <= 9) {
    return {
      title: '夏季健康预警',
      description: '高温天气注意防暑降温，多补充水分。避免长时间户外活动，注意饮食卫生。'
    }
  }
  return {
    title: '换季健康预警',
    description: '季节交替时期，注意增减衣物，预防感冒。建议加强锻炼，提高免疫力。'
  }
})

// Enhanced ECharts Option
const healthChartOption = computed((): EChartsOption => {
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const data = [22.1, 22.0, 21.8, 21.9, 21.7, 21.6] // BMI trend data

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#111827'
      },
      formatter: (params: any) => {
        const param = params[0]
        return `${param.name}<br/>BMI: <strong>${param.value}</strong>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: {
        lineStyle: { color: '#e5e7eb' }
      },
      axisLabel: {
        color: '#6b7280'
      }
    },
    yAxis: {
      type: 'value',
      min: 18,
      max: 28,
      axisLine: { show: false },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#6b7280'
      }
    },
    series: [
      {
        type: 'line',
        data,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#3b82f6',
          width: 3
        },
        itemStyle: {
          color: '#3b82f6',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0)' }
            ]
          }
        }
      }
    ]
  }
})

// Status Helpers
const appointmentStatusLabel = (status: string): string => {
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

const appointmentStatusTone = (status: string): 'default' | 'success' | 'warning' | 'danger' | 'info' => {
  if (status === 'completed') return 'success'
  if (status === 'confirmed' || status === 'in_progress') return 'info'
  if (status === 'pending' || status === 'rescheduled') return 'warning'
  if (status === 'cancelled' || status === 'missed') return 'danger'
  return 'default'
}

const orderStatusLabel = (status: PharmacyOrder['status']): string => {
  if (status === 'completed') return '已完成'
  if (status === 'pending') return '处理中'
  return '已取消'
}

const orderStatusTone = (status: PharmacyOrder['status']): 'success' | 'warning' | 'default' => {
  if (status === 'completed') return 'success'
  if (status === 'pending') return 'warning'
  return 'default'
}

const SH_TIMEZONE = 'Asia/Shanghai'

const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    timeZone: SH_TIMEZONE
  })
}

// Data Loading
const loadAppointments = async (): Promise<void> => {
  appointmentsLoading.value = true
  try {
    const response = await appointmentsService.mine({ page: 1, page_size: 20 })
    upcomingAppointments.value = response.items
      .filter((item) => ['pending', 'confirmed', 'in_progress'].includes(item.status))
      .slice(0, 4)
  } finally {
    appointmentsLoading.value = false
  }
}

const loadOrders = async (): Promise<void> => {
  ordersLoading.value = true
  try {
    const response = await pharmacyService.listOrders({ page: 1, page_size: 5 })
    recentOrders.value = response.items
  } catch {
    recentOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const parsed = JSON.parse(saved) as { height: number; weight: number }
      form.height = parsed.height
      form.weight = parsed.weight
    } catch {
      // ignore
    }
  }
  void Promise.all([loadAppointments(), loadOrders()])
})

watch(
  () => ({ height: form.height, weight: form.weight }),
  (value) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  },
  { deep: true }
)
</script>

<style scoped>
.dashboard {
  padding: var(--space-4);
}

/* Stats Grid */
.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
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

/* Content Grid */
.dashboard__content {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--space-5);
}

.dashboard__column {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* Cards */
.dashboard__card {
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}

.dashboard__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.dashboard__card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.dashboard__card-desc {
  margin: 0 0 var(--space-4);
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* Form Grid */
.dashboard__form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.dashboard__field label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.dashboard__field input {
  width: 100%;
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  font-size: 14px;
  transition: all var(--transition-fast);
}

.dashboard__field input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

/* List */
.dashboard__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dashboard__list-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-lg);
}

.dashboard__list-icon {
  width: 40px;
  height: 40px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dashboard__list-icon--warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.dashboard__list-icon svg {
  width: 20px;
  height: 20px;
}

.dashboard__list-content {
  flex: 1;
  min-width: 0;
}

.dashboard__list-content strong {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard__list-content span {
  font-size: 13px;
  color: var(--text-secondary);
}

.dashboard__list-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
}

.dashboard__list-date,
.dashboard__list-price {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

/* Empty State */
.dashboard__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  color: var(--text-secondary);
}

.dashboard__empty svg {
  width: 48px;
  height: 48px;
  margin-bottom: var(--space-3);
  color: var(--color-gray-300);
}

.dashboard__empty p {
  margin: 0;
  font-size: 14px;
}

/* Loading */
.dashboard__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-8);
  color: var(--text-secondary);
}

.dashboard__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-color);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 1200px) {
  .dashboard__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard__content {
    grid-template-columns: 1fr;
  }

  .dashboard__stats {
    grid-template-columns: 1fr;
  }
}
</style>
