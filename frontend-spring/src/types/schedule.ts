import type { PageResponse } from './api'

export type ScheduleStatus = 'open' | 'closed'

export interface DoctorScheduleItem {
  id: number
  doctor_id: number
  date: string
  time_slot: string
  capacity: number
  booked_count: number
  status: ScheduleStatus
  created_at: string
  updated_at: string
}

export type DoctorScheduleListResponse = PageResponse<DoctorScheduleItem>
