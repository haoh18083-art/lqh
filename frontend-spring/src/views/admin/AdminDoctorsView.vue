<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Doctors"
        title="医生与科室管理"
        description="真实对接医生与科室接口，支持搜索、增改、启停和科室治理。"
      >
        <template #actions>
          <button class="button" @click="openDoctorModal()">新增医生</button>
          <button class="button button--secondary" @click="openDepartmentModal()">新增科室</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model.trim="filters.search" placeholder="搜索医生姓名 / 工号 / 用户名" />
          <select v-model="filters.department_id">
            <option value="">全部科室</option>
            <option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option>
          </select>
          <button class="button button--secondary" @click="loadDoctors">搜索</button>
          <button class="button button--ghost" @click="resetDoctorFilters">重置</button>
        </div>

        <table v-if="doctors.length" class="data-table">
          <thead>
            <tr>
              <th>工号</th>
              <th>姓名</th>
              <th>科室</th>
              <th>职称</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doctor in doctors" :key="doctor.id">
              <td>{{ doctor.doctor_id }}</td>
              <td>{{ doctor.full_name }}</td>
              <td>{{ doctor.department }}</td>
              <td>{{ doctor.title }}</td>
              <td>{{ doctor.email }}</td>
              <td><StatusBadge :label="doctor.is_active ? '启用' : '禁用'" :tone="doctor.is_active ? 'success' : 'default'" /></td>
              <td>
                <div class="action-stack">
                  <button class="link-button" @click="openDoctorModal(doctor)">编辑</button>
                  <button class="link-button" @click="toggleDoctorStatus(doctor)">
                    {{ doctor.is_active ? '禁用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">医生数据加载中...</div>
        <div v-else class="empty-state">暂无医生数据。</div>
      </section>

      <section class="glass-card panel stack">
        <div class="panel-head">
          <div>
            <h3>科室列表</h3>
            <p class="dashboard-muted">科室管理与医生选择器保持同一份数据源。</p>
          </div>
        </div>

        <table v-if="departments.length" class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>描述</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="department in departments" :key="department.id">
              <td>{{ department.name }}</td>
              <td>{{ department.description || '-' }}</td>
              <td>{{ department.sort_order }}</td>
              <td><StatusBadge :label="department.is_active ? '启用' : '禁用'" :tone="department.is_active ? 'success' : 'default'" /></td>
              <td>
                <div class="action-stack">
                  <button class="link-button" @click="openDepartmentModal(department)">编辑</button>
                  <button class="link-button" @click="toggleDepartmentStatus(department)">
                    {{ department.is_active ? '禁用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>

    <BaseModal :open="doctorModalOpen" :title="doctorDraft.id ? '编辑医生' : '新增医生'" eyebrow="Doctor Form" @close="doctorModalOpen = false">
      <div class="form-grid">
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>工号</label>
            <input v-model.trim="doctorDraft.doctor_id" :disabled="Boolean(doctorDraft.id)" />
          </div>
          <div class="field">
            <label>用户名</label>
            <input v-model.trim="doctorDraft.username" :disabled="Boolean(doctorDraft.id)" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>邮箱</label>
            <input v-model.trim="doctorDraft.email" />
          </div>
          <div class="field">
            <label>姓名</label>
            <input v-model.trim="doctorDraft.full_name" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>科室</label>
            <select v-model="doctorDraft.department_id">
              <option value="">请选择科室</option>
              <option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>职称</label>
            <input v-model.trim="doctorDraft.title" />
          </div>
        </div>
        <div class="field">
          <label>电话</label>
          <input v-model.trim="doctorDraft.phone" />
        </div>
        <div class="field">
          <label>简介</label>
          <textarea v-model.trim="doctorDraft.introduction" rows="4"></textarea>
        </div>
        <div v-if="!doctorDraft.id" class="field">
          <label>初始密码</label>
          <input v-model.trim="doctorDraft.password" type="password" />
        </div>
        <button class="button" :disabled="actionLoading" @click="submitDoctor">
          {{ actionLoading ? '保存中...' : '保存医生信息' }}
        </button>
      </div>
    </BaseModal>

    <BaseModal :open="departmentModalOpen" :title="departmentDraft.id ? '编辑科室' : '新增科室'" eyebrow="Department Form" size="sm" @close="departmentModalOpen = false">
      <div class="form-grid">
        <div class="field">
          <label>名称</label>
          <input v-model.trim="departmentDraft.name" />
        </div>
        <div class="field">
          <label>描述</label>
          <textarea v-model.trim="departmentDraft.description" rows="3"></textarea>
        </div>
        <div class="field">
          <label>排序</label>
          <input v-model.number="departmentDraft.sort_order" type="number" min="0" />
        </div>
        <button class="button" :disabled="actionLoading" @click="submitDepartment">
          {{ actionLoading ? '保存中...' : '保存科室' }}
        </button>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { doctorService } from '@/services/http/doctor'
import { departmentService } from '@/services/http/department'
import type { Doctor } from '@/types/doctor'
import type { Department } from '@/types/department'

const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const doctors = ref<Doctor[]>([])
const departments = ref<Department[]>([])
const doctorModalOpen = ref(false)
const departmentModalOpen = ref(false)

const filters = reactive({
  search: '',
  department_id: '' as number | ''
})

const doctorDraft = reactive({
  id: 0,
  doctor_id: '',
  username: '',
  password: '',
  email: '',
  full_name: '',
  department_id: '' as number | '',
  title: '',
  introduction: '',
  phone: ''
})

const departmentDraft = reactive({
  id: 0,
  name: '',
  description: '',
  sort_order: 0
})

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadDepartments = async (): Promise<void> => {
  const response = await departmentService.getDepartments({ page: 1, page_size: 100 })
  departments.value = response.items
}

const loadDoctors = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const response = await doctorService.getDoctors({
      page: 1,
      page_size: 200,
      search: filters.search || undefined,
      department_id: filters.department_id || undefined
    })
    doctors.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取医生列表失败'
  } finally {
    loading.value = false
  }
}

const resetDoctorFilters = (): void => {
  filters.search = ''
  filters.department_id = ''
  void loadDoctors()
}

const openDoctorModal = (doctor?: Doctor): void => {
  if (doctor) {
    doctorDraft.id = doctor.id
    doctorDraft.doctor_id = doctor.doctor_id
    doctorDraft.username = doctor.username
    doctorDraft.password = ''
    doctorDraft.email = doctor.email
    doctorDraft.full_name = doctor.full_name
    doctorDraft.department_id = doctor.department_id
    doctorDraft.title = doctor.title
    doctorDraft.introduction = doctor.introduction
    doctorDraft.phone = doctor.phone || ''
  } else {
    doctorDraft.id = 0
    doctorDraft.doctor_id = ''
    doctorDraft.username = ''
    doctorDraft.password = ''
    doctorDraft.email = ''
    doctorDraft.full_name = ''
    doctorDraft.department_id = ''
    doctorDraft.title = ''
    doctorDraft.introduction = ''
    doctorDraft.phone = ''
  }
  doctorModalOpen.value = true
}

const openDepartmentModal = (department?: Department): void => {
  if (department) {
    departmentDraft.id = department.id
    departmentDraft.name = department.name
    departmentDraft.description = department.description || ''
    departmentDraft.sort_order = department.sort_order
  } else {
    departmentDraft.id = 0
    departmentDraft.name = ''
    departmentDraft.description = ''
    departmentDraft.sort_order = 0
  }
  departmentModalOpen.value = true
}

const submitDoctor = async (): Promise<void> => {
  resetMessages()
  actionLoading.value = true
  try {
    if (doctorDraft.id) {
      await doctorService.update(doctorDraft.id, {
        full_name: doctorDraft.full_name,
        email: doctorDraft.email,
        phone: doctorDraft.phone || undefined,
        department_id: Number(doctorDraft.department_id),
        title: doctorDraft.title,
        introduction: doctorDraft.introduction
      })
      successMessage.value = '医生信息已更新'
    } else {
      await doctorService.create({
        doctor_id: doctorDraft.doctor_id,
        username: doctorDraft.username,
        password: doctorDraft.password,
        email: doctorDraft.email,
        full_name: doctorDraft.full_name,
        department_id: Number(doctorDraft.department_id),
        title: doctorDraft.title,
        introduction: doctorDraft.introduction,
        phone: doctorDraft.phone || undefined
      })
      successMessage.value = '医生已创建'
    }
    doctorModalOpen.value = false
    await loadDoctors()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存医生失败'
  } finally {
    actionLoading.value = false
  }
}

const toggleDoctorStatus = async (doctor: Doctor): Promise<void> => {
  resetMessages()
  try {
    await doctorService.updateStatus(doctor.id, !doctor.is_active)
    successMessage.value = `医生账号已${doctor.is_active ? '禁用' : '启用'}`
    await loadDoctors()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新医生状态失败'
  }
}

const submitDepartment = async (): Promise<void> => {
  resetMessages()
  actionLoading.value = true
  try {
    if (departmentDraft.id) {
      await departmentService.update(departmentDraft.id, {
        name: departmentDraft.name,
        description: departmentDraft.description || undefined,
        sort_order: departmentDraft.sort_order
      })
      successMessage.value = '科室信息已更新'
    } else {
      await departmentService.create({
        name: departmentDraft.name,
        description: departmentDraft.description || undefined,
        sort_order: departmentDraft.sort_order
      })
      successMessage.value = '科室已创建'
    }
    departmentModalOpen.value = false
    await Promise.all([loadDepartments(), loadDoctors()])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存科室失败'
  } finally {
    actionLoading.value = false
  }
}

const toggleDepartmentStatus = async (department: Department): Promise<void> => {
  resetMessages()
  try {
    await departmentService.updateStatus(department.id, !department.is_active)
    successMessage.value = `科室已${department.is_active ? '禁用' : '启用'}`
    await Promise.all([loadDepartments(), loadDoctors()])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新科室状态失败'
  }
}

onMounted(async () => {
  await Promise.all([loadDepartments(), loadDoctors()])
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

.action-stack {
  display: grid;
  gap: 8px;
}
</style>
