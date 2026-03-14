import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/login', component: LoginView }]
})

describe('LoginView', () => {
  it('renders email/password login and auto-role messaging without role selector', async () => {
    setActivePinia(createPinia())
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('校园医疗管理系统')
    expect(wrapper.text()).toContain('欢迎回来')
    expect(wrapper.text()).toContain('请登录您的账号以继续')
    expect(wrapper.text()).toContain('记住账号')
    expect(wrapper.find('input[autocomplete="email"]').exists()).toBe(true)
    expect(wrapper.find('input[autocomplete="current-password"]').exists()).toBe(true)
  })
})
