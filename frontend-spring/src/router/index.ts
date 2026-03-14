import { createRouter, createWebHistory } from 'vue-router'
import type { UserRole } from '@/types/auth'
import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores/pinia'

const roleHomeMap: Record<UserRole, string> = {
  student: '/student/dashboard',
  doctor: '/doctor/dashboard',
  admin: '/admin/dashboard'
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/student',
      component: () => import('@/layouts/StudentLayout.vue'),
      meta: { requiresAuth: true, role: 'student' },
      children: [
        { path: '', redirect: '/student/dashboard' },
        { path: 'dashboard', name: 'student-dashboard', component: () => import('@/views/student/StudentDashboardView.vue') },
        { path: 'consultation', name: 'student-consultation', component: () => import('@/views/student/StudentConsultationView.vue') },
        { path: 'pharmacy', name: 'student-pharmacy', component: () => import('@/views/student/StudentPharmacyView.vue') },
        { path: 'appointments', name: 'student-appointments', component: () => import('@/views/student/StudentAppointmentsView.vue') },
        { path: 'medicine-records', name: 'student-medicine-records', component: () => import('@/views/student/StudentMedicineRecordsView.vue') }
      ]
    },
    {
      path: '/doctor',
      component: () => import('@/layouts/DoctorLayout.vue'),
      meta: { requiresAuth: true, role: 'doctor' },
      children: [
        { path: '', redirect: '/doctor/dashboard' },
        { path: 'dashboard', name: 'doctor-dashboard', component: () => import('@/views/doctor/DoctorDashboardView.vue') },
        { path: 'schedule', name: 'doctor-schedule', component: () => import('@/views/doctor/DoctorScheduleView.vue') }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboardView.vue') },
        { path: 'schedules', name: 'admin-schedules', component: () => import('@/views/admin/AdminSchedulesView.vue') },
        { path: 'doctors', name: 'admin-doctors', component: () => import('@/views/admin/AdminDoctorsView.vue') },
        { path: 'students', name: 'admin-students', component: () => import('@/views/admin/AdminStudentsView.vue') },
        { path: 'medicines', name: 'admin-medicines', component: () => import('@/views/admin/AdminMedicinesView.vue') },
        { path: 'settings', name: 'admin-settings', component: () => import('@/views/admin/AdminSettingsView.vue') }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)
  authStore.hydrate()

  const requiresAuth = Boolean(to.meta.requiresAuth)
  const requiredRole = to.meta.role as UserRole | undefined

  if (to.path === '/login' && authStore.isAuthenticated && authStore.user) {
    return roleHomeMap[authStore.user.role]
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (requiresAuth && requiredRole && authStore.user?.role !== requiredRole) {
    return authStore.user ? roleHomeMap[authStore.user.role] : '/login'
  }

  return true
})

export default router
