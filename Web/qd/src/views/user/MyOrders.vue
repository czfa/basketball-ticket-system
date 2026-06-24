<template>
  <div class="my-orders-container">
    <div class="my-orders-header">
      <div class="header-left">
        <button class="back-btn" @click="goToMatches">← 返回篮球赛事</button>
        <h1 class="page-title">我的订单</h1>
      </div>
      <div class="filter-bar">
        <label>订单状态：</label>
        <select v-model="statusFilter" @change="loadOrders">
          <option value="">全部</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
          <option value="REFUNDED">已退款</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载订单列表中...</p>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <p>暂无订单</p>
      <button class="go-matches-btn" @click="goToMatches">去购票</button>
    </div>

    <div v-else class="orders-list">
      <div
        v-for="order in orders"
        :key="order.orderId"
        class="order-card"
        @click="goToOrderDetail(order.orderId)"
      >
        <div class="order-header">
          <div class="order-info">
            <span class="order-number">订单号：{{ order.orderNumber }}</span>
            <span class="order-time">下单时间：{{ formatDateTime(order.orderTime) }}</span>
          </div>
          <span class="order-status" :class="getStatusClass(order.status)">
            {{ getStatusText(order.status) }}
          </span>
        </div>
        <div class="order-body">
          <div class="order-item">
            <span class="label">赛事：</span>
            <span class="value">{{ order.matchName || '未知赛事' }}</span>
          </div>
          <div class="order-item">
            <span class="label">座位数量：</span>
            <span class="value">{{ order.seatCount }} 个</span>
          </div>
          <div class="order-item">
            <span class="label">订单金额：</span>
            <span class="value price">¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <div v-if="order.expireTime" class="order-item">
            <span class="label">过期时间：</span>
            <span class="value" :class="{ 'expiring': isExpiringSoon(order.expireTime) }">
              {{ formatDateTime(order.expireTime) }}
            </span>
          </div>
        </div>
        <div class="order-footer">
          <button
            v-if="order.status === 'PENDING'"
            class="action-btn cancel-btn"
            @click.stop="cancelOrder(order.orderId)"
            :disabled="cancelling"
          >
            取消订单
          </button>
          <button
            v-if="order.status === 'PENDING'"
            class="action-btn pay-btn"
            @click.stop="payOrder(order.orderId)"
            :disabled="paying"
          >
            立即支付
          </button>
          <button
            v-if="order.status === 'PAID'"
            class="action-btn refund-btn"
            @click.stop="refundOrder(order.orderId)"
            :disabled="refunding"
          >
            申请退票
          </button>
          <button
            class="action-btn detail-btn"
            @click.stop="goToOrderDetail(order.orderId)"
          >
            查看详情
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderAPI } from '@/api'

export default {
  name: 'MyOrders',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const cancelling = ref(false)
    const paying = ref(false)
    const refunding = ref(false)
    const orders = ref([])
    const statusFilter = ref('')

    /**
     * 加载订单列表
     */
    const loadOrders = async () => {
      // 检查是否登录
      const userInfoStr = localStorage.getItem('user_info')
      if (!userInfoStr) {
        alert('请先登录')
        router.push({ name: 'UserLogin', query: { redirect: '/orders' } })
        return
      }

      // 验证用户信息格式
      try {
        const userInfo = JSON.parse(userInfoStr)
        const userId = userInfo.userId || userInfo.user_id
        if (!userId) {
          console.error('user_info 中没有 userId:', userInfo)
          alert('用户信息格式错误，请重新登录')
          localStorage.removeItem('user_info')
          router.push({ name: 'UserLogin', query: { redirect: '/orders' } })
          return
        }
      } catch (error) {
        console.error('解析 user_info 失败:', error)
        alert('用户信息格式错误，请重新登录')
        localStorage.removeItem('user_info')
        router.push({ name: 'UserLogin', query: { redirect: '/orders' } })
        return
      }

      loading.value = true
      try {
        const response = await orderAPI.getUserOrders(statusFilter.value || null)
        if (response.success) {
          orders.value = response.data || []
        } else {
          // 如果是未登录错误，跳转到登录页
          if (response.message && response.message.includes('未登录')) {
            alert('请先登录')
            router.push({ name: 'UserLogin', query: { redirect: '/orders' } })
          } else {
            alert(response.message || '获取订单列表失败')
          }
        }
      } catch (error) {
        console.error('加载订单列表错误:', error)
        // 如果是未登录错误，跳转到登录页
        if (error.message && error.message.includes('未登录')) {
          alert('请先登录')
          router.push({ name: 'UserLogin', query: { redirect: '/orders' } })
        } else {
          alert(error.message || '获取订单列表失败')
        }
      } finally {
        loading.value = false
      }
    }

    /**
     * 取消订单
     */
    const cancelOrder = async (orderId) => {
      if (!confirm('确定要取消此订单吗？')) {
        return
      }

      cancelling.value = true
      try {
        const response = await orderAPI.cancelOrder(orderId)
        if (response.success) {
          alert('订单已取消')
          loadOrders() // 重新加载订单列表
        } else {
          alert(response.message || '取消订单失败')
        }
      } catch (error) {
        alert(error.message || '取消订单失败')
      } finally {
        cancelling.value = false
      }
    }

    /**
     * 支付订单
     */
    const payOrder = async (orderId) => {
      paying.value = true
      try {
        // 模拟支付（实际项目中应调用支付接口）
        const payMethod = 'ALIPAY'
        const transactionId = 'TXN' + Date.now()
        
        const response = await orderAPI.payOrder(orderId, payMethod, transactionId)
        if (response.success) {
          alert('支付成功！电子票已生成')
          loadOrders() // 重新加载订单列表
        } else {
          alert(response.message || '支付失败')
        }
      } catch (error) {
        alert(error.message || '支付失败')
      } finally {
        paying.value = false
      }
    }

    /**
     * 退票（退款）
     */
    const refundOrder = async (orderId) => {
      const refundReason = prompt('请输入退票原因：', '个人原因')
      if (!refundReason) {
        return // 用户取消输入
      }

      refunding.value = true
      try {
        const response = await orderAPI.refundOrder(orderId, refundReason)
        if (response.success) {
          alert('退票成功！退款将在1-3个工作日内到账')
          loadOrders() // 重新加载订单列表
        } else {
          alert(response.message || '退票失败')
        }
      } catch (error) {
        alert(error.message || '退票失败')
      } finally {
        refunding.value = false
      }
    }

    /**
     * 跳转到订单详情
     */
    const goToOrderDetail = (orderId) => {
      router.push({ name: 'OrderDetail', params: { orderId } })
    }

    /**
     * 跳转到赛事列表（返回篮球赛事页面）
     */
    const goToMatches = () => {
      router.push({ name: 'MatchList' })
    }

    /**
     * 获取状态文本
     */
    const getStatusText = (status) => {
      const statusMap = {
        'PENDING': '待支付',
        'PAID': '已支付',
        'COMPLETED': '已完成',
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
        'COMPLETED': 'status-completed',
        'CANCELLED': 'status-cancelled',
        'REFUNDED': 'status-refunded',
        'EXPIRED': 'status-expired'
      }
      return statusMap[status] || ''
    }

    /**
     * 检查是否即将过期（30分钟内）
     */
    const isExpiringSoon = (expireTime) => {
      if (!expireTime) return false
      const expire = new Date(expireTime)
      const now = new Date()
      const diff = expire.getTime() - now.getTime()
      return diff > 0 && diff < 30 * 60 * 1000 // 30分钟
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
      loadOrders()
    })

    return {
      loading,
      cancelling,
      paying,
      refunding,
      orders,
      statusFilter,
      loadOrders,
      cancelOrder,
      payOrder,
      refundOrder,
      goToOrderDetail,
      goToMatches,
      getStatusText,
      getStatusClass,
      isExpiringSoon,
      formatDateTime,
      formatPrice
    }
  }
}
</script>

<style scoped>
.my-orders-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.my-orders-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-bar label {
  font-weight: 600;
  color: #606266;
}

.filter-bar select {
  padding: 8px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.go-matches-btn {
  margin-top: 20px;
  padding: 10px 30px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.order-card {
  background: white;
  border-radius: 8px;
  padding: 25px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-number {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.order-time {
  color: #909399;
  font-size: 14px;
}

.order-status {
  padding: 6px 15px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.status-pending {
  background: #fef0f0;
  color: #e6a23c;
}

.status-paid {
  background: #f0f9ff;
  color: #67c23a;
}

.status-completed {
  background: #f4f4f5;
  color: #909399;
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

.order-body {
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.order-item {
  display: flex;
  align-items: center;
}

.order-item .label {
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
}

.order-item .value {
  color: #303133;
}

.order-item .value.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 600;
}

.order-item .value.expiring {
  color: #f56c6c;
  font-weight: 600;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.action-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: white;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.cancel-btn:hover:not(:disabled) {
  border-color: #909399;
  color: #303133;
}

.pay-btn {
  background: #409eff;
  color: white;
}

.pay-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.detail-btn {
  background: #67c23a;
  color: white;
}

.detail-btn:hover {
  background: #85ce61;
}

.refund-btn {
  background: #f56c6c;
  color: white;
}

.refund-btn:hover:not(:disabled) {
  background: #f78989;
}

.action-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
  color: white;
}

@media (max-width: 768px) {
  .order-body {
    grid-template-columns: 1fr;
  }

  .my-orders-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .order-footer {
    flex-wrap: wrap;
  }
}
</style>
