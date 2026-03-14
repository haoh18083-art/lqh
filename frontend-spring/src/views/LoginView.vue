<template>
  <div class="login-page">
    <!-- 左侧图片区域 (70%) -->
    <div class="login-page__visual">
      <div class="login-page__image-wrapper">
        <img
          src="/images/campus-gate.jpg"
          alt="广州华立学院"
          class="login-page__image"
        />
        <div class="login-page__overlay">
          <div class="login-page__brand">
            <h1 class="login-page__title">校园医疗管理系统</h1>
            <p class="login-page__subtitle">Campus Medical Management System</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 (30%) -->
    <div class="login-page__form-section">
      <div class="login-page__form-wrapper">
        <div class="login-page__form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号以继续</p>
        </div>

        <form class="login-form" @submit.prevent="handleSubmit">
          <div class="field">
            <label class="field__label">账号</label>
            <div class="field__input-wrapper">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <input
                v-model.trim="email"
                class="field__input field__input--with-icon"
                type="email"
                autocomplete="email"
                placeholder="请输入邮箱"
              />
            </div>
          </div>

          <div class="field">
            <label class="field__label">密码</label>
            <div class="field__input-wrapper">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                v-model="password"
                class="field__input field__input--with-icon"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
            </div>
          </div>

          <div class="login-form__options">
            <label class="checkbox">
              <input v-model="rememberMe" type="checkbox" class="checkbox__input" />
              <span class="checkbox__checkmark"></span>
              <span class="checkbox__label">记住账号</span>
            </label>
          </div>

          <div v-if="errorMessage" class="login-form__error">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {{ errorMessage }}
          </div>

          <button class="login-form__submit" type="submit" :disabled="isSubmitting">
            <span v-if="isSubmitting" class="login-form__spinner"></span>
            <span v-else>登录系统</span>
          </button>
        </form>

        <div class="login-page__footer">
          <p>广州华立学院 · 校园医疗服务平台</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')
const isSubmitting = ref(false)

const handleSubmit = async (): Promise<void> => {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = '请输入邮箱和密码'
    return
  }

  isSubmitting.value = true
  try {
    const redirect = (route.query.redirect as string | undefined) || (await authStore.login({
      email: email.value,
      password: password.value
    }))

    if (rememberMe.value) {
      localStorage.setItem('remembered_email', email.value)
    } else {
      localStorage.removeItem('remembered_email')
    }

    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  const remembered = localStorage.getItem('remembered_email')
  if (remembered) {
    email.value = remembered
    rememberMe.value = true
  }
})
</script>

<style scoped>
/* 页面整体布局 */
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--color-white);
}

/* 左侧图片区域 - 70% */
.login-page__visual {
  flex: 0 0 70%;
  position: relative;
  overflow: hidden;
}

.login-page__image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.login-page__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.login-page__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(0, 0, 0, 0.4) 0%,
    rgba(0, 0, 0, 0.2) 60%,
    rgba(0, 0, 0, 0.1) 100%
  );
  display: flex;
  align-items: flex-end;
  padding: 48px 64px;
}

.login-page__brand {
  color: white;
}

.login-page__title {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.login-page__subtitle {
  font-size: 16px;
  font-weight: 400;
  opacity: 0.9;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

/* 右侧表单区域 - 30% */
.login-page__form-section {
  flex: 0 0 30%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px;
  background: var(--color-white);
}

.login-page__form-wrapper {
  width: 100%;
  max-width: 360px;
}

.login-page__form-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-page__form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.login-page__form-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.field__input-wrapper {
  position: relative;
}

.field__icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  color: var(--text-tertiary);
  pointer-events: none;
}

.field__input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  font-size: 15px;
  color: var(--text-primary);
  background: var(--fill-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.field__input--with-icon {
  padding-left: 48px;
}

.field__input::placeholder {
  color: var(--text-tertiary);
}

.field__input:hover {
  background: var(--fill-secondary);
}

.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-white);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

/* 选项区域 */
.login-form__options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.checkbox__input {
  display: none;
}

.checkbox__checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-color-strong);
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.checkbox__checkmark::after {
  content: '';
  width: 8px;
  height: 5px;
  border-left: 2px solid var(--color-white);
  border-bottom: 2px solid var(--color-white);
  transform: rotate(-45deg) translate(1px, -1px);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.checkbox__input:checked + .checkbox__checkmark {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.checkbox__input:checked + .checkbox__checkmark::after {
  opacity: 1;
}

.checkbox__label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 错误提示 */
.login-form__error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: var(--color-danger);
  background: var(--color-danger-soft);
  border-radius: 10px;
}

.login-form__error svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 提交按钮 */
.login-form__submit {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-white);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 102, 84, 0.3);
}

.login-form__submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 102, 84, 0.4);
}

.login-form__submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-form__submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-form__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--color-white);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 底部版权 */
.login-page__footer {
  margin-top: 48px;
  text-align: center;
}

.login-page__footer p {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .login-page__visual {
    flex: 0 0 50%;
  }

  .login-page__form-section {
    flex: 0 0 50%;
  }

  .login-page__overlay {
    padding: 32px 40px;
  }

  .login-page__title {
    font-size: 32px;
  }
}

@media (max-width: 768px) {
  .login-page__visual {
    display: none;
  }

  .login-page__form-section {
    flex: 1;
    padding: 32px 24px;
  }
}
</style>
