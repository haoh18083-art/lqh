<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Dashboard"
        title="管理运营总览"
        description="结合真实后端统计入口和现有前端看板职责，展示医生、学生、药品与实时预警通道状态。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadDashboard">刷新看板</button>
          <button class="button" @click="socketStore.connect">连接预警通道</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>

      <section class="dashboard__stats">
        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--primary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">医生总数</span>
            <strong class="stat-card__value">{{ stats.doctors }}</strong>
            <span class="stat-card__trend">在职医护人员</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--info">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">学生总数</span>
            <strong class="stat-card__value">{{ stats.students }}</strong>
            <span class="stat-card__trend">注册用户</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--success">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">药品总数</span>
            <strong class="stat-card__value">{{ stats.medicines }}</strong>
            <span class="stat-card__trend">库存药品</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__icon stat-card__icon--warning">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4.24 17.34l-1.24 2.66 2.66-1.24L18.5 4.42l-1.42-1.42L4.24 17.34z"/>
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">预警通道</span>
            <strong class="stat-card__value" :class="{
              'status--connected': socketStore.status === 'connected',
              'status--disconnected': socketStore.status === 'disconnected',
              'status--connecting': socketStore.status === 'connecting'
            }">{{ socketStore.status === 'connected' ? '已连接' : socketStore.status === 'connecting' ? '连接中' : '待机' }}</strong>
            <span class="stat-card__trend" :class="{
              'stat-card__trend--up': socketStore.status === 'connected',
              'stat-card__trend--error': socketStore.status === 'disconnected'
            }">{{ socketStore.status === 'connected' ? '实时接收预警' : '点击连接预警通道' }}</span>
          </div>
        </article>
      </section>

      <section class="split-grid">
        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>资源负荷趋势</h3>
              <p class="dashboard-muted">图表承接现有 ECharts 视觉职责，数据来自实时实体数量与展示型趋势。 </p>
            </div>
          </div>
          <BaseChart :option="chartOption" />
        </article>

        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>低库存药品</h3>
              <p class="dashboard-muted">当前基于后端药品库存字段实时计算。</p>
            </div>
            <RouterLink class="link-button" to="/admin/medicines">进入库存管理</RouterLink>
          </div>
          <ul v-if="lowStockMedicines.length" class="list-block">
            <li v-for="medicine in lowStockMedicines" :key="medicine.id" class="list-item">
              <div>
                <strong>{{ medicine.name }}</strong>
                <p>{{ medicine.spec || '-' }}</p>
              </div>
              <StatusBadge :label="`库存 ${medicine.stock}`" :tone="medicine.stock === 0 ? 'danger' : 'warning'" />
            </li>
          </ul>
          <div v-else class="empty-state">当前没有低库存药品。</div>
        </article>
      </section>

      <!-- 运营统计图表 -->
      <section class="dashboard__charts">
        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>药品消耗与疾病关联</h3>
              <p class="dashboard-muted">热力图展示不同疾病对各药品的消耗量分布</p>
            </div>
          </div>
          <MedicineDiseaseChart
            :diseases="operationCharts.medicine_disease_heatmap.diseases"
            :medicines="operationCharts.medicine_disease_heatmap.medicines"
            :points="operationCharts.medicine_disease_heatmap.points"
          />
        </article>

        <div class="split-grid">
          <article class="glass-card panel stack">
            <div class="panel-head">
              <div>
                <h3>药品消耗与季节趋势</h3>
                <p class="dashboard-muted">按真实药品名称聚合的月度消耗趋势（展示本年消耗量 Top 5，数据截止至本年当前时段）</p>
              </div>
            </div>
            <MedicineSeasonChart
              :months="operationCharts.medicine_season_series.months"
              :series="operationCharts.medicine_season_series.series"
              :current-month="operationCharts.meta.current_month"
            />
          </article>

          <article class="glass-card panel stack">
            <div class="panel-head">
              <div>
                <h3>疾病与季节分布</h3>
                <p class="dashboard-muted">各月份不同疾病发病趋势（数据截止至本年当前时段）</p>
              </div>
            </div>
            <DiseaseSeasonChart
              :months="operationCharts.disease_season_series.months"
              :series="operationCharts.disease_season_series.series"
              :current-month="operationCharts.meta.current_month"
            />
          </article>
        </div>
      </section>

      <section class="glass-card panel stack">
        <div class="panel-head">
          <div>
            <h3>实时预警消息</h3>
            <p class="dashboard-muted">当前只保留 WebSocket 接入与消息展示，不扩展新的后端协议。</p>
          </div>
          <div class="toolbar">
            <button class="button button--secondary" @click="socketStore.disconnect">断开连接</button>
          </div>
        </div>
        <table v-if="socketStore.messages.length" class="data-table">
          <thead>
            <tr>
              <th>Topic</th>
              <th>接收时间</th>
              <th>Payload</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="message in socketStore.messages" :key="message.received_at">
              <td>{{ message.topic }}</td>
              <td>{{ formatDateTime(message.received_at) }}</td>
              <td class="payload-cell">{{ JSON.stringify(message.payload) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无预警消息，连接后会在此滚动显示。</div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import PageHero from '@/components/common/PageHero.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import MedicineDiseaseChart from '@/components/dashboard/MedicineDiseaseChart.vue'
import MedicineSeasonChart from '@/components/dashboard/MedicineSeasonChart.vue'
import DiseaseSeasonChart from '@/components/dashboard/DiseaseSeasonChart.vue'
import { doctorService } from '@/services/http/doctor'
import { studentService } from '@/services/http/student'
import { medicineService } from '@/services/http/medicine'
import { adminDashboardService } from '@/services/http/adminDashboard'
import type { AdminOperationChartsResponse } from '@/services/http/adminDashboard'
import { useSocketStore } from '@/stores/socket'
import { formatDateTime } from '@/utils/format'

const socketStore = useSocketStore()
const errorMessage = ref('')
const lowStockMedicines = ref<Array<{ id: number; name: string; spec?: string | null; stock: number }>>([])
const operationCharts = ref<AdminOperationChartsResponse>({
  medicine_disease_heatmap: {
    diseases: [] as string[],
    medicines: [] as string[],
    points: [] as Array<{ disease_index: number; medicine_index: number; quantity: number }>
  },
  medicine_season_series: {
    months: [] as string[],
    series: [] as Array<{ name: string; data: number[] }>
  },
  disease_season_series: {
    months: [] as string[],
    series: [] as Array<{ name: string; data: number[] }>
  },
  meta: {
    current_year: new Date().getFullYear(),
    current_month: new Date().getMonth() + 1,
    generated_at: ''
  }
})
const stats = reactive({
  doctors: 0,
  students: 0,
  medicines: 0
})

const chartOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['医生', '学生', '药品', '低库存'] },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data: [stats.doctors, stats.students, stats.medicines, lowStockMedicines.value.length],
      itemStyle: {
        color: (params: { dataIndex: number }) => ['#275850', '#4a7ab0', '#d37a52', '#b94732'][params.dataIndex]
      }
    }
  ]
}))

const loadDashboard = async (): Promise<void> => {
  errorMessage.value = ''
  try {
    const [doctors, students, medicines, charts] = await Promise.all([
      doctorService.getDoctors({ page: 1, page_size: 1 }),
      studentService.getStudents({ page: 1, page_size: 1 }),
      medicineService.list({ page: 1, page_size: 200, is_active: true }),
      adminDashboardService.getOperationCharts()
    ])

    stats.doctors = doctors.total
    stats.students = students.total
    stats.medicines = medicines.total
    lowStockMedicines.value = medicines.items.filter((item) => item.stock <= 10).slice(0, 8)
    operationCharts.value = charts
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载管理看板失败'
  }
}

onMounted(() => {
  void loadDashboard()
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

.payload-cell {
  max-width: 420px;
  word-break: break-all;
}

.list-block {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(24, 49, 45, 0.08);
}

.list-item:last-child {
  border-bottom: none;
}

.list-item p {
  margin: 6px 0 0;
  color: var(--muted);
}

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

.stat-card__trend--error {
  color: var(--color-danger);
}

.status--connected {
  color: var(--color-success);
}

.status--connecting {
  color: var(--color-warning);
}

.status--disconnected {
  color: var(--text-secondary);
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
</style>
