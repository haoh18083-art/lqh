import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { LoginRequest, UserInfo, UserRole } from '@/types/auth'
import { authService } from '@/services/http/auth'
import { tokenManager, userManager } from '@/services/http/session'

const roleHomeMap: Record<string, string> = {
  student: '/student/dashboard',
  doctor: '/doctor/dashboard',
  admin: '/admin/dashboard'
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(userManager.getUserInfo())
  const accessToken = ref<string | null>(tokenManager.getToken())
  const refreshToken = ref<string | null>(tokenManager.getRefreshToken())

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  const hydrate = (): void => {
    user.value = userManager.getUserInfo()
    accessToken.value = tokenManager.getToken()
    refreshToken.value = tokenManager.getRefreshToken()
  }

  const login = async (payload: LoginRequest): Promise<string> => {
    const response = await authService.login(payload)
    tokenManager.setToken(response.access_token)
    tokenManager.setRefreshToken(response.refresh_token)
    userManager.setUserInfo(response.user)
    hydrate()
    return roleHomeMap[response.user.role] || '/'
  }

  const syncCurrentUser = async (): Promise<UserInfo | null> => {
    if (!tokenManager.getToken()) {
      return null
    }

    const currentUser = await authService.me()
    userManager.setUserInfo(currentUser as UserInfo)
    hydrate()
    return currentUser as UserInfo
  }

  const hasRole = (role: UserRole): boolean => user.value?.role === role

  const logout = async (): Promise<void> => {
    try {
      if (refreshToken.value) {
        await authService.logout(refreshToken.value)
      }
    } finally {
      tokenManager.removeToken()
      tokenManager.removeRefreshToken()
      userManager.removeUserInfo()
      hydrate()
    }
  }

  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated,
    hydrate,
    login,
    syncCurrentUser,
    hasRole,
    logout
  }
})
