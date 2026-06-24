<template>
  <div class="order-detail-container">
    <div class="order-detail-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1 class="page-title">订单详情</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载订单详情中...</p>
    </div>

    <div v-else-if="!order" class="error-state">
      <p>订单不存在</p>
    </div>

    <div v-else class="order-detail-content">
      <!-- 订单基本信息 -->
      <div class="detail-section">
        <h2>订单信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">订单号：</span>
            <span class="value">{{ order.orderNumber }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单状态：</span>
            <span class="value status-badge" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">下单时间：</span>
            <span class="value">{{ formatDateTime(order.orderTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付时间：</span>
            <span class="value">{{ formatDateTime(order.payTime) || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">过期时间：</span>
            <span class="value" :class="{ 'expiring': isExpiringSoon(order.expireTime) }">
              {{ formatDateTime(order.expireTime) || '-' }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">订单金额：</span>
            <span class="value price">¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单操作 -->
      <div class="detail-actions" v-if="order.status === 'PENDING'">
        <button class="action-btn cancel-btn" @click="handleCancel" :disabled="processing">
          取消订单
        </button>
        <button class="action-btn pay-btn" @click="handlePay" :disabled="processing">
          立即支付
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { orderAPI } from '@/api'

export default {
  name: 'OrderDetail',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(true)
    const processing = ref(false)
    const order = ref(null)

    /**
     * 加载订单详情
     */
    const loadOrderDetail = async () => {
      const orderId = route.params.orderId
      if (!orderId) {
        loading.value = false
        return
      }

      try {
        const response = await orderAPI.getOrderDetail(orderId)
        if (response.success) {
          order.value = response.data
        } else {
          alert(response.message || '获取订单详情失败')
        }
      } catch (error) {
        alert(error.message || '获取订单详情失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 取消订单
     */
    const handleCancel = async () => {
      if (!confirm('确定要取消此订单吗？')) {
        return
      }

      processing.value = true
      try {
        const response = await orderAPI.cancelOrder(order.value.orderId)
        if (response.success) {
          alert('订单已取消')
          loadOrderDetail() // 重新加载订单详情
        } else {
          alert(response.message || '取消订单失败')
        }
      } catch (error) {
        alert(error.message || '取消订单失败')
      } finally {
        processing.value = false
      }
    }

    /**
     * 支付订单
     */
    const handlePay = async () => {
      processing.value = true
      try {
        const payMethod = 'ALIPAY'
        const transactionId = 'TXN' + Date.now()
        const response = await orderAPI.payOrder(order.value.orderId, payMethod, transactionId)
        if (response.success) {
          alert('支付成功！电子票已生成')
          loadOrderDetail() // 重新加载订单详情
        } else {
          alert(response.message || '支付失败')
        }
      } catch (error) {
        alert(error.message || '支付失败')
      } finally {
        processing.value = false
      }
    }

    /**
     * 返回上一页
     */
    const goBack = () => {
      router.back()
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
     * 检查是否即将过期
     */
    const isExpiringSoon = (expireTime) => {
      if (!expireTime) return false
      const expire = new Date(expireTime)
      const now = new Date()
      const diff = expire.getTime() - now.getTime()
      return diff > 0 && diff < 30 * 60 * 1000
    }

    /**
     * 格式化日期时间
     */
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      return new Date(dateTime).toLocaleString('zh-CN')
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0'
      return Number(price).toFixed(2)
    }

    onMounted(() => {
      loadOrderDetail()
    })

    return {
      loading,
      processing,
      order,
      loadOrderDetail,
      handleCancel,
      handlePay,
      goBack,
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
.order-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.order-detail-header {
  margin-bottom: 20px;
}

.back-btn {
  background: none;
  border: none;
  color: #409eff;
  font-size: 16px;
  cursor: pointer;
  padding: 10px 0;
  margin-bottom: 10px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.order-detail-content {
  max-width: 1000px;
  margin: 0 auto;
}

.detail-section {
  background: white;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.detail-section h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #303133;
  padding-bottom: 15px;
  border-bottom: 2px solid #ebeef5;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item .label {
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
}

.info-item .value {
  color: #303133;
}

.info-item .value.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 600;
}

.info-item .value.expiring {
  color: #f56c6c;
  font-weight: 600;
}

.status-badge {
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

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  background: white;
  border-radius: 8px;
  padding: 20px 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-btn {
  padding: 12px 30px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: white;
  color: #606266;
  border: 2px solid #dcdfe6;
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

.action-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
  color: white;
}
</style>
