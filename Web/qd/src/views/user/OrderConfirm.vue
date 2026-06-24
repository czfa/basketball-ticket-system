<template>
  <div class="order-confirm-container">
    <div class="order-confirm-header">
      <h1 class="page-title">订单确认</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载订单信息中...</p>
    </div>

    <div v-else-if="!orderData" class="error-state">
      <p>订单数据不存在，请重新选座</p>
      <button class="back-btn" @click="goToMatches">返回赛事列表</button>
    </div>

    <div v-else class="order-confirm-content">
      <!-- 赛事信息 -->
      <div class="order-section">
        <h2>赛事信息</h2>
        <div class="match-info">
          <div class="info-item">
            <span class="label">赛事名称：</span>
            <span class="value">{{ orderData.matchInfo?.matchName }}</span>
          </div>
          <div class="info-item">
            <span class="label">对阵双方：</span>
            <span class="value">{{ orderData.matchInfo?.homeTeam }} VS {{ orderData.matchInfo?.awayTeam }}</span>
          </div>
          <div class="info-item">
            <span class="label">比赛时间：</span>
            <span class="value">{{ formatDateTime(orderData.matchInfo?.matchTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">比赛场馆：</span>
            <span class="value">{{ orderData.matchInfo?.venue }}</span>
          </div>
        </div>
      </div>

      <!-- 已选座位 -->
      <div class="order-section">
        <h2>已选座位 ({{ orderData.seats?.length || 0 }})</h2>
        <div class="seats-list">
          <div
            v-for="seat in orderData.seats"
            :key="seat.seatId"
            class="seat-item"
          >
            <span>{{ seat.seatZone }} - {{ seat.seatNumber || `${seat.seatRow}排${seat.seatCol}座` }}</span>
            <span class="seat-type">{{ seat.seatType }}</span>
            <span class="seat-price">¥{{ formatPrice(seat.price) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单金额 -->
      <div class="order-section order-summary">
        <div class="summary-item">
          <span class="label">座位数量：</span>
          <span class="value">{{ orderData.seats?.length || 0 }} 个</span>
        </div>
        <div class="summary-item">
          <span class="label">订单总额：</span>
          <span class="value total-amount">¥{{ formatPrice(orderData.totalAmount) }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="order-actions">
        <button class="cancel-btn" @click="goBack">返回修改</button>
        <button
          class="confirm-btn"
          :disabled="creating"
          @click="createOrder"
        >
          {{ creating ? '创建订单中...' : '确认下单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderAPI } from '@/api'

export default {
  name: 'OrderConfirm',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const creating = ref(false)
    const orderData = ref(null)

    /**
     * 加载订单数据
     */
    const loadOrderData = () => {
      loading.value = true
      try {
        // 从sessionStorage获取选座信息
        const storedData = sessionStorage.getItem('selected_seats')
        if (storedData) {
          orderData.value = JSON.parse(storedData)
        }
      } catch (error) {
        console.error('加载订单数据失败:', error)
      } finally {
        loading.value = false
      }
    }

    /**
     * 创建订单（直接创建并自动支付）
     */
    const createOrder = async () => {
      if (!orderData.value || !orderData.value.seats || orderData.value.seats.length === 0) {
        alert('订单数据不完整，请重新选座')
        return
      }

      // 检查是否登录
      const userInfo = JSON.parse(localStorage.getItem('user_info') || 'null')
      if (!userInfo || !userInfo.userId) {
        alert('请先登录')
        router.push({ name: 'UserLogin' })
        return
      }

      creating.value = true
      try {
        const seatIds = orderData.value.seats.map(s => s.seatId)
        // 创建订单并自动支付（传递 userId）
        const response = await orderAPI.createOrder(orderData.value.matchId, seatIds, userInfo.userId)

        if (response.success) {
          // 订单创建成功，清除选座信息
          sessionStorage.removeItem('selected_seats')
          
          alert(response.message || '订单创建成功！')
          // 跳转到订单详情页（response 已经是后端返回的数据，不需要 .data）
          const orderId = response.orderId
          if (orderId) {
            router.push({ name: 'OrderDetail', params: { orderId: orderId } })
          } else {
            // 如果没有 orderId，跳转到我的订单列表
            router.push({ name: 'MyOrders' })
          }
        } else {
          alert(response.message || '订单创建失败')
        }
      } catch (error) {
        console.error('创建订单错误:', error)
        alert(error.message || '订单创建失败，请重试')
      } finally {
        creating.value = false
      }
    }

    /**
     * 返回上一页
     */
    const goBack = () => {
      router.back()
    }

    /**
     * 跳转到赛事列表
     */
    const goToMatches = () => {
      router.push({ name: 'MatchList' })
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
      loadOrderData()
    })

    return {
      loading,
      creating,
      orderData,
      loadOrderData,
      createOrder,
      goBack,
      goToMatches,
      formatDateTime,
      formatPrice
    }
  }
}
</script>

<style scoped>
.order-confirm-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.order-confirm-header {
  margin-bottom: 20px;
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

.error-state .back-btn {
  margin-top: 20px;
  padding: 10px 30px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.order-confirm-content {
  max-width: 1000px;
  margin: 0 auto;
}

.order-section {
  background: white;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.order-section h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #303133;
  padding-bottom: 15px;
  border-bottom: 2px solid #ebeef5;
}

.match-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
}

.info-item .label {
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
}

.info-item .value {
  color: #303133;
}

.seats-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.seat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.seat-type {
  color: #909399;
  font-size: 14px;
}

.seat-price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 16px;
}

.order-summary {
  background: #ecf5ff;
  border: 2px solid #409eff;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 16px;
}

.summary-item:last-child {
  margin-bottom: 0;
  padding-top: 15px;
  border-top: 1px solid #b3d8ff;
  font-size: 20px;
}

.summary-item .label {
  color: #606266;
}

.summary-item .value {
  color: #303133;
  font-weight: 600;
}

.total-amount {
  color: #f56c6c;
  font-size: 28px;
}

.order-actions {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 30px;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  padding: 15px;
  border: none;
  border-radius: 6px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: white;
  color: #606266;
  border: 2px solid #dcdfe6;
}

.cancel-btn:hover {
  border-color: #909399;
  color: #303133;
}

.confirm-btn {
  background: #409eff;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.confirm-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .match-info {
    grid-template-columns: 1fr;
  }

  .order-actions {
    flex-direction: column;
  }
}
</style>
