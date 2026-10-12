import test from 'node:test'
import assert from 'node:assert/strict'
import { createHttpClient } from '../http.ts'
import { createAuthApi } from '../auth.ts'

test('auth api maps login, current user, routes and logout endpoints', async () => {
  const calls: Array<{ url: string; method: string; body?: string }> = []
  const fetcher = async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), method: init?.method ?? 'GET', body: init?.body as string | undefined })
    const data = url.toString().endsWith('/login')
      ? { token: 'jwt-token', expiresIn: 7200 }
      : url.toString().endsWith('/me')
        ? { username: 'admin', nickname: '周明远', roles: ['super_admin'], permissions: ['system:user:list'] }
        : url.toString().endsWith('/routes')
          ? { routes: [{ name: '工作台', path: '/dashboard' }] }
          : null
    return new Response(JSON.stringify({ code: 200, message: 'success', data, traceId: 't' }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    })
  }
  const api = createAuthApi(createHttpClient({ baseUrl: '/api', tokenProvider: () => 'token', fetcher }))

  const login = await api.login({ username: 'admin', password: 'admin123', captcha: '1234', captchaUuid: 'a'.repeat(32) })
  const me = await api.me()
  const routes = await api.routes()
  await api.changePassword({ oldPassword: 'oldPwd123', newPassword: 'newPwd123' })
  await api.logout()

  assert.equal(login.token, 'jwt-token')
  assert.equal(me.username, 'admin')
  assert.deepEqual(routes.routes, [{ name: '工作台', path: '/dashboard' }])
  assert.deepEqual(calls.map(call => `${call.method} ${call.url}`), [
    'POST /api/auth/login',
    'GET /api/auth/me',
    'GET /api/auth/routes',
    'PUT /api/auth/password',
    'POST /api/auth/logout',
  ])
  assert.deepEqual(JSON.parse(calls[0].body ?? '{}'), { username: 'admin', password: 'admin123', captcha: '1234', captchaUuid: 'a'.repeat(32) })
  assert.deepEqual(JSON.parse(calls[3].body ?? '{}'), { oldPassword: 'oldPwd123', newPassword: 'newPwd123' })
})

test('auth api maps own profile read and update endpoints', async () => {
  const calls: Array<{ url: string; method: string; body?: string }> = []
  const profile = { userId: 7, username: 'zhou', nickname: '周明远', phone: null, email: 'zhou@example.com', roles: ['普通员工'], posts: [], recentLogins: [] }
  const fetcher = async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), method: init?.method ?? 'GET', body: init?.body as string | undefined })
    return new Response(JSON.stringify({ code: 200, message: 'success', data: profile, traceId: 't' }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    })
  }
  const api = createAuthApi(createHttpClient({ baseUrl: '/api', tokenProvider: () => 'token', fetcher }))

  const loaded = await api.profile()
  const saved = await api.updateProfile({ nickname: '周明远', phone: '', email: 'zhou@example.com' })

  assert.equal(loaded.username, 'zhou')
  assert.equal(saved.email, 'zhou@example.com')
  assert.deepEqual(calls.map(call => `${call.method} ${call.url}`), ['GET /api/profile', 'PUT /api/profile'])
  assert.deepEqual(JSON.parse(calls[1].body ?? '{}'), { nickname: '周明远', phone: '', email: 'zhou@example.com' })
})
