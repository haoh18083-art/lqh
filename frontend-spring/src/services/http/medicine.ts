import { request } from './apiClient'

export interface MedicineResponse {
  id: number
  name: string
  spec?: string | null
  unit?: string | null
  stock: number
  is_active: boolean
  price: number
  created_at: string
  updated_at: string
}

export interface MedicinePageResponse {
  items: MedicineResponse[]
  total: number
  page: number
  page_size: number
  total_pages: number
}

export const medicineService = {
  list(params?: Record<string, unknown>): Promise<MedicinePageResponse> {
    return request.get<MedicinePageResponse>('/medicines', { params }).then((response) => {
      if (!response.data) {
        throw new Error('获取药品列表失败')
      }
      return response.data
    })
  },
  create(payload: {
    name: string
    spec?: string
    unit?: string
    stock: number
    is_active: boolean
    price: number
  }): Promise<MedicineResponse> {
    return request.post<MedicineResponse>('/medicines', payload).then((response) => {
      if (!response.data) {
        throw new Error('创建药品失败')
      }
      return response.data
    })
  },
  update(medicineId: number, payload: Partial<{
    name: string
    spec?: string
    unit?: string
    stock: number
    is_active: boolean
    price: number
  }>): Promise<MedicineResponse> {
    return request.patch<MedicineResponse>(`/medicines/${medicineId}`, payload).then((response) => {
      if (!response.data) {
        throw new Error('更新药品失败')
      }
      return response.data
    })
  },
  updateStock(medicineId: number, delta: number, reason?: string): Promise<MedicineResponse> {
    return request.patch<MedicineResponse>(`/medicines/${medicineId}/stock`, {
      delta,
      reason
    }).then((response) => {
      if (!response.data) {
        throw new Error('更新药品库存失败')
      }
      return response.data
    })
  }
}
