export type UserRole = 'student' | 'doctor' | 'admin'

export interface UserInfo {
  id: number
  username: string
  role: UserRole
  email?: string
  full_name?: string
  phone?: string
  student_id?: string
}

export interface LoginRequest {
  email: string
  password: string
  role?: UserRole
}

export interface LoginResponse {
  access_token: string
  refresh_token: string
  user: UserInfo
}
