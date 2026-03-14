import type { MedicinePageResponse } from './medicine'
import type { DoctorAppointmentHistoryResponse, DoctorAppointmentListResponse } from '@/types/doctorAppointments'
import { request } from './apiClient'

export const doctorAppointmentsService = {
  list(params?: Record<string, unknown>): Promise<DoctorAppointmentListResponse> {
    return request.get<DoctorAppointmentListResponse>('/doctor/appointments', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取医生预约列表失败')
      }
      return response.data
    })
  },
  start(appointmentId: number) {
    return request.post(`/doctor/appointments/${appointmentId}/start`).then((response) => response.data)
  },
  submitDiagnosis(
    appointmentId: number,
    payload: {
      category?: string | null
      signs?: string | null
      conclusion: string
      advice?: string | null
      items?: Array<{ medicine_id: number; dosage?: string | null; quantity?: number }>
    }
  ) {
    return request.post(`/doctor/appointments/${appointmentId}/diagnosis`, payload).then((response) => response.data)
  },
  history(appointmentId: number, params?: Record<string, unknown>): Promise<DoctorAppointmentHistoryResponse> {
    return request.get<DoctorAppointmentHistoryResponse>(`/doctor/appointments/${appointmentId}/history`, { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取学生历史失败')
      }
      return response.data
    })
  },
  medicines(): Promise<MedicinePageResponse> {
    return request.get<MedicinePageResponse>('/medicines?page=1&page_size=200&is_active=true').then((response) => {
      if (!response.data) {
        throw new Error('获取药品列表失败')
      }
      return response.data
    })
  }
}
