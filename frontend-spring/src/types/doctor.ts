import type { PageResponse } from './api'

export interface Doctor {
  id: number
  user_id: number
  doctor_id: string
  username: string
  email: string
  full_name: string
  department_id: number
  department: string
  title: string
  introduction: string
  phone?: string
  is_active: boolean
  created_at: string
}

export type DoctorListResponse = PageResponse<Doctor>
