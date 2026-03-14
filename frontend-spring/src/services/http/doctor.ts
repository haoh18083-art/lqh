import type { Doctor, DoctorListResponse } from '@/types/doctor'
import { request } from './apiClient'

export const doctorService = {
  getDoctors(params?: Record<string, unknown>): Promise<DoctorListResponse> {
    return request.get<DoctorListResponse>('/doctors', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取医生列表失败')
      }
      return response.data
    })
  },
  getDoctor(id: number): Promise<Doctor> {
    return request.get<Doctor>(`/doctors/${id}`).then((response) => {
      if (!response.data) {
        throw new Error('获取医生详情失败')
      }
      return response.data
    })
  },
  getActiveByDepartment(departmentId: number, params?: Record<string, unknown>): Promise<DoctorListResponse> {
    return request.get<DoctorListResponse>('/public/doctors', {
      params: { department_id: departmentId, ...params }
    }).then((response) => {
      if (!response.data) {
        throw new Error('获取公开医生列表失败')
      }
      return response.data
    })
  },
  create(payload: {
    doctor_id: string
    username: string
    password: string
    email: string
    full_name: string
    department_id: number
    title: string
    introduction: string
    phone?: string
  }): Promise<Doctor> {
    return request.post<Doctor>('/doctors', payload).then((response) => {
      if (!response.data) {
        throw new Error('创建医生失败')
      }
      return response.data
    })
  },
  update(id: number, payload: Partial<{
    full_name: string
    email: string
    phone?: string
    department_id: number
    title: string
    introduction: string
  }>): Promise<Doctor> {
    return request.put<Doctor>(`/doctors/${id}`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新医生失败')
      }
      return response.data
    })
  },
  updateStatus(id: number, isActive: boolean): Promise<Doctor> {
    return request.patch<Doctor>(`/doctors/${id}/status`, { is_active: isActive }).then((response) => {
      if (!response.data) {
        throw new Error('更新医生状态失败')
      }
      return response.data
    })
  },
  remove(id: number): Promise<void> {
    return request.delete(`/doctors/${id}`).then(() => undefined)
  }
}
