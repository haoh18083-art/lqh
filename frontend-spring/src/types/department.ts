import type { PageResponse } from './api'

export interface Department {
  id: number
  name: string
  description?: string
  is_active: boolean
  sort_order: number
  created_at: string
  updated_at: string
}

export type DepartmentListResponse = PageResponse<Department>
