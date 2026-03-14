import type { MedicalRecordListResponse } from '@/types/medicalRecord'
import { request } from './apiClient'

export const medicalRecordsService = {
  mine(params?: Record<string, unknown>): Promise<MedicalRecordListResponse> {
    return request.get<MedicalRecordListResponse>('/medical-records/mine', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取就诊记录失败')
      }
      return response.data
    })
  }
}
