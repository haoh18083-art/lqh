<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Students"
        title="学生管理"
        description="真实对接学生列表、创建、编辑、删除与健康档案读取/更新接口。"
      >
        <template #actions>
          <button class="button" @click="openStudentModal()">新增学生</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model.trim="filters.search" placeholder="搜索姓名或学号" />
          <input v-model.trim="filters.major" placeholder="专业筛选" />
          <input v-model.trim="filters.grade" placeholder="年级筛选" />
          <select v-model="filters.health_status">
            <option value="">全部健康状态</option>
            <option value="良好">良好</option>
            <option value="异常">异常</option>
          </select>
          <button class="button button--secondary" @click="loadStudents">查询</button>
          <button class="button button--ghost" @click="resetFilters">重置</button>
        </div>

        <table v-if="students.length" class="data-table">
          <thead>
            <tr>
              <th>学号</th>
              <th>姓名</th>
              <th>专业</th>
              <th>年级</th>
              <th>健康状态</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="student in students" :key="student.id">
              <td>{{ student.student_id }}</td>
              <td>{{ student.full_name }}</td>
              <td>{{ student.major }}</td>
              <td>{{ student.grade }}</td>
              <td><StatusBadge :label="student.health_status" :tone="student.health_status === '异常' ? 'danger' : 'success'" /></td>
              <td><StatusBadge :label="student.is_active ? '启用' : '禁用'" :tone="student.is_active ? 'success' : 'default'" /></td>
              <td>
                <div class="action-stack">
                  <button class="link-button" @click="openHealthProfile(student)">健康档案</button>
                  <button class="link-button" @click="openStudentModal(student)">编辑</button>
                  <button class="link-button" @click="deleteStudent(student.id)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">学生数据加载中...</div>
        <div v-else class="empty-state">暂无学生数据。</div>
      </section>
    </div>

    <BaseModal :open="studentModalOpen" :title="studentDraft.id ? '编辑学生' : '新增学生'" eyebrow="Student Form" @close="studentModalOpen = false">
      <div class="form-grid">
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>学号</label>
            <input v-model.trim="studentDraft.student_id" :disabled="Boolean(studentDraft.id)" />
          </div>
          <div class="field">
            <label>用户名</label>
            <input v-model.trim="studentDraft.username" :disabled="Boolean(studentDraft.id)" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>姓名</label>
            <input v-model.trim="studentDraft.full_name" />
          </div>
          <div class="field">
            <label>邮箱</label>
            <input v-model.trim="studentDraft.email" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>专业</label>
            <input v-model.trim="studentDraft.major" />
          </div>
          <div class="field">
            <label>年级</label>
            <input v-model.trim="studentDraft.grade" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>班级</label>
            <input v-model.trim="studentDraft.class_name" />
          </div>
          <div class="field">
            <label>电话</label>
            <input v-model.trim="studentDraft.phone" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>性别</label>
            <select v-model="studentDraft.gender">
              <option value="">未设置</option>
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="field">
            <label>生日</label>
            <input v-model="studentDraft.dob" type="date" />
          </div>
        </div>
        <div class="field">
          <label>健康状态</label>
          <select v-model="studentDraft.health_status">
            <option value="良好">良好</option>
            <option value="异常">异常</option>
          </select>
        </div>
        <div v-if="!studentDraft.id" class="field">
          <label>初始密码</label>
          <input v-model.trim="studentDraft.password" type="password" />
        </div>
        <button class="button" :disabled="actionLoading" @click="submitStudent">
          {{ actionLoading ? '保存中...' : '保存学生信息' }}
        </button>
      </div>
    </BaseModal>

    <BaseModal :open="healthModalOpen" title="健康档案" eyebrow="Health Profile" size="lg" @close="healthModalOpen = false">
      <div class="stack">
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>血型</label>
            <input v-model.trim="healthDraft.blood_type" />
          </div>
          <div class="field">
            <label>最近体检时间</label>
            <input v-model="healthDraft.last_checkup_date" type="datetime-local" />
          </div>
        </div>
        <div class="field">
          <label>过敏史（逗号分隔）</label>
          <input v-model.trim="healthDraft.allergies_text" />
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>紧急联系人</label>
            <input v-model.trim="healthDraft.emergency_contact.name" />
          </div>
          <div class="field">
            <label>联系电话</label>
            <input v-model.trim="healthDraft.emergency_contact.phone" />
          </div>
        </div>
        <div class="field">
          <label>关系</label>
          <input v-model.trim="healthDraft.emergency_contact.relationship" />
        </div>
        <div class="field">
          <label>既往病史（每行：疾病|日期|备注）</label>
          <textarea v-model="healthDraft.medical_history_text" rows="6"></textarea>
        </div>
        <button class="button" :disabled="actionLoading || !healthStudentId" @click="submitHealthProfile">
          {{ actionLoading ? '保存中...' : '保存健康档案' }}
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
import { studentService } from '@/services/http/student'
import type { HealthProfile, Student } from '@/types/student'

const students = ref<Student[]>([])
const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const studentModalOpen = ref(false)
const healthModalOpen = ref(false)
const healthStudentId = ref<number | null>(null)

const filters = reactive({
  search: '',
  major: '',
  grade: '',
  health_status: ''
})

const studentDraft = reactive({
  id: 0,
  student_id: '',
  username: '',
  password: '',
  email: '',
  full_name: '',
  major: '',
  grade: '',
  class_name: '',
  phone: '',
  gender: '',
  dob: '',
  health_status: '良好'
})

const healthDraft = reactive({
  blood_type: '',
  last_checkup_date: '',
  allergies_text: '',
  medical_history_text: '',
  emergency_contact: {
    name: '',
    phone: '',
    relationship: ''
  }
})

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadStudents = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const response = await studentService.getStudents({
      page: 1,
      page_size: 200,
      search: filters.search || undefined,
      major: filters.major || undefined,
      grade: filters.grade || undefined,
      health_status: filters.health_status || undefined
    })
    students.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取学生列表失败'
  } finally {
    loading.value = false
  }
}

const resetFilters = (): void => {
  filters.search = ''
  filters.major = ''
  filters.grade = ''
  filters.health_status = ''
  void loadStudents()
}

const openStudentModal = (student?: Student): void => {
  if (student) {
    studentDraft.id = student.id
    studentDraft.student_id = student.student_id
    studentDraft.username = student.username
    studentDraft.password = ''
    studentDraft.email = student.email
    studentDraft.full_name = student.full_name
    studentDraft.major = student.major
    studentDraft.grade = student.grade
    studentDraft.class_name = student.class_name || ''
    studentDraft.phone = student.phone || ''
    studentDraft.gender = student.gender || ''
    studentDraft.dob = student.dob || ''
    studentDraft.health_status = student.health_status
  } else {
    studentDraft.id = 0
    studentDraft.student_id = ''
    studentDraft.username = ''
    studentDraft.password = ''
    studentDraft.email = ''
    studentDraft.full_name = ''
    studentDraft.major = ''
    studentDraft.grade = ''
    studentDraft.class_name = ''
    studentDraft.phone = ''
    studentDraft.gender = ''
    studentDraft.dob = ''
    studentDraft.health_status = '良好'
  }
  studentModalOpen.value = true
}

const submitStudent = async (): Promise<void> => {
  resetMessages()
  actionLoading.value = true
  try {
    if (studentDraft.id) {
      await studentService.update(studentDraft.id, {
        full_name: studentDraft.full_name,
        email: studentDraft.email,
        phone: studentDraft.phone || undefined,
        major: studentDraft.major,
        grade: studentDraft.grade,
        class_name: studentDraft.class_name || undefined,
        health_status: studentDraft.health_status,
        gender: studentDraft.gender || undefined,
        dob: studentDraft.dob || undefined
      })
      successMessage.value = '学生信息已更新'
    } else {
      await studentService.create({
        student_id: studentDraft.student_id,
        username: studentDraft.username,
        password: studentDraft.password,
        email: studentDraft.email,
        full_name: studentDraft.full_name,
        major: studentDraft.major,
        grade: studentDraft.grade,
        class_name: studentDraft.class_name || undefined,
        phone: studentDraft.phone || undefined,
        gender: studentDraft.gender || undefined,
        dob: studentDraft.dob || undefined
      })
      successMessage.value = '学生已创建'
    }
    studentModalOpen.value = false
    await loadStudents()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存学生失败'
  } finally {
    actionLoading.value = false
  }
}

const deleteStudent = async (studentId: number): Promise<void> => {
  if (!window.confirm('确定删除该学生吗？')) return
  resetMessages()
  try {
    await studentService.remove(studentId)
    successMessage.value = '学生已删除'
    await loadStudents()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除学生失败'
  }
}

const openHealthProfile = async (student: Student): Promise<void> => {
  resetMessages()
  healthStudentId.value = student.id
  try {
    const profile = await studentService.getHealthProfile(student.id)
    fillHealthDraft(profile)
    healthModalOpen.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取健康档案失败'
  }
}

const fillHealthDraft = (profile: HealthProfile): void => {
  healthDraft.blood_type = profile.blood_type || ''
  healthDraft.last_checkup_date = profile.last_checkup_date ? profile.last_checkup_date.slice(0, 16) : ''
  healthDraft.allergies_text = (profile.allergies || []).join(', ')
  healthDraft.medical_history_text = (profile.medical_history || [])
    .map((item) => `${item.condition}|${item.date}|${item.notes || ''}`)
    .join('\n')
  healthDraft.emergency_contact.name = profile.emergency_contact?.name || ''
  healthDraft.emergency_contact.phone = profile.emergency_contact?.phone || ''
  healthDraft.emergency_contact.relationship = profile.emergency_contact?.relationship || ''
}

const submitHealthProfile = async (): Promise<void> => {
  if (!healthStudentId.value) return
  resetMessages()
  actionLoading.value = true
  try {
    const medicalHistory = healthDraft.medical_history_text
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [condition, date, notes] = line.split('|')
        return {
          condition: condition?.trim(),
          date: date?.trim(),
          notes: notes?.trim() || undefined
        }
      })

    await studentService.updateHealthProfile(healthStudentId.value, {
      blood_type: healthDraft.blood_type || undefined,
      last_checkup_date: healthDraft.last_checkup_date || undefined,
      allergies: healthDraft.allergies_text.split(',').map((item) => item.trim()).filter(Boolean),
      medical_history: medicalHistory,
      emergency_contact: {
        name: healthDraft.emergency_contact.name,
        phone: healthDraft.emergency_contact.phone,
        relationship: healthDraft.emergency_contact.relationship
      }
    })
    successMessage.value = '健康档案已更新'
    healthModalOpen.value = false
    await loadStudents()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新健康档案失败'
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  void loadStudents()
})
</script>

<style scoped>
.action-stack {
  display: grid;
  gap: 8px;
}
</style>
