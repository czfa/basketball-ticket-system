<template>
  <div class="match-list-container">
    <!-- 页面头部：搜索栏和筛选 -->
    <div class="match-list-header">
      <div class="header-top">
        <h1 class="page-title">篮球赛事</h1>
        <div class="header-actions">
          <div v-if="userInfo" class="user-info">
            <span class="username">{{ userInfo.username || '用户' }}</span>
            <button class="profile-btn" @click="goToProfile">个人中心</button>
            <button class="orders-btn" @click="goToMyOrders">我的订单</button>
            <button class="logout-btn" @click="handleLogout">退出登录</button>
          </div>
          <div v-else class="guest-actions">
            <button class="login-btn" @click="goToLogin">登录</button>
          </div>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索赛事、球队、场馆"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
      </div>
      <div class="header-filter">
        <div class="filter-group">
          <label class="filter-label">
            <input type="radio" v-model="statusFilter" value="" @change="handleStatusFilterChange" />
            <span>全部</span>
          </label>
          <label class="filter-label">
            <input type="radio" v-model="statusFilter" value="UPCOMING" @change="handleStatusFilterChange" />
            <span>即将开始</span>
          </label>
          <label class="filter-label">
            <input type="radio" v-model="statusFilter" value="ONGOING" @change="handleStatusFilterChange" />
            <span>进行中</span>
          </label>
          <label class="filter-label">
            <input type="radio" v-model="statusFilter" value="FINISHED" @change="handleStatusFilterChange" />
            <span>已结束</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 赛事列表 -->
    <div class="match-list-content">
      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>
      <div v-else-if="matches.length === 0" class="empty-state">
        <p>暂无赛事数据</p>
      </div>
      <div v-else class="match-grid">
        <div
          v-for="match in matches"
          :key="match.matchId"
          class="match-card"
          @click="goToMatchDetail(match.matchId)"
        >
          <div class="match-card-header">
            <h3 class="match-name">{{ match.matchName }}</h3>
            <span class="status-tag" :class="getStatusTagClass(match.status)">
              {{ getStatusText(match.status) }}
            </span>
          </div>
          <div class="match-card-body">
            <div class="match-info-item">
              <span class="info-icon">⏰</span>
              <span>{{ formatDateTime(match.matchTime) }}</span>
            </div>
            <div class="match-info-item">
              <span class="info-icon">📍</span>
              <span>{{ match.venue }}</span>
            </div>
            <div class="match-info-item">
              <span class="info-icon">👥</span>
              <span>{{ match.homeTeam }} VS {{ match.awayTeam }}</span>
            </div>
            <div class="match-info-item">
              <span class="info-icon">🎫</span>
              <span>可用座位：{{ match.availableSeats }}/{{ match.totalSeats }}</span>
            </div>
          </div>
          <div class="match-card-footer">
            <div class="price-info">
              <span class="price-label">票价：</span>
              <span class="price-value">¥{{ formatPrice(match.basePrice) }} 起</span>
            </div>
            <button
              class="buy-btn"
              :disabled="match.status !== 'UPCOMING' || match.availableSeats === 0"
              @click.stop="goToSeatSelection(match.matchId)"
            >
              {{ match.status === 'UPCOMING' && match.availableSeats > 0 ? '立即购票' : '暂不可售' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页组件 -->
    <div class="match-list-pagination" v-if="total > 0">
      <div class="pagination-info">
        <span>共 {{ total }} 条，每页 {{ pageSize }} 条</span>
      </div>
      <div class="pagination-controls">
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="handlePageChange(currentPage - 1)"
        >
          上一页
        </button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button
          class="page-btn"
          :disabled="currentPage >= totalPages"
          @click="handlePageChange(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { matchAPI, authAPI } from '@/api'

export default {
  name: 'MatchList',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const matches = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const statusFilter = ref('')
    const searchKeyword = ref('')
    const userInfo = ref(null)

    // 计算总页数
    const totalPages = computed(() => {
      return total.value > 0 ? Math.ceil(total.value / pageSize.value) : 0
    })

    /**
     * 加载赛事列表
     */
    const loadMatchList = async () => {
      loading.value = true
      try {
        let response
        if (searchKeyword.value) {
          // 如果有搜索关键词，调用搜索接口
          response = await matchAPI.searchMatches(searchKeyword.value)
        } else {
          // 否则调用列表接口
          response = await matchAPI.getMatchList(currentPage.value, pageSize.value, statusFilter.value || null)
        }

        if (response.success) {
          matches.value = response.data || []
          total.value = response.total || 0
        } else {
          alert(response.message || '获取赛事列表失败')
        }
      } catch (error) {
        console.error('加载赛事列表失败:', error)
        alert(error.message || '获取赛事列表失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 处理状态筛选变化
     */
    const handleStatusFilterChange = () => {
      currentPage.value = 1
      loadMatchList()
    }

    /**
     * 处理搜索
     */
    const handleSearch = () => {
      currentPage.value = 1
      loadMatchList()
    }

    /**
     * 处理页码变化
     */
    const handlePageChange = (page) => {
      currentPage.value = page
      loadMatchList()
    }

    /**
     * 跳转到赛事详情页
     */
    const goToMatchDetail = (matchId) => {
      router.push({ name: 'MatchDetail', params: { matchId } })
    }

    /**
     * 跳转到选座页面
     */
    const goToSeatSelection = (matchId) => {
      // 检查是否登录
      const userInfo = JSON.parse(localStorage.getItem('user_info') || 'null')
      if (!userInfo) {
        alert('请先登录')
        router.push({ name: 'UserLogin', query: { redirect: `/matches/${matchId}/seats` } })
        return
      }
      router.push({ name: 'SeatSelection', params: { matchId } })
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
      router.push({ name: 'UserLogin', query: { redirect: '/matches' } })
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
        // 刷新页面或跳转到首页
        router.push({ name: 'MatchList' })
      } catch (error) {
        console.error('退出登录失败:', error)
        // 即使API调用失败，也清除本地存储
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
     * 获取状态标签样式类
     */
    const getStatusTagClass = (status) => {
      const statusMap = {
        'UPCOMING': 'status-upcoming',
        'ONGOING': 'status-ongoing',
        'FINISHED': 'status-finished',
        'CANCELLED': 'status-cancelled'
      }
      return statusMap[status] || 'status-default'
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
     * 格式化日期时间
     */
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0'
      return Number(price).toFixed(2)
    }

    // 页面加载时获取数据
    onMounted(() => {
      loadUserInfo()
      loadMatchList()
    })

    return {
      loading,
      matches,
      total,
      currentPage,
      pageSize,
      totalPages,
      statusFilter,
      searchKeyword,
      loadMatchList,
      handleStatusFilterChange,
      handleSearch,
      handlePageChange,
      goToMatchDetail,
      goToSeatSelection,
      goToMyOrders,
      goToProfile,
      goToLogin,
      handleLogout,
      userInfo,
      getStatusTagClass,
      getStatusText,
      formatDateTime,
      formatPrice
    }
  }
}
</script>

<style scoped>
.match-list-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.match-list-header {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.guest-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
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

.search-input {
  padding: 8px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  width: 300px;
}

.search-input:focus {
  outline: none;
  border-color: #409eff;
}

.search-btn {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn:hover {
  background: #66b1ff;
}

.header-filter {
  display: flex;
  gap: 10px;
}

.filter-group {
  display: flex;
  gap: 20px;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.filter-label input[type="radio"] {
  cursor: pointer;
}

.match-list-content {
  min-height: 400px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.match-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.match-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.match-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.match-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.match-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
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

.match-card-body {
  margin-bottom: 15px;
}

.match-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}

.info-icon {
  font-size: 16px;
}

.match-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.price-label {
  color: #909399;
  font-size: 14px;
}

.price-value {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 600;
}

.buy-btn {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.buy-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.buy-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.match-list-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.pagination-info {
  color: #606266;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.page-btn {
  padding: 6px 15px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  background: #f5f7fa;
  border-color: #409eff;
  color: #409eff;
}

.page-btn:disabled {
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
}

.page-info {
  color: #606266;
  font-size: 14px;
}
</style>
