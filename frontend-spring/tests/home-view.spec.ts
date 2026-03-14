import { mount } from '@vue/test-utils'
import HomeView from '@/views/HomeView.vue'

describe('HomeView', () => {
  it('renders the new separated Vue portal entry', () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: ['RouterLink']
      }
    })

    expect(wrapper.text()).toContain('校园医疗前端已切到独立 Vue 工作台')
    expect(wrapper.text()).toContain('学生工作台')
    expect(wrapper.text()).toContain('医生工作台')
    expect(wrapper.text()).toContain('管理工作台')
    expect(wrapper.text()).toContain('backend-spring')
    expect(wrapper.text()).toContain('langgraph-app')
  })
})
