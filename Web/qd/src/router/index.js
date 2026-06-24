/**
 * Vue Router 路由配置
 * 包含用户端和管理员端的完整路由结构
 */
import { createRouter, createWebHashHistory } from 'vue-router'

// 路由懒加载：按需加载页面组件
const routes = [
  // ==================== 公共页面 ====================
  {
    path: '/',
    name: 'Home',
    redirect: '/matches' // 默认跳转到赛事列表页
  },
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/user/UserLogin.vue'),
    meta: { title: '用户登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/user/UserRegister.vue'),
    meta: { title: '用户注册', requiresAuth: false }
  },

  // ==================== 用户端页面 ====================
  {
    path: '/matches',
    name: 'MatchList',
    component: () => import('@/views/user/MatchList.vue'),
    meta: { title: '赛事列表', requiresAuth: false }
  },
  {
    path: '/matches/:matchId',
    name: 'MatchDetail',
    component: () => import('@/views/user/MatchDetail.vue'),
    meta: { title: '赛事详情', requiresAuth: false }
  },
  {
    path: '/matches/:matchId/seats',
    name: 'SeatSelection',
    component: () => import('@/views/user/SeatSelection.vue'),
    meta: { title: '在线选座', requiresAuth: true }
  },
  {
    path: '/orders/confirm',
    name: 'OrderConfirm',
    component: () => import('@/views/user/OrderConfirm.vue'),
    meta: { title: '订单确认', requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'MyOrders',
    component: () => import('@/views/user/MyOrders.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/orders/:orderId',
    name: 'OrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/UserProfile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },

  // ==================== 管理员端页面 ====================
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { title: '管理员登录', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboard.vue'),
    meta: { title: '管理后台首页', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/matches',
    name: 'AdminMatchManagement',
    component: () => import('@/views/admin/AdminMatchManagement.vue'),
    meta: { title: '赛事管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/seats',
    name: 'AdminSeatManagement',
    component: () => import('@/views/admin/AdminSeatManagement.vue'),
    meta: { title: '座位管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrderManagement',
    component: () => import('@/views/admin/AdminOrderManagement.vue'),
    meta: { title: '订单管理与统计', requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHashHistory('/admin'),
  routes
})

/**
 * 路由守卫：检查用户登录状态和角色权限
 */
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 篮球票务系统`
  }

  // 获取用户信息（从 localStorage）
  // 更严格的检查：确保用户信息存在且有效（有 userId 或 username）
  const userInfoStr = localStorage.getItem('user_info')
  let userInfo = null
  try {
    if (userInfoStr) {
      const parsed = JSON.parse(userInfoStr)
      // 检查是否是有效用户对象（不是 null，且包含必要的用户标识）
      // 支持 userId 或 user_id 字段名
      if (parsed && (parsed.userId || parsed.user_id || parsed.username)) {
        userInfo = parsed
      } else {
        console.warn('user_info 格式无效，缺少 userId/user_id 或 username:', parsed)
      }
    }
  } catch (e) {
    // 解析失败，清除无效数据
    console.warn('无效的用户信息，已清除:', e)
    localStorage.removeItem('user_info')
  }

  // 获取管理员信息（从 localStorage）
  const adminInfoStr = localStorage.getItem('admin_info')
  let adminInfo = null
  try {
    if (adminInfoStr) {
      const parsed = JSON.parse(adminInfoStr)
      // 检查是否是有效管理员对象（不是 null，且包含必要的管理员标识）
      if (parsed && (parsed.adminId || parsed.username)) {
        adminInfo = parsed
      }
    }
  } catch (e) {
    // 解析失败，清除无效数据
    console.warn('无效的管理员信息，已清除:', e)
    localStorage.removeItem('admin_info')
  }

  // 检查是否需要管理员权限
  if (to.meta.requiresAdmin) {
    if (!adminInfo) {
      // 未登录或不是管理员，跳转到管理员登录页
      next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
      return
    }
    next()
    return
  }

  // 检查是否需要用户登录
  if (to.meta.requiresAuth) {
    if (!userInfo) {
      // 未登录，跳转到用户登录页，并记录原本要访问的页面
      next({ name: 'UserLogin', query: { redirect: to.fullPath } })
      return
    }
  }

  // 已登录用户访问登录/注册页，跳转到首页
  if ((to.name === 'UserLogin' || to.name === 'UserRegister') && userInfo) {
    next({ name: 'MatchList' })
    return
  }

  // 已登录管理员访问管理员登录页，跳转到管理后台
  if (to.name === 'AdminLogin' && adminInfo) {
    next({ name: 'AdminDashboard' })
    return
  }

  next()
})

export default router
