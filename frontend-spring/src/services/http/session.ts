import type { UserInfo } from '@/types/auth'

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_INFO_KEY = 'user_info'

export const tokenManager = {
  getToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  },
  setToken(token: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
  },
  removeToken(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  },
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },
  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },
  removeRefreshToken(): void {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export const userManager = {
  getUserInfo(): UserInfo | null {
    const raw = localStorage.getItem(USER_INFO_KEY)
    if (!raw) {
      return null
    }

    try {
      return JSON.parse(raw) as UserInfo
    } catch {
      return null
    }
  },
  setUserInfo(user: UserInfo): void {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
  },
  removeUserInfo(): void {
    localStorage.removeItem(USER_INFO_KEY)
  }
}
