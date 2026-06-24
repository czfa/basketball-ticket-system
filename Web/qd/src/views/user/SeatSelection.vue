<template>
  <div class="seat-selection-container">
    <div class="seat-selection-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1 class="page-title">在线选座 - {{ matchInfo?.matchName || '加载中...' }}</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载座位数据中...</p>
    </div>

    <div v-else-if="!matchInfo" class="error-state">
      <p>赛事不存在</p>
    </div>

    <div v-else class="seat-selection-content">
      <!-- 座位图例 -->
      <div class="seat-legend">
        <div class="legend-item">
          <div class="legend-seat seat-available"></div>
          <span>可选</span>
        </div>
        <div class="legend-item">
          <div class="legend-seat seat-selected"></div>
          <span>已选</span>
        </div>
        <div class="legend-item">
          <div class="legend-seat seat-booked"></div>
          <span>已售</span>
        </div>
        <div class="legend-item">
          <div class="legend-seat seat-unavailable"></div>
          <span>不可用</span>
        </div>
      </div>

      <!-- 座位区域筛选 -->
      <div class="zone-filter">
        <label>座位区域：</label>
        <select v-model="selectedZone" @change="filterSeats">
          <option value="">全部区域</option>
          <option v-for="zone in availableZones" :key="zone" :value="zone">{{ zone }}</option>
        </select>
      </div>

      <!-- 座位图 -->
      <div class="seat-map-container">
        <div v-for="zone in filteredZones" :key="zone" class="zone-section">
          <h3 class="zone-title">{{ zone }}</h3>
          <div class="seat-map">
            <div
              v-for="seat in getSeatsByZone(zone)"
              :key="seat.seatId"
              class="seat-item"
              :class="getSeatClass(seat)"
              @click="toggleSeat(seat)"
              :title="getSeatTooltip(seat)"
            >
              <span class="seat-number">{{ seat.seatNumber || `${seat.seatRow}排${seat.seatCol}座` }}</span>
              <span class="seat-price">¥{{ formatPrice(seat.price) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 已选座位列表和结算 -->
      <div class="selected-seats-panel">
        <div class="selected-seats-info">
          <h3>已选座位 ({{ selectedSeats.length }})</h3>
          <div class="selected-seats-list">
            <div
              v-for="seat in selectedSeats"
              :key="seat.seatId"
              class="selected-seat-item"
            >
              <span>{{ seat.seatZone }} - {{ seat.seatNumber || `${seat.seatRow}排${seat.seatCol}座` }}</span>
              <span class="seat-price">¥{{ formatPrice(seat.price) }}</span>
              <button class="remove-btn" @click="removeSeat(seat.seatId)">移除</button>
            </div>
            <div v-if="selectedSeats.length === 0" class="empty-selected">
              请选择座位
            </div>
          </div>
        </div>
        <div class="checkout-section">
          <div class="total-info">
            <div class="total-item">
              <span>座位数量：</span>
              <span>{{ selectedSeats.length }}</span>
            </div>
            <div class="total-item">
              <span>总金额：</span>
              <span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
            </div>
          </div>
          <button
            class="checkout-btn"
            :disabled="selectedSeats.length === 0 || checking"
            @click="handleCheckout"
          >
            {{ checking ? '处理中...' : '确认选座并下单' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref,computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { matchAPI, seatAPI } from '@/api'

export default {
  name: 'SeatSelection',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(true)
    const checking = ref(false)
    const matchInfo = ref(null)
    const seats = ref([])
    const selectedSeats = ref([])
    const selectedZone = ref('')

    /**
     * 加载赛事信息和座位数据
     */
    const loadData = async () => {
      const matchId = route.params.matchId
      if (!matchId) {
        loading.value = false
        return
      }

      try {
        // 并行加载赛事信息和座位列表
        const [matchResponse, seatResponse] = await Promise.all([
          matchAPI.getMatchDetail(matchId),
          seatAPI.getSeatsByMatch(matchId)
        ])

        if (matchResponse.success) {
          matchInfo.value = matchResponse.data
        }

        if (seatResponse.success) {
          seats.value = seatResponse.data || []
        }
      } catch (error) {
        alert(error.message || '加载数据失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 获取所有可用区域
     */
    const availableZones = computed(() => {
      const zones = new Set()
      seats.value.forEach(seat => {
        if (seat.seatZone) {
          zones.add(seat.seatZone)
        }
      })
      return Array.from(zones).sort()
    })

    /**
     * 获取过滤后的区域列表
     */
    const filteredZones = computed(() => {
      if (selectedZone.value) {
        return [selectedZone.value]
      }
      return availableZones.value
    })

    /**
     * 根据区域获取座位
     */
    const getSeatsByZone = (zone) => {
      let filtered = seats.value.filter(seat => seat.seatZone === zone)
      
      // 按排和列排序
      filtered.sort((a, b) => {
        if (a.seatRow !== b.seatRow) {
          return (a.seatRow || 0) - (b.seatRow || 0)
        }
        return (a.seatCol || 0) - (b.seatCol || 0)
      })

      return filtered
    }

    /**
     * 获取座位样式类
     */
    const getSeatClass = (seat) => {
      // 检查是否已选
      if (selectedSeats.value.find(s => s.seatId === seat.seatId)) {
        return 'seat-selected'
      }

      // 判断座位状态
      if (!seat.isAvailable) {
        return 'seat-unavailable'
      }
      if (seat.isBooked) {
        return 'seat-booked'
      }
      return 'seat-available'
    }

    /**
     * 获取座位提示信息
     */
    const getSeatTooltip = (seat) => {
      if (!seat.isAvailable) {
        return '不可用'
      }
      if (seat.isBooked) {
        return '已售'
      }
      return `${seat.seatZone} - ${seat.seatNumber || `${seat.seatRow}排${seat.seatCol}座`} - ¥${formatPrice(seat.price)}`
    }

    /**
     * 切换座位选择状态
     */
    const toggleSeat = (seat) => {
      // 不可选座位
      if (!seat.isAvailable || seat.isBooked) {
        return
      }

      // 查找是否已选
      const index = selectedSeats.value.findIndex(s => s.seatId === seat.seatId)
      if (index > -1) {
        // 已选，取消选择
        selectedSeats.value.splice(index, 1)
      } else {
        // 未选，添加到已选列表
        selectedSeats.value.push(seat)
      }
    }

    /**
     * 移除已选座位
     */
    const removeSeat = (seatId) => {
      const index = selectedSeats.value.findIndex(s => s.seatId === seatId)
      if (index > -1) {
        selectedSeats.value.splice(index, 1)
      }
    }

    /**
     * 过滤座位（按区域）
     */
    const filterSeats = () => {
      // 区域筛选已通过computed属性实现
    }

    /**
     * 计算总金额
     */
    const totalAmount = computed(() => {
      return selectedSeats.value.reduce((sum, seat) => {
        return sum + (Number(seat.price) || 0)
      }, 0)
    })

    /**
     * 确认选座并下单
     */
    const handleCheckout = async () => {
      if (selectedSeats.value.length === 0) {
        alert('请至少选择一个座位')
        return
      }

      // 再次检查座位可用性
      checking.value = true
      try {
        const seatIds = selectedSeats.value.map(s => s.seatId)
        const checkResponse = await seatAPI.checkAvailability(seatIds)

        if (checkResponse.success && checkResponse.data.allAvailable) {
          // 所有座位可用，跳转到订单确认页
          // 将选座信息存储到sessionStorage，供订单确认页使用
          sessionStorage.setItem('selected_seats', JSON.stringify({
            matchId: route.params.matchId,
            matchInfo: matchInfo.value,
            seats: selectedSeats.value,
            totalAmount: totalAmount.value
          }))

          router.push({ name: 'OrderConfirm' })
        } else {
          // 有座位不可用，提示用户并刷新座位状态
          alert('部分座位已被其他用户预订，请重新选择')
          await loadData() // 重新加载座位状态
          selectedSeats.value = [] // 清空已选座位
        }
      } catch (error) {
        alert(error.message || '检查座位可用性失败')
      } finally {
        checking.value = false
      }
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0'
      return Number(price).toFixed(2)
    }

    /**
     * 返回上一页
     */
    const goBack = () => {
      router.back()
    }

    onMounted(() => {
      loadData()
    })

    return {
      loading,
      checking,
      matchInfo,
      seats,
      selectedSeats,
      selectedZone,
      availableZones,
      filteredZones,
      totalAmount,
      loadData,
      getSeatsByZone,
      getSeatClass,
      getSeatTooltip,
      toggleSeat,
      removeSeat,
      filterSeats,
      handleCheckout,
      formatPrice,
      goBack
    }
  }
}
</script>

<style scoped>
.seat-selection-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.seat-selection-header {
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

.back-btn:hover {
  color: #66b1ff;
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

.seat-selection-content {
  max-width: 1400px;
  margin: 0 auto;
}

.seat-legend {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  gap: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-seat {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 2px solid;
}

.seat-available {
  background: #67c23a;
  border-color: #67c23a;
}

.seat-selected {
  background: #409eff;
  border-color: #409eff;
}

.seat-booked {
  background: #909399;
  border-color: #909399;
}

.seat-unavailable {
  background: #f4f4f5;
  border-color: #dcdfe6;
}

.zone-filter {
  background: white;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.zone-filter label {
  margin-right: 10px;
  font-weight: 600;
  color: #606266;
}

.zone-filter select {
  padding: 8px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.seat-map-container {
  background: white;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.zone-section {
  margin-bottom: 40px;
}

.zone-section:last-child {
  margin-bottom: 0;
}

.zone-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 15px;
  border-bottom: 2px solid #ebeef5;
}

.seat-map {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 20px;
}

.seat-item {
  padding: 10px;
  border: 2px solid;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-height: 80px;
  justify-content: center;
}

.seat-item.seat-available {
  background: #f0f9ff;
  border-color: #67c23a;
  color: #67c23a;
}

.seat-item.seat-available:hover {
  background: #e1f3d8;
  transform: scale(1.05);
}

.seat-item.seat-selected {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
  font-weight: 600;
}

.seat-item.seat-booked {
  background: #f4f4f5;
  border-color: #909399;
  color: #909399;
  cursor: not-allowed;
}

.seat-item.seat-unavailable {
  background: #fafafa;
  border-color: #dcdfe6;
  color: #c0c4cc;
  cursor: not-allowed;
}

.seat-number {
  font-size: 12px;
  font-weight: 600;
}

.seat-price {
  font-size: 11px;
}

.selected-seats-panel {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 30px;
}

.selected-seats-info h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #303133;
}

.selected-seats-list {
  max-height: 300px;
  overflow-y: auto;
}

.selected-seat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 6px;
}

.selected-seat-item .seat-price {
  color: #f56c6c;
  font-weight: 600;
  margin: 0 10px;
}

.remove-btn {
  padding: 5px 15px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.remove-btn:hover {
  background: #f78989;
}

.empty-selected {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.checkout-section {
  border-left: 1px solid #ebeef5;
  padding-left: 30px;
}

.total-info {
  margin-bottom: 20px;
}

.total-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 16px;
  color: #606266;
}

.total-amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 600;
}

.checkout-btn {
  width: 100%;
  padding: 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.checkout-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.checkout-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .selected-seats-panel {
    grid-template-columns: 1fr;
  }

  .checkout-section {
    border-left: none;
    border-top: 1px solid #ebeef5;
    padding-left: 0;
    padding-top: 20px;
  }
}
</style>
