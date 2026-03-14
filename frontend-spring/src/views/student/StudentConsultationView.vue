<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Student / Consultation"
        title="就诊助手"
        description="保持当前前端的两段式逻辑：左侧完成预约挂号，右侧管理本地健康档案与真实就诊记录。"
      />

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="split-grid">
        <article class="glass-card panel stack">
          <div class="panel-head">
            <div>
              <h3>预约挂号</h3>
              <p class="dashboard-muted">依次选择科室、医生、时段，并提交本次主诉。</p>
            </div>
            <RouterLink class="button button--secondary" to="/student/appointments">查看预约管理</RouterLink>
          </div>

          <div class="form-grid form-grid--two">
            <div class="field">
              <label>科室</label>
              <select v-model="selectedDepartmentId">
                <option value="">请选择科室</option>
                <option v-for="department in departments" :key="department.id" :value="department.id">
                  {{ department.name }}
                </option>
              </select>
            </div>
            <div class="field">
              <label>医生</label>
              <select v-model="selectedDoctorId" :disabled="!selectedDepartmentId || doctorsLoading">
                <option value="">请选择医生</option>
                <option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id">
                  {{ doctor.full_name }} · {{ doctor.title }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-grid form-grid--two">
            <div class="field">
              <label>起始日期</label>
              <input v-model="dateFrom" type="date" />
            </div>
            <div class="field">
              <label>结束日期</label>
              <input v-model="dateTo" type="date" />
            </div>
          </div>

          <div class="toolbar">
            <button class="button" :disabled="!selectedDoctorId || slotsLoading" @click="loadSlots">
              {{ slotsLoading ? '加载时段中...' : '查询可预约时段' }}
            </button>
          </div>

          <div class="slot-grid">
            <button
              v-for="slot in slots"
              :key="`${slot.date}-${slot.time}`"
              type="button"
              class="slot-card"
              :class="{
                'slot-card--selected': selectedSlotKey === `${slot.date}-${slot.time}`,
                'slot-card--disabled': slot.status !== 'available'
              }"
              :disabled="slot.status !== 'available'"
              @click="selectedSlotKey = `${slot.date}-${slot.time}`"
            >
              <strong>{{ slot.date }} · {{ slot.time }}</strong>
              <span>{{ slotStatusLabel(slot.status) }}</span>
              <small>{{ slot.booked_count }}/{{ slot.capacity }} 已预约</small>
            </button>
            <div v-if="!slots.length && !slotsLoading" class="empty-state">请选择医生并查询可预约时段。</div>
          </div>

          <div class="field">
            <label>主诉</label>
            <textarea v-model.trim="symptoms" rows="4" placeholder="请输入本次就诊主诉"></textarea>
          </div>

          <button class="button" :disabled="creatingAppointment || !selectedSlot" @click="submitAppointment">
            {{ creatingAppointment ? '提交中...' : '提交预约' }}
          </button>
        </article>

        <div class="stack">
          <article class="glass-card panel stack">
            <div class="panel-head">
              <div>
                <h3>健康档案录入</h3>
                <p class="dashboard-muted">与现有 React 页一致，这部分仍为本地 BMI 表单。</p>
              </div>
              <StatusBadge :label="bmiLabel" :tone="bmiTone" />
            </div>
            <div class="form-grid form-grid--two">
              <div class="field">
                <label>身高 (cm)</label>
                <input v-model.number="healthForm.height" type="number" min="100" max="250" />
              </div>
              <div class="field">
                <label>体重 (kg)</label>
                <input v-model.number="healthForm.weight" type="number" min="30" max="200" />
              </div>
            </div>
            <div class="notice">
              实时 BMI：<strong>{{ bmiValue }}</strong> · {{ bmiLabel }}
            </div>
          </article>

          <article class="glass-card panel">
            <div class="panel-head">
              <div>
                <h3>就诊记录</h3>
                <p class="dashboard-muted">真实读取 Mongo 病历摘要。</p>
              </div>
              <button class="button button--ghost" @click="loadMedicalRecords">刷新</button>
            </div>
            <div v-if="medicalRecordsLoading" class="empty-state">病历加载中...</div>
            <table v-else-if="medicalRecords.length" class="data-table">
              <thead>
                <tr>
                  <th>就诊日期</th>
                  <th>科室</th>
                  <th>诊断结论</th>
                  <th>主治医生</th>
                  <th>费用</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in medicalRecords" :key="record.id">
                  <td>{{ record.visit_date }} {{ record.time_slot }}</td>
                  <td>{{ record.department_name }}</td>
                  <td>{{ record.diagnosis_summary?.conclusion || '-' }}</td>
                  <td>{{ record.doctor_name }}</td>
                  <td>{{ formatMoney(record.fee_total) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-state">暂无就诊记录。</div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { departmentService } from '@/services/http/department'
import { doctorService } from '@/services/http/doctor'
import { appointmentsService } from '@/services/http/appointments'
import { medicalRecordsService } from '@/services/http/medicalRecords'
import type { Department } from '@/types/department'
import type { Doctor } from '@/types/doctor'
import type { AppointmentSlotResponse } from '@/types/appointment'
import type { MedicalRecordItem } from '@/types/medicalRecord'
import { formatMoney } from '@/utils/format'

const STORAGE_KEY = 'student_consultation_health_form'

const departments = ref<Department[]>([])
const doctors = ref<Doctor[]>([])
const slots = ref<AppointmentSlotResponse[]>([])
const medicalRecords = ref<MedicalRecordItem[]>([])

const selectedDepartmentId = ref<number | ''>('')
const selectedDoctorId = ref<number | ''>('')
const selectedSlotKey = ref('')
const symptoms = ref('')
const dateFrom = ref(new Date().toISOString().slice(0, 10))
const dateTo = ref(new Date(Date.now() + (6 * 24 * 60 * 60 * 1000)).toISOString().slice(0, 10))

const doctorsLoading = ref(false)
const slotsLoading = ref(false)
const medicalRecordsLoading = ref(false)
const creatingAppointment = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const healthForm = reactive({
  height: 175,
  weight: 65
})

const bmiValue = computed(() => {
  const heightInMeter = healthForm.height / 100
  return Number((healthForm.weight / (heightInMeter * heightInMeter)).toFixed(1))
})

const bmiMeta = computed(() => {
  if (bmiValue.value < 18.5) return { label: '偏瘦', tone: 'info' as const }
  if (bmiValue.value < 24) return { label: '正常', tone: 'success' as const }
  if (bmiValue.value < 28) return { label: '偏胖', tone: 'warning' as const }
  return { label: '肥胖', tone: 'danger' as const }
})
const bmiLabel = computed(() => bmiMeta.value.label)
const bmiTone = computed(() => bmiMeta.value.tone)

const selectedSlot = computed(() => slots.value.find((slot) => `${slot.date}-${slot.time}` === selectedSlotKey.value) || null)

const slotStatusLabel = (status: string): string => {
  if (status === 'available') return '可预约'
  if (status === 'full') return '已满'
  if (status === 'booked') return '已预约'
  return '关闭'
}

const resetNotice = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadDepartments = async (): Promise<void> => {
  const response = await departmentService.getActiveDepartments({ page: 1, page_size: 100 })
  departments.value = response.items
}

const loadDoctors = async (): Promise<void> => {
  doctors.value = []
  selectedDoctorId.value = ''
  selectedSlotKey.value = ''
  slots.value = []

  if (!selectedDepartmentId.value) {
    return
  }

  doctorsLoading.value = true
  try {
    const response = await doctorService.getActiveByDepartment(Number(selectedDepartmentId.value), { page: 1, page_size: 100 })
    doctors.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取医生列表失败'
  } finally {
    doctorsLoading.value = false
  }
}

const loadSlots = async (): Promise<void> => {
  resetNotice()
  if (!selectedDoctorId.value) {
    errorMessage.value = '请先选择医生'
    return
  }

  slotsLoading.value = true
  try {
    slots.value = await appointmentsService.getSlots({
      doctor_id: Number(selectedDoctorId.value),
      date_from: dateFrom.value,
      date_to: dateTo.value
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取可预约时段失败'
  } finally {
    slotsLoading.value = false
  }
}

const loadMedicalRecords = async (): Promise<void> => {
  medicalRecordsLoading.value = true
  try {
    const response = await medicalRecordsService.mine({ page: 1, page_size: 6 })
    medicalRecords.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取病历失败'
  } finally {
    medicalRecordsLoading.value = false
  }
}

const submitAppointment = async (): Promise<void> => {
  resetNotice()
  if (!selectedSlot.value || !selectedDoctorId.value) {
    errorMessage.value = '请选择预约时段'
    return
  }

  creatingAppointment.value = true
  try {
    await appointmentsService.create({
      doctor_id: Number(selectedDoctorId.value),
      date: selectedSlot.value.date,
      time: selectedSlot.value.time,
      symptoms: symptoms.value || undefined
    })
    successMessage.value = '预约成功，已同步到预约管理页面。'
    symptoms.value = ''
    selectedSlotKey.value = ''
    await Promise.all([loadSlots(), loadMedicalRecords()])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '预约失败'
  } finally {
    creatingAppointment.value = false
  }
}

onMounted(async () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const parsed = JSON.parse(saved) as { height: number; weight: number }
      healthForm.height = parsed.height
      healthForm.weight = parsed.weight
    } catch {
      // ignore
    }
  }

  await Promise.all([loadDepartments(), loadMedicalRecords()])
})

watch(selectedDepartmentId, () => {
  void loadDoctors()
})

watch(
  () => ({ height: healthForm.height, weight: healthForm.weight }),
  (value) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  },
  { deep: true }
)
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
  gap: 12px;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.slot-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.78);
  text-align: left;
  cursor: pointer;
  display: grid;
  gap: 8px;
}

.slot-card--selected {
  border-color: rgba(39, 88, 80, 0.4);
  box-shadow: inset 0 0 0 1px rgba(39, 88, 80, 0.3);
}

.slot-card--disabled {
  cursor: not-allowed;
  opacity: 0.58;
}
</style>
