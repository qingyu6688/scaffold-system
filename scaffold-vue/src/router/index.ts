import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setUnauthorizedHandler } from '@/api'
import { authed, clearSession, dynamicRoutes, loadRoutes, navGroups, refreshUser, sessionVersion, setSessionRouteReset } from '@/composables/auth'
import { pushToast } from '@/composables/toast'
import { APP } from '@/data'
import AppShell from '@/components/layout/AppShell.vue'

const staticRoutes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    name: 'root',
    component: AppShell,
    children: [
      { path: 'no-access', name: 'no-access', component: () => import('@/views/NoAccess.vue'), meta: { title: '暂未分配菜单' } },
      // 个人中心属于每个已登录账号，不走 sys_menu 授权，未分配菜单的账号也能进入
      { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
      { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
})

let routesReady = false
let routeLoad: Promise<boolean> | null = null
let cancelRouteLoad: (() => void) | null = null
const removeRoutes: Array<() => void> = []

/** 登录后的落点：有工作台权限去工作台，否则去第一个可见菜单 */
export function homePath() {
  return navGroups.value[0]?.items[0]?.path ?? '/no-access'
}

export function resetRoutes() {
  removeRoutes.splice(0).forEach(remove => remove())
  routesReady = false
  cancelRouteLoad?.()
  cancelRouteLoad = null
  routeLoad = null
}
setSessionRouteReset(resetRoutes)

async function ensureRoutes() {
  if (routesReady) return true
  if (routeLoad) return routeLoad
  const version = sessionVersion.value
  const cancelled = new Promise<boolean>(resolve => { cancelRouteLoad = () => resolve(false) })
  const task = Promise.race([cancelled, (async () => {
    await Promise.all([loadRoutes(), refreshUser()])
    if (version !== sessionVersion.value || !authed.value) return false
    dynamicRoutes.value.forEach(route => removeRoutes.push(router.addRoute('root', route)))
    routesReady = true
    return true
  })()])
  routeLoad = task
  try {
    return await task
  } finally {
    if (routeLoad === task) {
      routeLoad = null
      cancelRouteLoad = null
    }
  }
}

/**
 * 本人账号在后台被本人修改后调用：不清会话，以服务端为准刷新当前用户、权限与菜单，并重建动态路由。
 * 角色变化后当前页面若已不在授权范围内，回到新的首页。
 */
export async function syncSessionRoutes() {
  if (!routesReady) {
    // 路由仍在初始化，守卫完成后自然是最新数据，这里只刷新用户
    await refreshUser()
    return
  }
  const version = sessionVersion.value
  await Promise.all([loadRoutes(), refreshUser()])
  if (version !== sessionVersion.value || !authed.value) return
  removeRoutes.splice(0).forEach(remove => remove())
  dynamicRoutes.value.forEach(route => removeRoutes.push(router.addRoute('root', route)))
  const current = router.currentRoute.value.name
  if (current && !router.hasRoute(current)) await router.replace(homePath())
}

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return authed.value && to.name === 'login' ? { path: '/' } : true
  }
  if (!authed.value) {
    return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined }
  }
  if (!routesReady) {
    const version = sessionVersion.value
    try {
      if (!await ensureRoutes()) return false
    } catch {
      if (version !== sessionVersion.value) return false
      clearSession()
      pushToast('菜单加载失败，请重新登录', 'danger')
      return { name: 'login' }
    }
    // 动态路由刚注册：按原地址重新解析，保留查询参数（深链如 ?trace=user:1）
    return to.path === '/' ? { path: homePath(), replace: true } : { path: to.path, query: to.query, hash: to.hash, replace: true }
  }
  if (to.path === '/') return { path: homePath(), replace: true }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · ${APP.name}` : APP.name
})

// 任一接口返回 401：清理登录态并回到登录页
let redirecting = false
setUnauthorizedHandler(() => {
  if (redirecting || !authed.value) return
  redirecting = true
  clearSession()
  pushToast('登录已过期，请重新登录', 'info')
  router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } }).finally(() => { redirecting = false })
})

export default router
