import type { PageResponse } from './api'

export interface MedicalHistoryRecord {
  condition: string
  date: string
  notes?: string
}

export interface EmergencyContact {
  name: string
  phone: string
  relationship: string
}

export interface HealthProfile {
  user_id: number
  blood_type?: string
  last_checkup_date?: string
  allergies: string[]
  medical_history: MedicalHistoryRecord[]
  emergency_contact?: EmergencyContact
  created_at?: string
  updated_at?: string
}

export interface Student {
  id: number
  user_id: number
  student_id: string
  username: string
  email: string
  full_name: string
  major: string
  grade: string
  class_name?: string
  gender?: string
  dob?: string
  health_status: '良好' | '异常'
  phone?: string
  is_active: boolean
  created_at: string
}

export type StudentListResponse = PageResponse<Student>
