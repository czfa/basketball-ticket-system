<template>
  <div class="admin-dashboard-container">
    <div class="admin-header">
      <h1 class="page-title">管理后台首页</h1>
      <div class="admin-actions">
        <span class="admin-name">{{ adminInfo?.username || '管理员' }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <div class="admin-nav">
      <router-link to="/admin" class="nav-item">数据概览</router-link>
      <router-link to="/admin/matches" class="nav-item">赛事管理</router-link>
      <router-link to="/admin/seats" class="nav-item">座位管理</router-link>
      <router-link to="/admin/orders" class="nav-item">订单管理</router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载数据中...</p>
    </div>

    <div v-else class="dashboard-content">
      <h2>数据概览</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <h3>总赛事数</h3>
          <p class="stat-value">{{ statistics.totalMatches || 0 }}</p>
        </div>
        <div class="stat-card">
          <h3>总订单数</h3>
          <p class="stat-value">{{ statistics.totalOrders || 0 }}</p>
        </div>
        <div class="stat-card">
          <h3>总销售额</h3>
          <p class="stat-value price">¥{{ formatPrice(statistics.totalSales) }}</p>
        </div>
        <div class="stat-card">
          <h3>已售票数</h3>
          <p class="stat-value">{{ statistics.totalTickets || 0 }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminAPI } from '@/api'

export default {
  name: 'AdminDashboard',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const statistics = ref({})
    const adminInfo = ref(JSON.parse(localStorage.getItem('admin_info') || 'null'))

    /**
     * 加载统计数据
     */
    const loadStatistics = async () => {
      loading.value = true
      try {
        const response = await adminAPI.getSummaryStatistics()
        if (response.success) {
          statistics.value = response.data || {}
        } else {
          alert(response.message || '获取统计数据失败')
        }
      } catch (error) {
        alert(error.message || '获取统计数据失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 退出登录
     */
    const handleLogout = async () => {
      try {
        await adminAPI.logout()
        localStorage.removeItem('admin_info')
        router.push({ name: 'AdminLogin' })
      } catch (error) {
        console.error('退出登录失败:', error)
        localStorage.removeItem('admin_info')
        router.push({ name: 'AdminLogin' })
      }
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0'
      return Number(price).toFixed(2)
    }

    onMounted(() => {
      loadStatistics()
    })

    return {
      loading,
      statistics,
      adminInfo,
      loadStatistics,
      handleLogout,
      formatPrice
    }
  }
}
</script>

<style scoped>
.admin-dashboard-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.admin-header {
  background: white;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.admin-name {
  color: #606266;
  font-weight: 600;
}

.logout-btn {
  padding: 8px 20px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-btn:hover {
  background: #f78989;
}

.admin-nav {
  background: white;
  padding: 0 30px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  gap: 30px;
}

.nav-item {
  padding: 15px 0;
  color: #606266;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.nav-item:hover,
.nav-item.router-link-active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.loading-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.dashboard-content {
  padding: 30px;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-content h2 {
  margin: 0 0 30px 0;
  font-size: 22px;
  color: #303133;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-card h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #909399;
  font-weight: 600;
}

.stat-value {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: #303133;
}

.stat-value.price {
  color: #f56c6c;
}
</style>
