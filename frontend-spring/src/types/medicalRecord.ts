import type { PageResponse } from './api'

export interface MedicalRecordDiagnosis {
  category?: string | null
  signs?: string | null
  conclusion: string
  advice?: string | null
}

export interface MedicalRecordPrescriptionItem {
  name: string
  dosage?: string | null
  quantity: number
  unit?: string | null
  unit_price: number
  total_price: number
}

export interface MedicalRecordItem {
  id: string
  appointment_id: number
  student_id: number
  doctor_id: number
  department_id: number
  visit_date: string
  time_slot: string
  symptoms?: string | null
  doctor_name: string
  department_name: string
  diagnosis_summary: MedicalRecordDiagnosis
  prescription_summary: MedicalRecordPrescriptionItem[]
  fee_total: number
  created_at: string
}

export type MedicalRecordListResponse = PageResponse<MedicalRecordItem>
