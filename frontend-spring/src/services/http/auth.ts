import type { LoginRequest, LoginResponse } from '@/types/auth'
import { request } from './apiClient'

export const authService = {
  login(payload: LoginRequest): Promise<LoginResponse> {
    return request.post<LoginResponse>('/auth/login', payload).then((response) => {
      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '登录失败')
      }
      return response.data
    })
  },
  logout(refreshToken: string): Promise<void> {
    return request.post('/auth/logout', { refresh_token: refreshToken }).then(() => undefined)
  },
  me() {
    return request.get('/auth/me').then((response) => {
      if (!response.data) {
        throw new Error('获取当前用户失败')
      }
      return response.data
    })
  }
}
