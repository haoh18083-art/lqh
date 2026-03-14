import type { Department, DepartmentListResponse } from '@/types/department'
import { request } from './apiClient'

export const departmentService = {
  getDepartments(params?: Record<string, unknown>): Promise<DepartmentListResponse> {
    return request.get<DepartmentListResponse>('/departments', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取科室列表失败')
      }
      return response.data
    })
  },
  getActiveDepartments(params?: Record<string, unknown>): Promise<DepartmentListResponse> {
    return request.get<DepartmentListResponse>('/public/departments', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取启用科室失败')
      }
      return response.data
    })
  },
  getDepartment(id: number): Promise<Department> {
    return request.get<Department>(`/departments/${id}`).then((response) => {
      if (!response.data) {
        throw new Error('获取科室详情失败')
      }
      return response.data
    })
  },
  create(payload: { name: string; description?: string; sort_order: number }): Promise<Department> {
    return request.post<Department>('/departments', payload).then((response) => {
      if (!response.data) {
        throw new Error('创建科室失败')
      }
      return response.data
    })
  },
  update(id: number, payload: Partial<{ name: string; description?: string; sort_order: number; is_active: boolean }>): Promise<Department> {
    return request.put<Department>(`/departments/${id}`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新科室失败')
      }
      return response.data
    })
  },
  updateStatus(id: number, isActive: boolean): Promise<Department> {
    return request.patch<Department>(`/departments/${id}/status`, { is_active: isActive }).then((response) => {
      if (!response.data) {
        throw new Error('更新科室状态失败')
      }
      return response.data
    })
  },
  remove(id: number): Promise<void> {
    return request.delete(`/departments/${id}`).then(() => undefined)
  }
}
