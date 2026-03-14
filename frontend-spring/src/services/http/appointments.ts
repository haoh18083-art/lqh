import apiClient, { request } from './apiClient'
import type {
  AppointmentDocumentListResponse,
  AppointmentListResponse,
  AppointmentResponse,
  AppointmentSlotResponse
} from '@/types/appointment'

export const appointmentsService = {
  getSlots(params: { doctor_id: number; date_from: string; date_to: string }): Promise<AppointmentSlotResponse[]> {
    return request.get<AppointmentSlotResponse[]>('/appointments/slots', { params }).then((response) => response.data || [])
  },
  create(payload: { doctor_id: number; date: string; time: string; symptoms?: string }): Promise<AppointmentResponse> {
    return request.post<AppointmentResponse>('/appointments', payload).then((response) => {
      if (!response.data) {
        throw new Error('创建预约失败')
      }
      return response.data
    })
  },
  mine(params?: Record<string, unknown>): Promise<AppointmentListResponse> {
    return request.get<AppointmentListResponse>('/appointments/mine', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取预约列表失败')
      }
      return response.data
    })
  },
  cancel(appointmentId: number, reason: string): Promise<AppointmentResponse> {
    return request.post<AppointmentResponse>(`/appointments/${appointmentId}/cancel`, { reason }).then((response) => {
      if (!response.data) {
        throw new Error('取消预约失败')
      }
      return response.data
    })
  },
  reschedule(appointmentId: number, payload: { new_date: string; new_time: string; reason: string }) {
    return request.post(`/appointments/${appointmentId}/reschedule`, payload).then((response) => response.data)
  },
  detail(appointmentId: number): Promise<AppointmentResponse> {
    return request.get<AppointmentResponse>(`/appointments/${appointmentId}`).then((response) => {
      if (!response.data) {
        throw new Error('获取预约详情失败')
      }
      return response.data
    })
  },
  documents(appointmentId: number): Promise<AppointmentDocumentListResponse> {
    return request.get<AppointmentDocumentListResponse>(`/appointments/${appointmentId}/documents`).then((response) => {
      if (!response.data) {
        throw new Error('获取文档列表失败')
      }
      return response.data
    })
  },
  download(appointmentId: number, docType: 'diagnosis' | 'prescription'): Promise<Blob> {
    return apiClient
      .get<Blob>(`/appointments/${appointmentId}/documents/download`, {
        params: { doc_type: docType },
        responseType: 'blob'
      })
      .then((response) => response.data)
  }
}
