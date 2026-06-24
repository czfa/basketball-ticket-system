<template>
  <div class="match-detail-container" v-if="match">
    <div class="match-detail-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1 class="match-title">{{ match.matchName }}</h1>
      <div v-if="userInfo" class="header-actions">
        <span class="username">{{ userInfo.username || '用户' }}</span>
        <button class="profile-btn" @click="goToProfile">个人中心</button>
        <button class="orders-btn" @click="goToMyOrders">我的订单</button>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
      <div v-else class="header-actions">
        <button class="login-btn" @click="goToLogin">登录</button>
      </div>
    </div>

    <div class="match-detail-content">
      <!-- 赛事基本信息 -->
      <div class="match-info-card">
        <h2>赛事信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">对阵双方：</span>
            <span class="info-value">{{ match.homeTeam }} VS {{ match.awayTeam }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">比赛时间：</span>
            <span class="info-value">{{ formatDateTime(match.matchTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">比赛场馆：</span>
            <span class="info-value">{{ match.venue }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">赛事状态：</span>
            <span class="info-value status-badge" :class="getStatusClass(match.status)">
              {{ getStatusText(match.status) }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">基础票价：</span>
            <span class="info-value price">¥{{ formatPrice(match.basePrice) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">座位情况：</span>
            <span class="info-value">
              可用 {{ match.availableSeats }} / 总共 {{ match.totalSeats }}
            </span>
          </div>
        </div>
        <div v-if="match.description" class="match-description">
          <h3>赛事描述</h3>
          <p>{{ match.description }}</p>
        </div>
      </div>

      <!-- 购票操作区 -->
      <div class="purchase-card">
        <div class="purchase-info">
          <div class="price-display">
            <span class="price-label">票价：</span>
            <span class="price-amount">¥{{ formatPrice(match.basePrice) }} 起</span>
          </div>
          <div class="seat-info">
            <span>剩余座位：{{ match.availableSeats }} 个</span>
          </div>
        </div>
        <button
          class="purchase-btn"
          :disabled="match.status !== 'UPCOMING' || match.availableSeats === 0"
          @click="goToSeatSelection"
        >
          {{ match.status === 'UPCOMING' && match.availableSeats > 0 ? '立即选座购票' : '暂不可售' }}
        </button>
      </div>
    </div>
  </div>
  <div v-else class="loading-container">
    <p v-if="loading">加载中...</p>
    <p v-else>赛事不存在</p>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { matchAPI, authAPI } from '@/api'

export default {
  name: 'MatchDetail',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const match = ref(null)
    const loading = ref(true)
    const userInfo = ref(null)

    /**
     * 加载赛事详情
     */
    const loadMatchDetail = async () => {
      const matchId = route.params.matchId
      if (!matchId) {
        loading.value = false
        return
      }

      try {
        const response = await matchAPI.getMatchDetail(matchId)
        if (response.success) {
          match.value = response.data
        } else {
          alert(response.message || '获取赛事详情失败')
        }
      } catch (error) {
        alert(error.message || '获取赛事详情失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 跳转到选座页面
     */
    const goToSeatSelection = () => {
      // 检查是否登录
      const userInfo = JSON.parse(localStorage.getItem('user_info') || 'null')
      if (!userInfo) {
        alert('请先登录')
        router.push({ name: 'UserLogin', query: { redirect: route.fullPath + '/seats' } })
        return
      }
      router.push({ name: 'SeatSelection', params: { matchId: route.params.matchId } })
    }

    /**
     * 返回上一页
     */
    const goBack = () => {
      router.back()
    }

    /**
     * 跳转到我的订单页面
     */
    const goToMyOrders = () => {
      router.push({ name: 'MyOrders' })
    }

    /**
     * 跳转到个人中心页面
     */
    const goToProfile = () => {
      router.push({ name: 'UserProfile' })
    }

    /**
     * 跳转到登录页面
     */
    const goToLogin = () => {
      router.push({ name: 'UserLogin', query: { redirect: route.fullPath } })
    }

    /**
     * 退出登录
     */
    const handleLogout = async () => {
      if (!confirm('确定要退出登录吗？')) {
        return
      }

      try {
        await authAPI.logout()
        localStorage.removeItem('user_info')
        userInfo.value = null
        alert('退出登录成功')
        router.push({ name: 'MatchList' })
      } catch (error) {
        console.error('退出登录失败:', error)
        localStorage.removeItem('user_info')
        userInfo.value = null
        alert('退出登录成功')
        router.push({ name: 'MatchList' })
      }
    }

    /**
     * 加载用户信息
     */
    const loadUserInfo = () => {
      try {
        const userInfoStr = localStorage.getItem('user_info')
        if (userInfoStr) {
          const parsed = JSON.parse(userInfoStr)
          if (parsed && (parsed.userId || parsed.user_id || parsed.username)) {
            userInfo.value = parsed
          }
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
        userInfo.value = null
      }
    }

    /**
     * 获取状态文本
     */
    const getStatusText = (status) => {
      const statusMap = {
        'UPCOMING': '即将开始',
        'ONGOING': '进行中',
        'FINISHED': '已结束',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status
    }

    /**
     * 获取状态样式类
     */
    const getStatusClass = (status) => {
      const statusMap = {
        'UPCOMING': 'status-upcoming',
        'ONGOING': 'status-ongoing',
        'FINISHED': 'status-finished',
        'CANCELLED': 'status-cancelled'
      }
      return statusMap[status] || ''
    }

    /**
     * 格式化日期时间
     */
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN')
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0'
      return Number(price).toFixed(2)
    }

    onMounted(() => {
      loadUserInfo()
      loadMatchDetail()
    })

    return {
      match,
      loading,
      loadMatchDetail,
      goToSeatSelection,
      goBack,
      goToMyOrders,
      goToProfile,
      goToLogin,
      handleLogout,
      userInfo,
      getStatusText,
      getStatusClass,
      formatDateTime,
      formatPrice
    }
  }
}
</script>

<style scoped>
.match-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.match-detail-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.username {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.match-detail-header .back-btn {
  background: none;
  border: none;
  color: #409eff;
  font-size: 16px;
  cursor: pointer;
  padding: 10px 0;
}

.match-detail-header .back-btn:hover {
  color: #66b1ff;
}

.match-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  flex: 1;
}

.profile-btn,
.orders-btn,
.login-btn,
.logout-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  white-space: nowrap;
}

.profile-btn {
  background: #409eff;
  color: white;
}

.profile-btn:hover {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.3);
}

.orders-btn {
  background: #67c23a;
  color: white;
}

.orders-btn:hover {
  background: #85ce61;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.3);
}

.login-btn {
  background: #409eff;
  color: white;
}

.login-btn:hover {
  background: #66b1ff;
}

.logout-btn {
  background: #f56c6c;
  color: white;
}

.logout-btn:hover {
  background: #f78989;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3);
}

.match-detail-content {
  max-width: 1200px;
  margin: 0 auto;
}

.match-info-card {
  background: white;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.match-info-card h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #303133;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-label {
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
}

.info-value {
  color: #303133;
}

.info-value.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 600;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
}

.status-upcoming {
  background: #f0f9ff;
  color: #67c23a;
}

.status-ongoing {
  background: #fef0f0;
  color: #e6a23c;
}

.status-finished {
  background: #f4f4f5;
  color: #909399;
}

.status-cancelled {
  background: #fef0f0;
  color: #f56c6c;
}

.match-description {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid #ebeef5;
}

.match-description h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #303133;
}

.match-description p {
  color: #606266;
  line-height: 1.8;
  margin: 0;
}

.purchase-card {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.purchase-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.price-display {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-label {
  color: #909399;
  font-size: 16px;
}

.price-amount {
  color: #f56c6c;
  font-size: 28px;
  font-weight: 600;
}

.seat-info {
  color: #606266;
  font-size: 14px;
}

.purchase-btn {
  padding: 15px 40px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.purchase-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.purchase-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  color: #606266;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .purchase-card {
    flex-direction: column;
    gap: 20px;
  }

  .purchase-btn {
    width: 100%;
  }
}
</style>
