import type { PageResponse } from './api'

export type AppointmentStatusType =
  | 'pending'
  | 'confirmed'
  | 'in_progress'
  | 'completed'
  | 'cancelled'
  | 'missed'
  | 'rescheduled'

export interface AppointmentSlotResponse {
  date: string
  time: string
  status: 'available' | 'full' | 'booked' | 'closed'
  booked_count: number
  capacity: number
}

export interface AppointmentStudentInfo {
  id: number
  student_id: string
  full_name: string
  gender?: string | null
  dob?: string | null
}

export interface AppointmentDoctorInfo {
  id: number
  full_name: string
  title: string
  department_id: number
  department_name: string
}

export interface AppointmentDepartmentInfo {
  id: number
  name: string
}

export interface AppointmentResponse {
  id: number
  date: string
  time: string
  status: AppointmentStatusType
  queue_no?: string | null
  symptoms?: string | null
  created_at: string
  confirmed_at?: string | null
  cancelled_at?: string | null
  completed_at?: string | null
  student: AppointmentStudentInfo
  doctor: AppointmentDoctorInfo
  department: AppointmentDepartmentInfo
}

export type AppointmentListResponse = PageResponse<AppointmentResponse>

export interface AppointmentDocumentInfo {
  id: number
  appointment_id: number
  doc_type: 'diagnosis' | 'prescription'
  file_name: string
  created_at: string
}

export interface AppointmentDocumentListResponse {
  items: AppointmentDocumentInfo[]
}
