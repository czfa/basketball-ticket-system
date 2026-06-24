<template>
  <div class="admin-order-management-container">
    <div class="admin-header">
      <div class="header-left">
        <button class="back-btn" @click="goToAdminDashboard">← 返回管理后台首页</button>
        <h1 class="page-title">订单管理与统计</h1>
      </div>
      <div class="header-actions">
        <select v-model="statusFilter" @change="loadOrders" class="status-select">
          <option value="">全部订单</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="CANCELLED">已取消</option>
          <option value="REFUNDED">已退款</option>
          <option value="EXPIRED">已过期</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <div v-else class="order-management-content">
      <!-- 统计卡片 -->
      <div class="stats-section">
        <h2>订单统计</h2>
        <div class="stats-grid">
          <div class="stat-card">
            <h3>总订单数</h3>
            <p class="stat-value">{{ statistics.totalOrders || 0 }}</p>
          </div>
          <div class="stat-card">
            <h3>已支付订单</h3>
            <p class="stat-value">{{ statistics.paidOrders || 0 }}</p>
          </div>
          <div class="stat-card">
            <h3>待支付订单</h3>
            <p class="stat-value">{{ statistics.pendingOrders || 0 }}</p>
          </div>
          <div class="stat-card">
            <h3>总销售额</h3>
            <p class="stat-value price">¥{{ formatPrice(statistics.totalSales) }}</p>
          </div>
          <div class="stat-card">
            <h3>总票数</h3>
            <p class="stat-value">{{ statistics.totalTickets || 0 }}</p>
          </div>
        </div>
      </div>

      <!-- 订单列表 -->
      <div class="orders-section">
        <h2>订单列表</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>用户ID</th>
                <th>赛事</th>
                <th>座位数</th>
                <th>订单金额</th>
                <th>下单时间</th>
                <th>支付时间</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orders" :key="order.orderId">
                <td>{{ order.orderNumber }}</td>
                <td>{{ order.userId }}</td>
                <td>{{ order.matchName || '未知赛事' }}</td>
                <td>{{ order.seatCount }}</td>
                <td class="price">¥{{ formatPrice(order.totalAmount) }}</td>
                <td>{{ formatDateTime(order.orderTime) }}</td>
                <td>{{ order.payTime ? formatDateTime(order.payTime) : '-' }}</td>
                <td>
                  <span class="status-badge" :class="getStatusClass(order.status)">
                    {{ getStatusText(order.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="orders.length === 0" class="empty-state">
          <p>暂无订单数据</p>
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
  name: 'AdminOrderManagement',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const orders = ref([])
    const statistics = ref({})
    const statusFilter = ref('')

    /**
     * 返回管理后台首页
     */
    const goToAdminDashboard = () => {
      router.push({ name: 'AdminDashboard' })
    }

    /**
     * 加载订单列表
     */
    const loadOrders = async () => {
      loading.value = true
      try {
        const response = await adminAPI.getAllOrders(statusFilter.value || null)
        if (response.success) {
          orders.value = response.data || []
        } else {
          alert(response.message || '获取订单列表失败')
        }
      } catch (error) {
        alert(error.message || '获取订单列表失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 加载统计数据
     */
    const loadStatistics = async () => {
      try {
        const response = await adminAPI.getOrderStatistics()
        if (response.success) {
          statistics.value = response.data || {}
        }
      } catch (error) {
        console.error('获取统计数据失败:', error)
      }
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
      if (!price) return '0.00'
      return Number(price).toFixed(2)
    }

    /**
     * 获取状态文本
     */
    const getStatusText = (status) => {
      const statusMap = {
        'PENDING': '待支付',
        'PAID': '已支付',
        'CANCELLED': '已取消',
        'REFUNDED': '已退款',
        'EXPIRED': '已过期'
      }
      return statusMap[status] || status
    }

    /**
     * 获取状态样式类
     */
    const getStatusClass = (status) => {
      const statusMap = {
        'PENDING': 'status-pending',
        'PAID': 'status-paid',
        'CANCELLED': 'status-cancelled',
        'REFUNDED': 'status-refunded',
        'EXPIRED': 'status-expired'
      }
      return statusMap[status] || ''
    }

    onMounted(() => {
      loadOrders()
      loadStatistics()
    })

    return {
      loading,
      orders,
      statistics,
      statusFilter,
      goToAdminDashboard,
      loadOrders,
      formatDateTime,
      formatPrice,
      getStatusText,
      getStatusClass
    }
  }
}
</script>

<style scoped>
.admin-order-management-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.back-btn {
  background: none;
  border: none;
  color: #409eff;
  font-size: 16px;
  cursor: pointer;
  padding: 10px 0;
  transition: color 0.3s;
}

.back-btn:hover {
  color: #66b1ff;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.status-select {
  padding: 8px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.loading-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.order-management-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-section,
.orders-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stats-section h2,
.orders-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-card h3 {
  font-size: 14px;
  color: #606266;
  margin: 0 0 10px 0;
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.stat-value.price {
  color: #f56c6c;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.data-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #606266;
}

.data-table td.price {
  color: #f56c6c;
  font-weight: 600;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: #fef0f0;
  color: #e6a23c;
}

.status-paid {
  background: #f0f9ff;
  color: #67c23a;
}

.status-cancelled,
.status-expired {
  background: #fef0f0;
  color: #f56c6c;
}

.status-refunded {
  background: #ecf5ff;
  color: #409eff;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

@media (max-width: 768px) {
  .data-table {
    font-size: 12px;
  }

  .data-table th,
  .data-table td {
    padding: 8px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
