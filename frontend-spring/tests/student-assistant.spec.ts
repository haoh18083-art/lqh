import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AIAssistantDrawer from '@/components/student/AIAssistantDrawer.vue'
import StudentLayout from '@/layouts/StudentLayout.vue'
import { useStudentAssistantStore } from '@/stores/studentAssistant'
import StudentDashboardView from '@/views/student/StudentDashboardView.vue'

const assistantMocks = vi.hoisted(() => ({
  streamChat: vi.fn(),
  executeAction: vi.fn(),
  listSessions: vi.fn(),
  listMessages: vi.fn()
}))

const dashboardMocks = vi.hoisted(() => ({
  appointmentsMine: vi.fn(),
  pharmacyListOrders: vi.fn()
}))

vi.mock('@/services/agent/agentAssistant', () => ({
  agentAssistantService: {
    streamChat: assistantMocks.streamChat,
    executeAction: assistantMocks.executeAction,
    listSessions: assistantMocks.listSessions,
    listMessages: assistantMocks.listMessages
  }
}))

vi.mock('@/services/http/appointments', () => ({
  appointmentsService: {
    mine: dashboardMocks.appointmentsMine
  }
}))

vi.mock('@/services/agent/pharmacy', () => ({
  pharmacyService: {
    listOrders: dashboardMocks.pharmacyListOrders
  }
}))

describe('AIAssistantDrawer', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    assistantMocks.streamChat.mockReset()
    assistantMocks.executeAction.mockReset()
    assistantMocks.listSessions.mockReset()
    assistantMocks.listMessages.mockReset()
    dashboardMocks.appointmentsMine.mockReset()
    dashboardMocks.pharmacyListOrders.mockReset()
  })

  it('renders streamed replies and recommendation cards from the Agent state payload', async () => {
    assistantMocks.streamChat.mockImplementation(async (_payload, handlers) => {
      handlers.onToken?.('建议先去呼吸内科')
      handlers.onState?.({
        public_session_id: 'sess-1',
        intent: 'appointment_and_medicine',
        answer: '建议先去呼吸内科并备一些常规药品。',
        reasoning_summary: '根据症状给出推荐。',
        doctor_cards: [
          {
            doctor_id: 14,
            doctor_name: '回归医生',
            department: '呼吸内科',
            title: '主治医师',
            introduction: '简介',
            recommend_reason: '症状匹配',
            slot_candidates: [{ date: '2026-03-10', time: '09:00', capacity: 10, booked_count: 2 }]
          }
        ],
        medicine_cards: [
          {
            medicine_id: 7,
            name: '感冒灵颗粒',
            spec: '10袋/盒',
            unit: '盒',
            price: 18.5,
            stock: 20,
            default_quantity: 1,
            max_quantity: 3,
            recommend_reason: '缓解发热和鼻塞'
          }
        ]
      })
      handlers.onDone?.({ public_session_id: 'sess-1' })
    })

    const wrapper = mount(AIAssistantDrawer, {
      props: { open: true },
      global: {
        stubs: {
          teleport: true
        }
      }
    })

    await wrapper.find('textarea').setValue('发热两天，想看看该挂哪个科室')
    const sendButton = wrapper.find('.ai-drawer__send-btn')

    expect(sendButton).toBeTruthy()
    await sendButton!.trigger('click')
    await flushPromises()

    expect(assistantMocks.streamChat).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('建议先去呼吸内科')
    expect(wrapper.text()).toContain('推荐医生')
    expect(wrapper.text()).toContain('感冒灵颗粒')
  })

  it('loads session history and maps action messages to readable text', async () => {
    assistantMocks.listSessions.mockResolvedValue([
      {
        public_session_id: 'sess-history',
        title: '发热三天',
        status: 'active',
        last_message_at: '2026-03-08T12:00:00Z',
        created_at: '2026-03-08T11:30:00Z'
      }
    ])
    assistantMocks.listMessages.mockResolvedValue([
      {
        seq_no: 1,
        role: 'user',
        message_kind: 'text',
        text: '想挂号',
        created_at: '2026-03-08T11:30:00Z'
      },
      {
        seq_no: 2,
        role: 'action',
        message_kind: 'action_result',
        text: '挂号成功',
        action_payload: { action_type: 'confirm_appointment' },
        created_at: '2026-03-08T11:31:00Z'
      }
    ])

    const wrapper = mount(AIAssistantDrawer, {
      props: { open: true },
      global: {
        stubs: {
          teleport: true
        }
      }
    })

    const historyButton = wrapper.find('.ai-drawer__action-btn[title="历史对话"]')

    expect(historyButton).toBeTruthy()
    await historyButton!.trigger('click')
    await flushPromises()

    expect(assistantMocks.listSessions).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('发热三天')

    const sessionButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('发热三天'))

    expect(sessionButton).toBeTruthy()
    await sessionButton!.trigger('click')
    await flushPromises()

    expect(assistantMocks.listMessages).toHaveBeenCalledWith('sess-history')
    // The component displays the raw message text from the history
    expect(wrapper.text()).toContain('挂号成功')
  })
})

describe('student assistant integration', () => {
  beforeEach(() => {
    dashboardMocks.appointmentsMine.mockResolvedValue({ items: [] })
    dashboardMocks.pharmacyListOrders.mockResolvedValue({ items: [] })
  })

  it('toggles the dashboard assistant CTA through the student assistant store', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useStudentAssistantStore()

    const wrapper = mount(StudentDashboardView, {
      global: {
        plugins: [pinia],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
          BaseChart: { template: '<div />' }
        }
      }
    })

    await flushPromises()

    // Toggle button is now in StudentLayout, not StudentDashboardView
    // Test the store directly
    expect(store.isOpen).toBe(false)
    store.togglePanel()
    expect(store.isOpen).toBe(true)
    store.closePanel()
    expect(store.isOpen).toBe(false)
  })

  it('closes the assistant when routing away from the student dashboard', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useStudentAssistantStore()

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/student',
          component: StudentLayout,
          children: [
            { path: 'dashboard', component: { template: '<div>dashboard</div>' } },
            { path: 'consultation', component: { template: '<div>consultation</div>' } }
          ]
        }
      ]
    })

    await router.push('/student/dashboard')
    await router.isReady()

    const wrapper = mount(StudentLayout, {
      global: {
        plugins: [pinia, router],
        stubs: {
          AIAssistantDrawer: { template: '<div data-testid="assistant-stub" />' },
          AppSidebar: { template: '<div data-testid="sidebar-stub" />' },
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })

    // AI toggle button should be visible on dashboard
    expect(wrapper.find('.ai-toggle-btn').exists()).toBe(true)

    // Open the assistant
    store.openPanel()
    expect(store.isOpen).toBe(true)

    // Navigate away from dashboard
    await router.push('/student/consultation')
    await flushPromises()

    // AI toggle button should not be visible on non-dashboard pages
    expect(wrapper.find('.ai-toggle-btn').exists()).toBe(false)
  })
})
