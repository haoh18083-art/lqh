import { tokenManager } from '../http/session'
import type { PharmacyMedicine } from '@/types/agent'

const baseURL = import.meta.env.VITE_AGENT_API_BASE_URL || '/agent-api/api/v1'

const authHeaders = (): HeadersInit => ({
  'Content-Type': 'application/json',
  ...(tokenManager.getToken() ? { Authorization: `Bearer ${tokenManager.getToken()}` } : {})
})

const extractErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  try {
    const payload = await response.json()
    if (typeof payload?.detail === 'string') {
      return payload.detail
    }
    if (Array.isArray(payload?.detail) && payload.detail.length > 0) {
      return payload.detail.map((item: { msg?: string }) => item.msg || '请求参数无效').join('; ')
    }
  } catch {
    // Ignore malformed JSON and use fallback message.
  }
  return fallback
}

export interface PharmacyOrderItem {
  id: number
  medicine_id?: number | null
  medicine_name: string
  specification?: string | null
  quantity: number
  unit?: string | null
  price: number
}

export interface PharmacyOrder {
  id: number
  order_no: string
  purchase_date: string
  total_amount: number
  status: 'completed' | 'pending' | 'cancelled'
  items: PharmacyOrderItem[]
}

export const pharmacyService = {
  async listMedicines(params?: Record<string, string | number>) {
    const query = new URLSearchParams()
    Object.entries(params || {}).forEach(([key, value]) => query.set(key, String(value)))
    const response = await fetch(`${baseURL}/pharmacy/medicines?${query.toString()}`, {
      headers: authHeaders()
    })
    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '获取药房药品失败'))
    }
    return response.json() as Promise<{ items: PharmacyMedicine[]; total: number; page: number; page_size: number; total_pages: number }>
  },
  async createOrder(items: Array<{ medicine_id: number; quantity: number }>): Promise<PharmacyOrder> {
    const response = await fetch(`${baseURL}/pharmacy/orders`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ items })
    })
    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '创建购药订单失败'))
    }
    return response.json() as Promise<PharmacyOrder>
  },
  async listOrders(params?: Record<string, string | number>) {
    const query = new URLSearchParams()
    Object.entries(params || {}).forEach(([key, value]) => query.set(key, String(value)))
    const response = await fetch(`${baseURL}/pharmacy/orders?${query.toString()}`, {
      headers: authHeaders()
    })
    if (!response.ok) {
      throw new Error(await extractErrorMessage(response, '获取订单列表失败'))
    }
    return response.json() as Promise<{ items: PharmacyOrder[]; total: number; page: number; page_size: number; total_pages: number }>
  }
}
