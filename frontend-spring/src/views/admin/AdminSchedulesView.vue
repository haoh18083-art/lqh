<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Schedules"
        title="排班管理"
        description="周面板视图管理医生排班，点击格子可编辑或创建排班。"
      />

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <!-- 筛选栏 -->
      <section class="glass-card panel">
        <div class="filter-bar">
          <div class="field">
            <label>科室</label>
            <select v-model="selectedDepartment" @change="onDepartmentChange">
              <option value="">全部科室</option>
              <option v-for="dept in departments" :key="dept.id" :value="dept.id">
                {{ dept.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>医生</label>
            <select v-model="filters.doctor_id" @change="onDoctorChange">
              <option value="">全部医生</option>
              <option v-for="doctor in filteredDoctors" :key="doctor.id" :value="doctor.id">
                {{ doctor.full_name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>&nbsp;</label>
            <button class="button button--secondary" @click="openBulkModal">批量创建排班</button>
          </div>
        </div>
      </section>

      <!-- 周面板 -->
      <section class="glass-card panel week-panel">
        <!-- 周导航 -->
        <div class="week-navigator">
          <div class="week-navigator__buttons">
            <button class="button button--small button--secondary" @click="navigateWeek(-1)">← 上周</button>
            <button class="button button--small button--secondary" @click="goToCurrentWeek">本周</button>
            <button class="button button--small button--secondary" @click="navigateWeek(1)">下周 →</button>
          </div>
          <span class="week-range">{{ weekRangeText }}</span>
        </div>

        <!-- 周网格 -->
        <div class="week-grid-container">
          <!-- 表头 -->
          <div class="week-header">
            <div class="time-column-header">时间</div>
            <div
              v-for="(day, index) in weekDays"
              :key="index"
              class="day-column-header"
              :class="{ 'day-column-header--today': isToday(day) }"
            >
              <div class="day-name">{{ getDayName(day) }}</div>
              <div class="day-date">{{ formatDayDate(day) }}</div>
            </div>
          </div>

          <!-- 网格内容 -->
          <div class="week-grid">
            <div v-for="slot in availableTimeSlots" :key="slot" class="time-row">
              <div class="time-label">{{ slot }}</div>
              <div
                v-for="cell in weekGridData[slot]"
                :key="cell.date"
                class="schedule-cell"
                :class="{
                  'schedule-cell--open': cell.schedule?.status === 'open',
                  'schedule-cell--closed': cell.schedule?.status === 'closed',
                  'schedule-cell--empty': !cell.schedule,
                  'schedule-cell--past': cell.isPast
                }"
                @click="handleCellClick(cell)"
              >
                <template v-if="cell.schedule">
                  <span class="cell-status" :class="`cell-status--${cell.schedule.status}`">
                    {{ cell.schedule.status === 'open' ? '● 开放' : '● 关闭' }}
                  </span>
                  <span class="cell-booking">{{ cell.schedule.booked_count }}/{{ cell.schedule.capacity }}</span>
                </template>
                <template v-else-if="!cell.isPast">
                  <span class="cell-add">+</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 图例 -->
        <div class="week-legend">
          <div class="legend-item">
            <span class="legend-dot legend-dot--open"></span>
            <span>开放预约</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot legend-dot--closed"></span>
            <span>关闭预约</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot legend-dot--empty"></span>
            <span>未排班</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 编辑/创建排班弹窗 -->
    <BaseModal
      :open="modalVisible"
      :title="modalTitle"
      :eyebrow="isCreating ? '创建排班' : '编辑排班'"
      size="sm"
      @close="closeModal"
    >
      <div class="modal-form stack">
        <div class="field">
          <label>日期</label>
          <input :value="editingCell?.date" type="date" disabled />
        </div>
        <div class="field">
          <label>时段</label>
          <input :value="editingCell?.timeSlot" type="text" disabled />
        </div>
        <div v-if="isCreating" class="field">
          <label>医生 *</label>
          <select v-model="editForm.doctor_id">
            <option value="">请选择医生</option>
            <option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id">
              {{ doctor.full_name }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="editForm.status">
            <option value="open">开放预约</option>
            <option value="closed">关闭预约</option>
          </select>
        </div>
        <div class="field">
          <label>容量</label>
          <input v-model.number="editForm.capacity" type="number" min="1" max="100" />
        </div>
        <div class="toolbar">
          <button v-if="!isCreating" class="button button--danger" @click="handleDeleteSchedule">删除</button>
          <div class="toolbar__spacer"></div>
          <button class="button button--secondary" @click="closeModal">取消</button>
          <button class="button" :disabled="actionLoading" @click="handleSaveSchedule">
            {{ actionLoading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </BaseModal>

    <!-- 批量创建排班弹窗 -->
    <BaseModal
      :open="bulkModalVisible"
      title="批量创建排班"
      eyebrow="批量操作"
      size="md"
      @close="closeBulkModal"
    >
      <div class="modal-form stack">
        <!-- 科室 + 医生联动筛选 -->
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>科室</label>
            <select v-model="selectedDepartment" @change="onDepartmentChange">
              <option value="">全部科室</option>
              <option v-for="dept in departments" :key="dept.id" :value="dept.id">
                {{ dept.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>医生 *</label>
            <select v-model="bulkForm.doctor_id" :disabled="!filteredDoctors.length">
              <option value="">请选择医生</option>
              <option v-for="doctor in filteredDoctors" :key="doctor.id" :value="doctor.id">
                {{ doctor.full_name }}
              </option>
            </select>
          </div>
        </div>

        <div class="form-grid form-grid--two">
          <div class="field">
            <label>开始日期 *</label>
            <input v-model="bulkForm.date_from" type="date" />
          </div>
          <div class="field">
            <label>结束日期 *</label>
            <input v-model="bulkForm.date_to" type="date" />
          </div>
        </div>

        <!-- 时段可视化选择 -->
        <div class="field">
          <label>时段选择 *</label>
          <div class="time-slot-presets">
            <button
              v-for="preset in timeSlotPresets"
              :key="preset.label"
              type="button"
              class="button button--small button--secondary"
              @click="applyPreset(preset.slots)"
            >
              {{ preset.label }}
            </button>
            <button type="button" class="button button--small button--ghost" @click="clearTimeSlots">清空</button>
          </div>
          <div class="time-slot-grid">
            <button
              v-for="slot in availableTimeSlots"
              :key="slot"
              type="button"
              class="time-slot-btn"
              :class="{ 'time-slot-btn--selected': selectedTimeSlots.includes(slot) }"
              @click="toggleTimeSlot(slot)"
            >
              {{ slot }}
            </button>
          </div>
        </div>

        <div class="form-grid form-grid--two">
          <div class="field">
            <label>容量</label>
            <input v-model.number="bulkForm.capacity" type="number" min="1" max="100" />
          </div>
          <div class="field">
            <label>状态</label>
            <select v-model="bulkForm.status">
              <option value="open">开放预约</option>
              <option value="closed">关闭预约</option>
            </select>
          </div>
        </div>

        <div class="toolbar">
          <button class="button button--secondary" @click="closeBulkModal">取消</button>
          <button class="button" :disabled="actionLoading" @click="submitBulk">
            {{ actionLoading ? '创建中...' : '批量创建' }}
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { scheduleService } from '@/services/http/schedule'
import { doctorService } from '@/services/http/doctor'
import { departmentService } from '@/services/http/department'
import type { Doctor } from '@/types/doctor'
import type { Department } from '@/types/department'
import type { DoctorScheduleItem } from '@/types/schedule'

// ==================== 类型定义 ====================
interface WeekScheduleCell {
  date: string
  timeSlot: string
  schedule?: DoctorScheduleItem
  isPast: boolean
}

// ==================== 日期工具函数 ====================
const getWeekStart = (date: Date): Date => {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1) // 周一为周开始
  d.setDate(diff)
  d.setHours(0, 0, 0, 0)
  return d
}

const addDays = (date: Date, days: number): Date => {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

const formatDate = (date: Date): string => {
  return date.toISOString().slice(0, 10)
}

const formatDayDate = (date: Date): string => {
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getDayName = (date: Date): string => {
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const day = date.getDay()
  return names[day === 0 ? 6 : day - 1]
}

const isToday = (date: Date): boolean => {
  const today = new Date()
  return formatDate(date) === formatDate(today)
}

const isBefore = (date: Date, compareDate: Date): boolean => {
  const d1 = new Date(date)
  const d2 = new Date(compareDate)
  d1.setHours(0, 0, 0, 0)
  d2.setHours(0, 0, 0, 0)
  return d1 < d2
}

// ==================== 响应式数据 ====================
const departments = ref<Department[]>([])
const selectedDepartment = ref<number | ''>('')
const doctors = ref<Doctor[]>([])
const schedules = ref<DoctorScheduleItem[]>([])
const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 周导航
const currentWeekStart = ref(getWeekStart(new Date()))

// 弹窗状态
const modalVisible = ref(false)
const bulkModalVisible = ref(false)
const editingCell = ref<WeekScheduleCell | null>(null)
const isCreating = ref(false)

// 编辑表单
const editForm = reactive({
  doctor_id: '' as number | '',
  status: 'open' as 'open' | 'closed',
  capacity: 8
})

// 批量表单
const timeSlotPresets = [
  { label: '上午', slots: ['08:00', '09:00', '10:00', '11:00'] },
  { label: '下午', slots: ['14:00', '15:00', '16:00', '17:00'] },
  { label: '全天', slots: ['08:00', '09:00', '10:00', '11:00', '14:00', '15:00', '16:00', '17:00'] }
]

const availableTimeSlots = ['08:00', '09:00', '10:00', '11:00', '14:00', '15:00', '16:00', '17:00']
const selectedTimeSlots = ref<string[]>([])

const bulkForm = reactive({
  doctor_id: '' as number | '',
  date_from: formatDate(new Date()),
  date_to: formatDate(addDays(new Date(), 13)),
  capacity: 8,
  status: 'open' as 'open' | 'closed'
})

const filters = reactive({
  doctor_id: '' as number | '',
  date_from: '',
  date_to: ''
})

// ==================== 计算属性 ====================
const filteredDoctors = computed(() => {
  if (!selectedDepartment.value) {
    return doctors.value
  }
  return doctors.value.filter(d => d.department_id === selectedDepartment.value)
})

const weekDays = computed(() => {
  const days: Date[] = []
  for (let i = 0; i < 7; i++) {
    days.push(addDays(currentWeekStart.value, i))
  }
  return days
})

const weekRangeText = computed(() => {
  const start = weekDays.value[0]
  const end = weekDays.value[6]
  const startStr = `${start.getFullYear()}年${start.getMonth() + 1}月${start.getDate()}日`
  const endStr = `${end.getMonth() + 1}月${end.getDate()}日`
  return `${startStr} - ${endStr}`
})

const weekGridData = computed(() => {
  const grid: Record<string, WeekScheduleCell[]> = {}
  for (const slot of availableTimeSlots) {
    grid[slot] = weekDays.value.map(day => {
      const dateStr = formatDate(day)
      const schedule = schedules.value.find(s => s.date === dateStr && s.time_slot === slot)
      return {
        date: dateStr,
        timeSlot: slot,
        schedule,
        isPast: isBefore(day, new Date())
      }
    })
  }
  return grid
})

const modalTitle = computed(() => {
  if (!editingCell.value) return ''
  const date = editingCell.value.date
  const slot = editingCell.value.timeSlot
  return `${date} ${slot}`
})

// ==================== 方法 ====================
const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

// 周导航
const navigateWeek = (direction: number) => {
  currentWeekStart.value = addDays(currentWeekStart.value, direction * 7)
}

const goToCurrentWeek = () => {
  currentWeekStart.value = getWeekStart(new Date())
}

// 数据加载
const loadDepartments = async (): Promise<void> => {
  try {
    const response = await departmentService.getActiveDepartments()
    departments.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取科室列表失败'
  }
}

const loadDoctors = async (): Promise<void> => {
  try {
    const response = await doctorService.getDoctors({ page: 1, page_size: 200 })
    doctors.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取医生列表失败'
  }
}

const loadSchedules = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const endDate = weekDays.value[6]
    const response = await scheduleService.list({
      page: 1,
      page_size: 500,
      doctor_id: filters.doctor_id || undefined,
      date_from: formatDate(weekDays.value[0]),
      date_to: formatDate(endDate)
    })
    schedules.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取排班失败'
  } finally {
    loading.value = false
  }
}

// 筛选联动
const onDepartmentChange = () => {
  bulkForm.doctor_id = ''
}

const onDoctorChange = () => {
  loadSchedules()
}

// 时段选择
const toggleTimeSlot = (slot: string) => {
  const index = selectedTimeSlots.value.indexOf(slot)
  if (index > -1) {
    selectedTimeSlots.value.splice(index, 1)
  } else {
    selectedTimeSlots.value.push(slot)
  }
}

const applyPreset = (slots: string[]) => {
  selectedTimeSlots.value = [...slots]
}

const clearTimeSlots = () => {
  selectedTimeSlots.value = []
}

// 弹窗操作
const handleCellClick = (cell: WeekScheduleCell) => {
  if (cell.isPast) return
  editingCell.value = cell
  isCreating.value = !cell.schedule

  if (cell.schedule) {
    // 编辑模式 - 填充表单
    editForm.status = cell.schedule.status
    editForm.capacity = cell.schedule.capacity
    editForm.doctor_id = cell.schedule.doctor_id
  } else {
    // 创建模式 - 重置表单
    editForm.status = 'open'
    editForm.capacity = 8
    editForm.doctor_id = filters.doctor_id || ''
  }

  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  editingCell.value = null
}

const openBulkModal = () => {
  bulkModalVisible.value = true
}

const closeBulkModal = () => {
  bulkModalVisible.value = false
}

// 保存排班
const handleSaveSchedule = async () => {
  if (!editingCell.value) return

  resetMessages()
  actionLoading.value = true

  try {
    if (isCreating.value) {
      // 创建新排班
      if (!editForm.doctor_id) {
        errorMessage.value = '请选择医生'
        actionLoading.value = false
        return
      }
      await scheduleService.create({
        doctor_id: editForm.doctor_id as number,
        date: editingCell.value.date,
        time_slots: [editingCell.value.timeSlot],
        capacity: editForm.capacity
      })
      successMessage.value = '排班创建成功'
    } else if (editingCell.value.schedule) {
      // 更新现有排班
      await scheduleService.update(editingCell.value.schedule.id, {
        capacity: editForm.capacity,
        status: editForm.status
      })
      successMessage.value = '排班更新成功'
    }

    closeModal()
    await loadSchedules()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存排班失败'
  } finally {
    actionLoading.value = false
  }
}

// 删除排班
const handleDeleteSchedule = async () => {
  if (!editingCell.value?.schedule) return

  // 使用自定义确认框而不是 confirm
  const confirmed = window.confirm('确定要删除此排班吗？')
  if (!confirmed) return

  resetMessages()
  actionLoading.value = true

  try {
    // 使用 update 将状态设为 closed 或调用删除接口
    await scheduleService.update(editingCell.value.schedule.id, {
      status: 'closed'
    })
    successMessage.value = '排班已关闭'
    closeModal()
    await loadSchedules()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除排班失败'
  } finally {
    actionLoading.value = false
  }
}

// 批量创建
const submitBulk = async (): Promise<void> => {
  resetMessages()
  if (!bulkForm.doctor_id) {
    errorMessage.value = '请选择医生'
    return
  }
  if (!selectedTimeSlots.value.length) {
    errorMessage.value = '请至少选择一个时间段'
    return
  }

  actionLoading.value = true
  try {
    await scheduleService.bulkUpsert({
      doctor_id: bulkForm.doctor_id,
      date_from: bulkForm.date_from,
      date_to: bulkForm.date_to,
      time_slots: selectedTimeSlots.value,
      capacity: bulkForm.capacity,
      status: bulkForm.status
    })
    successMessage.value = '排班已批量创建'
    closeBulkModal()
    await loadSchedules()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '批量创建排班失败'
  } finally {
    actionLoading.value = false
  }
}

// ==================== 监听 ====================
watch(currentWeekStart, () => {
  loadSchedules()
}, { immediate: false })

// ==================== 生命周期 ====================
onMounted(async () => {
  await Promise.all([loadDepartments(), loadDoctors()])
  await loadSchedules()
})
</script>

<style scoped>
/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-bar .field {
  margin-bottom: 0;
}

/* 周导航 */
.week-navigator {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  margin-bottom: 16px;
}

.week-navigator__buttons {
  display: flex;
  gap: 8px;
}

.week-range {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

/* 周面板网格 */
.week-grid-container {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-lg, 8px);
  overflow: hidden;
}

.week-header {
  display: grid;
  grid-template-columns: 70px repeat(7, 1fr);
  background: var(--color-bg-secondary, #f3f4f6);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.time-column-header {
  padding: 12px 8px;
  text-align: center;
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
  border-right: 1px solid var(--border-color, #e5e7eb);
}

.day-column-header {
  padding: 12px 8px;
  text-align: center;
  border-right: 1px solid var(--border-color, #e5e7eb);
}

.day-column-header:last-child {
  border-right: none;
}

.day-column-header--today {
  background: var(--color-primary-light, #e0f2fe);
}

.day-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary, #111827);
}

.day-date {
  font-size: 12px;
  color: var(--text-secondary, #6b7280);
  margin-top: 2px;
}

/* 网格行 */
.week-grid {
  background: var(--color-white, #fff);
}

.time-row {
  display: grid;
  grid-template-columns: 70px repeat(7, 1fr);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.time-row:last-child {
  border-bottom: none;
}

.time-label {
  padding: 12px 8px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #6b7280);
  background: var(--color-bg-secondary, #f3f4f6);
  border-right: 1px solid var(--border-color, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 排班单元格 */
.schedule-cell {
  padding: 8px;
  min-height: 70px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-right: 1px solid var(--border-color, #e5e7eb);
  background: var(--color-white, #fff);
}

.schedule-cell:last-child {
  border-right: none;
}

.schedule-cell:hover {
  background: var(--color-bg-hover, #f9fafb);
}

/* 开放状态 */
.schedule-cell--open {
  background: var(--color-success-bg, #f0fdf4);
}

.schedule-cell--open:hover {
  background: var(--color-success-bg-hover, #dcfce7);
}

/* 关闭状态 */
.schedule-cell--closed {
  background: var(--color-error-bg, #fef2f2);
}

.schedule-cell--closed:hover {
  background: var(--color-error-bg-hover, #fee2e2);
}

/* 过期时段 */
.schedule-cell--past {
  opacity: 0.4;
  cursor: not-allowed;
  background: var(--color-bg-disabled, #f3f4f6);
}

.cell-status {
  font-size: 12px;
  font-weight: 600;
}

.cell-status--open {
  color: var(--color-success, #16a34a);
}

.cell-status--closed {
  color: var(--color-error, #dc2626);
}

.cell-booking {
  font-size: 11px;
  color: var(--text-secondary, #6b7280);
}

.cell-add {
  font-size: 20px;
  color: var(--text-tertiary, #9ca3af);
  font-weight: 300;
}

.schedule-cell:hover .cell-add {
  color: var(--color-primary, #0ea5e9);
}

/* 图例 */
.week-legend {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-dot--open {
  background: var(--color-success-bg, #f0fdf4);
  border: 1px solid var(--color-success, #16a34a);
}

.legend-dot--closed {
  background: var(--color-error-bg, #fef2f2);
  border: 1px solid var(--color-error, #dc2626);
}

.legend-dot--empty {
  background: var(--color-white, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
}

/* 时段选择 */
.time-slot-presets {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.time-slot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.time-slot-btn {
  padding: 12px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: var(--radius-md, 6px);
  background: var(--color-white, #fff);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.time-slot-btn:hover {
  border-color: var(--color-primary, #1890ff);
  color: var(--color-primary, #1890ff);
}

.time-slot-btn--selected {
  background: var(--color-primary, #1890ff);
  color: var(--color-white, #fff);
  border-color: var(--color-primary, #1890ff);
}

/* 弹窗表单 */
.modal-form .field {
  margin-bottom: 0;
}

.modal-form input:disabled {
  background: var(--color-bg-secondary, #f3f4f6);
  cursor: not-allowed;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.toolbar__spacer {
  flex: 1;
}

/* 按钮样式 */
.button--small {
  padding: 6px 12px;
  font-size: 13px;
}

.button--ghost {
  background: transparent;
  border: 1px solid var(--border-color, #ddd);
  color: var(--text-secondary, #666);
}

.button--ghost:hover {
  background: var(--color-bg-secondary, #f5f5f5);
}

.button--danger {
  background: var(--color-error, #dc2626);
  color: var(--color-white, #fff);
}

.button--danger:hover {
  background: var(--color-error-dark, #b91c1c);
}

/* 响应式 */
@media (max-width: 1024px) {
  .week-header,
  .time-row {
    grid-template-columns: 60px repeat(7, 1fr);
  }

  .time-column-header,
  .time-label {
    font-size: 11px;
    padding: 8px 4px;
  }

  .day-column-header {
    padding: 8px 4px;
  }

  .day-name {
    font-size: 12px;
  }

  .day-date {
    font-size: 10px;
  }

  .schedule-cell {
    padding: 4px;
    min-height: 50px;
  }

  .cell-status {
    font-size: 10px;
  }

  .cell-booking {
    font-size: 9px;
  }
}
</style>
