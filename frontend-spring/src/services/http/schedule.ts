import type { DoctorScheduleItem, DoctorScheduleListResponse } from '@/types/schedule'
import { request } from './apiClient'

export const scheduleService = {
  mine(params?: Record<string, unknown>): Promise<DoctorScheduleListResponse> {
    return request.get<DoctorScheduleListResponse>('/doctor-schedules/mine', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取医生排班失败')
      }
      return response.data
    })
  },
  list(params?: Record<string, unknown>): Promise<DoctorScheduleListResponse> {
    return request.get<DoctorScheduleListResponse>('/doctor-schedules', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取排班列表失败')
      }
      return response.data
    })
  },
  bulkUpsert(payload: Record<string, unknown>) {
    return request.post('/doctor-schedules/bulk', payload).then((response) => response.data)
  },
  update(scheduleId: number, payload: Record<string, unknown>): Promise<DoctorScheduleItem> {
    return request.patch<DoctorScheduleItem>(`/doctor-schedules/${scheduleId}`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新排班失败')
      }
      return response.data
    })
  },
  create(payload: {
    doctor_id: number
    date: string
    time_slots: string[]
    capacity: number
  }) {
    return request.post('/doctor-schedules', payload).then((response) => response.data)
  }
}
