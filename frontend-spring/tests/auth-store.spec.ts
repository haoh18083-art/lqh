import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('auth store skeleton', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('hydrates persisted login state from localStorage', () => {
    localStorage.setItem('access_token', 'access-token')
    localStorage.setItem('refresh_token', 'refresh-token')
    localStorage.setItem(
      'user_info',
      JSON.stringify({
        id: 1,
        username: 'reg_admin',
        role: 'admin',
        full_name: 'Regression Admin'
      })
    )

    const store = useAuthStore()
    store.hydrate()

    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.role).toBe('admin')
    expect(store.accessToken).toBe('access-token')
  })
})
