import type { HealthProfile, Student, StudentListResponse } from '@/types/student'
import { request } from './apiClient'

export const studentService = {
  getStudents(params?: Record<string, unknown>): Promise<StudentListResponse> {
    return request.get<StudentListResponse>('/students', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取学生列表失败')
      }
      return response.data
    })
  },
  getStudent(studentId: number): Promise<Student> {
    return request.get<Student>(`/students/${studentId}`).then((response) => {
      if (!response.data) {
        throw new Error('获取学生详情失败')
      }
      return response.data
    })
  },
  getHealthProfile(studentId: number): Promise<HealthProfile> {
    return request.get<HealthProfile>(`/students/${studentId}/health-profile`).then((response) => {
      if (!response.data) {
        throw new Error('获取健康档案失败')
      }
      return response.data
    })
  },
  create(payload: {
    student_id: string
    username: string
    password: string
    email: string
    full_name: string
    major: string
    grade: string
    class_name?: string
    phone?: string
    gender?: string
    dob?: string
  }): Promise<Student> {
    return request.post<Student>('/students', payload).then((response) => {
      if (!response.data) {
        throw new Error('创建学生失败')
      }
      return response.data
    })
  },
  update(studentId: number, payload: Partial<{
    full_name: string
    email: string
    phone?: string
    major: string
    grade: string
    class_name?: string
    health_status?: string
    gender?: string
    dob?: string
  }>): Promise<Student> {
    return request.put<Student>(`/students/${studentId}`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新学生失败')
      }
      return response.data
    })
  },
  remove(studentId: number): Promise<void> {
    return request.delete(`/students/${studentId}`).then(() => undefined)
  },
  updateHealthProfile(studentId: number, payload: {
    blood_type?: string
    last_checkup_date?: string
    allergies?: string[]
    medical_history?: Array<{ condition: string; date: string; notes?: string }>
    emergency_contact?: { name: string; phone: string; relationship: string }
  }): Promise<HealthProfile> {
    return request.put<HealthProfile>(`/students/${studentId}/health-profile`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新健康档案失败')
      }
      return response.data
    })
  }
}
