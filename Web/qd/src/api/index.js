/**
 * API 接口配置文件
 * 统一管理所有后端接口地址，便于维护和修改
 */
import axios from 'axios'

// API 基础配置
const API_BASE_URL = 'http://localhost:8088'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // 允许携带 Cookie (Session)
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  config => {
    // 可以在这里添加 token 等认证信息
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  response => {
    // 统一处理响应数据
    return response.data
  },
  error => {
    // 统一处理错误
    if (error.response) {
      // 服务器返回了错误状态码
      const message = error.response.data?.message || '请求失败'
      console.error('API Error:', message)
      return Promise.reject(new Error(message))
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      return Promise.reject(new Error('网络连接失败，请检查网络'))
    } else {
      // 其他错误
      return Promise.reject(error)
    }
  }
)

/**
 * ==================== 用户认证接口 ====================
 */
export const authAPI = {
  // 用户注册
  register: (userData) => apiClient.post('/api/auth/register', userData),
  
  // 用户登录
  login: (username, password) => apiClient.post('/api/auth/login', { username, password }),
  
  // 用户登出
  logout: () => apiClient.post('/api/auth/logout'),
  
  // 获取用户信息
  getProfile: () => apiClient.get('/api/auth/profile'),
  
  // 更新用户信息
  updateProfile: (userData) => apiClient.put('/api/auth/profile', userData),
  
  // 修改密码
  changePassword: (oldPassword, newPassword) => 
    apiClient.post('/api/auth/change-password', { oldPassword, newPassword })
}

/**
 * ==================== 赛事接口（用户端）====================
 */
export const matchAPI = {
  // 获取赛事列表（分页）
  getMatchList: (pageNum = 1, pageSize = 10, status = null) => {
    const params = { pageNum, pageSize }
    if (status) params.status = status
    return apiClient.get('/api/matches/list', { params })
  },
  
  // 获取赛事详情
  getMatchDetail: (matchId) => apiClient.get(`/api/matches/${matchId}`),
  
  // 搜索赛事
  searchMatches: (keyword) => apiClient.get('/api/matches/search', { params: { keyword } }),
  
  // 获取热门赛事
  getPopularMatches: (limit = 5) => apiClient.get('/api/matches/popular', { params: { limit } })
}

/**
 * ==================== 座位接口（用户端）====================
 */
export const seatAPI = {
  // 获取某场赛事的座位列表
  getSeatsByMatch: (matchId) => apiClient.get(`/api/seats/match/${matchId}`),
  
  // 获取可用座位列表
  getAvailableSeats: (matchId) => apiClient.get(`/api/seats/match/${matchId}/available`),
  
  // 根据区域获取座位
  getSeatsByZone: (matchId, zone) => apiClient.get(`/api/seats/match/${matchId}/zone/${zone}`),
  
  // 根据座位类型获取座位
  getSeatsByType: (matchId, seatType) => apiClient.get(`/api/seats/match/${matchId}/type/${seatType}`),
  
  // 根据价格范围获取座位
  getSeatsByPriceRange: (matchId, minPrice, maxPrice) => 
    apiClient.get(`/api/seats/match/${matchId}/price-range`, { params: { minPrice, maxPrice } }),
  
  // 检查座位可用性
  checkAvailability: (seatIds) => apiClient.post('/api/seats/check-availability', { seatIds })
}

/**
 * ==================== 订单接口（用户端）====================
 */
export const orderAPI = {
  // 创建订单（直接创建并自动支付）
  createOrder: (matchId, seatIds, userId) => apiClient.post('/api/orders/create', { matchId, seatIds, userId }),
  
  // 支付订单
  payOrder: (orderId, payMethod, transactionId) => 
    apiClient.post(`/api/orders/${orderId}/pay`, { payMethod, transactionId }),
  
  // 取消订单
  cancelOrder: (orderId) => apiClient.post(`/api/orders/${orderId}/cancel`),
  
  // 获取用户订单列表
  getUserOrders: (status = null) => {
    const params = status ? { status } : {}
    // 从 localStorage 获取用户信息，添加到请求中
    try {
      const userInfoStr = localStorage.getItem('user_info')
      console.log('getUserOrders - userInfoStr:', userInfoStr)
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        console.log('getUserOrders - parsed userInfo:', userInfo)
        // 支持 userId 或 user_id 字段名
        const userId = userInfo.userId || userInfo.user_id
        if (userId) {
          params.userId = userId
          console.log('getUserOrders - 添加 userId 到请求参数:', userId)
        } else {
          console.warn('user_info 中没有找到 userId 或 user_id 字段:', userInfo)
        }
      } else {
        console.warn('localStorage 中没有 user_info')
      }
    } catch (error) {
      console.error('解析 user_info 失败:', error)
    }
    console.log('getUserOrders - 最终请求参数:', params)
    return apiClient.get('/api/orders/my-orders', { params })
  },
  
  // 获取订单详情
  getOrderDetail: (orderId) => {
    // 从 localStorage 获取用户信息，添加到请求参数中
    const params = {}
    try {
      const userInfoStr = localStorage.getItem('user_info')
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        // 支持 userId 或 user_id 字段名
        const userId = userInfo.userId || userInfo.user_id
        if (userId) {
          params.userId = userId
        } else {
          console.warn('user_info 中没有找到 userId 或 user_id 字段:', userInfo)
        }
      }
    } catch (error) {
      console.error('解析 user_info 失败:', error)
    }
    return apiClient.get(`/api/orders/${orderId}`, { params })
  },
  
  // 退票（退款）
  refundOrder: (orderId, refundReason) => 
    apiClient.post(`/api/orders/${orderId}/refund`, { refundReason })
}

/**
 * ==================== 管理员接口 ====================
 */
export const adminAPI = {
  // 管理员登录
  login: (username, password) => apiClient.post('/api/admin/login', { username, password }),
  
  // 管理员登出
  logout: () => apiClient.post('/api/admin/logout'),
  
  // ========== 赛事管理 ==========
  // 获取所有赛事（管理员）
  getAllMatches: () => apiClient.get('/api/admin/matches/list'),
  
  // 新增赛事
  addMatch: (matchData) => apiClient.post('/api/admin/matches', matchData),
  
  // 更新赛事
  updateMatch: (matchId, matchData) => apiClient.put(`/api/admin/matches/${matchId}`, matchData),
  
  // 删除赛事
  deleteMatch: (matchId) => apiClient.delete(`/api/admin/matches/${matchId}`),
  
  // 批量删除赛事
  batchDeleteMatches: (matchIds) => apiClient.post('/api/admin/matches/batch-delete', { matchIds }),
  
  // 更新赛事状态
  updateMatchStatus: (matchId, status) => apiClient.put(`/api/admin/matches/${matchId}/status`, { status }),
  
  // 更新可用座位数
  updateAvailableSeats: (matchId, availableSeats) => 
    apiClient.put(`/api/admin/matches/${matchId}/available-seats`, { availableSeats }),
  
  // 更新基础票价
  updateBasePrice: (matchId, basePrice) => 
    apiClient.put(`/api/admin/matches/${matchId}/base-price`, { basePrice }),
  
  // ========== 座位管理 ==========
  // 获取座位列表（管理员）
  getSeatsByMatch: (matchId) => apiClient.get(`/api/admin/seats/match/${matchId}`),
  
  // 批量生成座位
  generateSeats: (matchId, seatConfig) => {
    const requestBody = { matchId, ...seatConfig }
    return apiClient.post(`/api/admin/seats/generate`, requestBody)
  },
  
  // 更新座位信息
  updateSeat: (seatId, seatData) => apiClient.put(`/api/admin/seats/${seatId}`, seatData),
  
  // 删除座位
  deleteSeat: (seatId) => apiClient.delete(`/api/admin/seats/${seatId}`),
  
  // 批量删除座位
  batchDeleteSeats: (seatIds) => apiClient.post('/api/admin/seats/batch-delete', { seatIds }),
  
  // ========== 订单管理与统计 ==========
  // 获取所有订单（管理员）
  getAllOrders: (status = null) => {
    const params = status ? { status } : {}
    return apiClient.get('/api/admin/statistics/orders', { params })
  },
  
  // 获取订单统计（销售统计）
  getOrderStatistics: () => apiClient.get('/api/admin/statistics/sales'),
  
  // 获取票务统计
  getTicketStatistics: () => apiClient.get('/api/admin/statistics/tickets'),
  
  // 获取赛事统计
  getMatchStatistics: (matchId) => apiClient.get(`/api/admin/statistics/match/${matchId}`),
  
  // 获取基础数据统计（汇总）
  getSummaryStatistics: () => apiClient.get('/api/admin/statistics/summary')
}

export default apiClient
