import type { HttpClient } from './http'

export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaUuid: string
}

export interface CaptchaResponse {
  uuid: string
  image: string
}

export interface LoginResponse {
  token: string
  expiresIn: number
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface CurrentUser {
  id?: number
  username: string
  nickname: string
  deptId?: number
  deptName?: string
  roles: string[]
  permissions: string[]
}

/** 个人中心：登录账号、部门、角色、岗位只读，姓名与联系方式可由本人维护 */
export interface ProfileLogin {
  time: string
  /** 1 成功，0 失败 */
  status: number
  msg?: string
}

export interface Profile {
  userId: number
  username: string
  nickname: string
  phone?: string | null
  email?: string | null
  deptName?: string | null
  roles: string[]
  posts: string[]
  lastLoginTime?: string | null
  createTime?: string | null
  recentLogins: ProfileLogin[]
}

export interface ProfileUpdateRequest {
  nickname: string
  phone: string
  email: string
}

export interface RouteItem {
  id?: number
  parentId?: number
  name: string
  routeName?: string
  routePath?: string
  path?: string
  component?: string
  icon?: string
  permission?: string
  children?: RouteItem[]
}

export interface RoutesResponse {
  routes: RouteItem[]
}

export function createAuthApi(client: HttpClient) {
  return {
    captcha: () => client.get<CaptchaResponse>('/auth/captcha'),
    login: (body: LoginRequest) => client.post<LoginResponse>('/auth/login', body),
    me: () => client.get<CurrentUser>('/auth/me'),
    routes: () => client.get<RoutesResponse>('/auth/routes'),
    changePassword: (body: ChangePasswordRequest) => client.put<void>('/auth/password', body),
    profile: () => client.get<Profile>('/profile'),
    updateProfile: (body: ProfileUpdateRequest) => client.put<Profile>('/profile', body),
    logout: () => client.post<void>('/auth/logout'),
  }
}
