import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { tokenManager, userManager } from './session'

export interface RequestConfig extends AxiosRequestConfig {
  skipErrorHandler?: boolean
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const createAxiosInstance = (): AxiosInstance =>
  axios.create({
    baseURL,
    timeout: 15000,
    headers: {
      'Content-Type': 'application/json'
    }
  })

const apiClient = createAxiosInstance()

const generateRequestId = (): string => `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`

const toAppError = (error: AxiosError<ApiResponse>): Error => {
  if (error.response) {
    const errorMessage = error.response.data?.error?.message || `请求失败 (${error.response.status})`
    const appError = new Error(errorMessage)
    ;(appError as Error & { status?: number }).status = error.response.status
    return appError
  }

  if (error.request) {
    return new Error('网络错误，请检查网络连接')
  }

  return new Error(error.message || '请求失败')
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenManager.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Request-ID'] = generateRequestId()
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse>) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = tokenManager.getRefreshToken()

      if (refreshToken) {
        try {
          const response = await axios.post<ApiResponse<{ access_token: string; refresh_token?: string }>>(
            `${baseURL}/auth/refresh`,
            { refresh_token: refreshToken }
          )
          const accessToken = response.data.data?.access_token
          if (!accessToken) {
            throw new Error('刷新令牌失败')
          }

          tokenManager.setToken(accessToken)
          if (response.data.data?.refresh_token) {
            tokenManager.setRefreshToken(response.data.data.refresh_token)
          }
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return apiClient(originalRequest)
        } catch (refreshError) {
          tokenManager.removeToken()
          tokenManager.removeRefreshToken()
          userManager.removeUserInfo()
          window.location.assign('/login')
          return Promise.reject(refreshError)
        }
      }

      tokenManager.removeToken()
      tokenManager.removeRefreshToken()
      userManager.removeUserInfo()
      window.location.assign('/login')
    }

    return Promise.reject(toAppError(error))
  }
)

export const request = {
  get<T>(url: string, config?: RequestConfig): Promise<ApiResponse<T>> {
    return apiClient.get(url, config).then((response) => response.data)
  },
  post<T>(url: string, data?: unknown, config?: RequestConfig): Promise<ApiResponse<T>> {
    return apiClient.post(url, data, config).then((response) => response.data)
  },
  put<T>(url: string, data?: unknown, config?: RequestConfig): Promise<ApiResponse<T>> {
    return apiClient.put(url, data, config).then((response) => response.data)
  },
  patch<T>(url: string, data?: unknown, config?: RequestConfig): Promise<ApiResponse<T>> {
    return apiClient.patch(url, data, config).then((response) => response.data)
  },
  delete<T>(url: string, config?: RequestConfig): Promise<ApiResponse<T>> {
    return apiClient.delete(url, config).then((response) => response.data)
  }
}

export default apiClient
