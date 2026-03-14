import type { MedicalRecordItem } from './medicalRecord'

export interface DoctorAppointmentItem {
  appointment_id: number
  queue_no?: string | null
  student_name: string
  gender?: string | null
  time_slot: string
  symptoms?: string | null
  created_at: string
}

export interface DoctorAppointmentListResponse {
  items: DoctorAppointmentItem[]
  total: number
}

export interface DoctorAppointmentHistoryStudent {
  id: number
  student_id: string
  full_name: string
  gender?: string | null
  dob?: string | null
}

export interface DoctorAppointmentHistoryRecord {
  condition: string
  date: string
  notes?: string | null
}

export interface DoctorAppointmentHistoryResponse {
  student: DoctorAppointmentHistoryStudent
  medical_history: DoctorAppointmentHistoryRecord[]
  medical_records: MedicalRecordItem[]
  total_records: number
  page: number
  page_size: number
  total_pages: number
}
