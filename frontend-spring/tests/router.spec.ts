import router from '@/router'

describe('router skeleton', () => {
  it('keeps student routes aligned with the React frontend', () => {
    expect(router.resolve('/student/dashboard').name).toBe('student-dashboard')
    expect(router.resolve('/student/consultation').name).toBe('student-consultation')
    expect(router.resolve('/student/appointments').name).toBe('student-appointments')
  })

  it('keeps doctor and admin routes aligned with the React frontend', () => {
    expect(router.resolve('/doctor/dashboard').name).toBe('doctor-dashboard')
    expect(router.resolve('/doctor/schedule').name).toBe('doctor-schedule')
    expect(router.resolve('/admin/settings').name).toBe('admin-settings')
    expect(router.resolve('/admin/doctors').name).toBe('admin-doctors')
  })
})
