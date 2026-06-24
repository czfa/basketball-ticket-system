<template>
  <div class="admin-seat-management-container">
    <div class="admin-header">
      <div class="header-left">
        <button class="back-btn" @click="goToAdminDashboard">← 返回管理后台首页</button>
        <h1 class="page-title">座位管理</h1>
      </div>
      <div class="header-actions">
        <select v-model="selectedMatchId" @change="loadSeats" class="match-select">
          <option value="">请选择赛事</option>
          <option v-for="match in matches" :key="match.matchId" :value="match.matchId">
            {{ match.matchName }}
          </option>
        </select>
        <button v-if="selectedMatchId" class="btn btn-primary" @click="showGenerateDialog">
          批量生成座位
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <div v-else-if="!selectedMatchId" class="empty-state">
      <p>请先选择赛事</p>
    </div>

    <div v-else-if="seats.length === 0" class="empty-state">
      <p>该赛事暂无座位，请先批量生成座位</p>
    </div>

    <div v-else class="seat-management-content">
      <!-- 座位列表 -->
      <div class="seats-header">
        <div class="header-left">
          <button class="btn btn-secondary btn-small" @click="handleSelectAll" :disabled="seats.length === 0">
            {{ isAllSelected() ? '取消全选' : '一键全选' }}
          </button>
          <span class="seat-count">共 {{ seats.length }} 个座位</span>
        </div>
        <button class="btn btn-danger" @click="handleBatchDelete" :disabled="selectedSeatIds.length === 0">
          批量删除 ({{ selectedSeatIds.length }})
        </button>
      </div>

      <div class="seats-grid">
        <div
          v-for="seat in seats"
          :key="seat.seatId"
          class="seat-card"
          :class="{ selected: selectedSeatIds.includes(seat.seatId), booked: seat.isBooked }"
          @click="!seat.isBooked && toggleSeatSelection(seat.seatId)"
        >
          <div class="seat-header">
            <input
              type="checkbox"
              :checked="selectedSeatIds.includes(seat.seatId)"
              @click.stop="toggleSeatSelection(seat.seatId)"
              :disabled="seat.isBooked"
            />
            <span class="seat-number">{{ seat.seatNumber }}</span>
          </div>
          <div class="seat-body">
            <div class="seat-info">
              <span class="label">区域：</span>
              <span class="value">{{ seat.zone }}</span>
            </div>
            <div class="seat-info">
              <span class="label">排：</span>
              <span class="value">{{ seat.rowNumber }}</span>
            </div>
            <div class="seat-info">
              <span class="label">座：</span>
              <span class="value">{{ seat.colNumber }}</span>
            </div>
            <div class="seat-info">
              <span class="label">类型：</span>
              <span class="value">{{ seat.seatType }}</span>
            </div>
            <div class="seat-info">
              <span class="label">价格：</span>
              <span class="value price">¥{{ formatPrice(seat.price) }}</span>
            </div>
            <div class="seat-info">
              <span class="label">状态：</span>
              <span class="value" :class="getStatusClass(seat)">
                {{ getStatusText(seat) }}
              </span>
            </div>
          </div>
          <div class="seat-footer">
            <button class="btn btn-small btn-primary" @click.stop="showEditDialog(seat)">
              编辑
            </button>
            <button class="btn btn-small btn-danger" @click.stop="handleDeleteSeat(seat.seatId)" :disabled="seat.isBooked">
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量生成座位对话框 -->
    <div v-if="showGenerateDialogFlag" class="dialog-overlay" @click="closeGenerateDialog">
      <div class="dialog-content" @click.stop>
        <h2 class="dialog-title">批量生成座位</h2>
        <form @submit.prevent="handleGenerateSeats" class="dialog-form">
          <div class="form-group">
            <label>区域配置（JSON格式）*</label>
            <textarea
              v-model="generateConfig"
              rows="10"
              placeholder='示例：[{"zone":"A区","rows":10,"cols":20,"seatType":"STANDARD","price":200.00}]'
              required
            ></textarea>
            <span class="form-hint">
              格式：数组，每个元素包含 zone(区域), rows(行数), cols(列数), seatType(类型), price(价格)
            </span>
          </div>
          <div v-if="generateMessage" class="message" :class="{ error: isGenerateError, success: !isGenerateError }">
            {{ generateMessage }}
          </div>
          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeGenerateDialog">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="generating">
              {{ generating ? '生成中...' : '确定生成' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 编辑座位对话框 -->
    <div v-if="showEditDialogFlag" class="dialog-overlay" @click="closeEditDialog">
      <div class="dialog-content" @click.stop>
        <h2 class="dialog-title">编辑座位</h2>
        <form @submit.prevent="handleUpdateSeat" class="dialog-form">
          <div class="form-group">
            <label>座位号</label>
            <input v-model="editFormData.seatNumber" type="text" disabled class="disabled" />
          </div>
          <div class="form-group">
            <label>区域</label>
            <input v-model="editFormData.zone" type="text" required />
          </div>
          <div class="form-group">
            <label>排号</label>
            <input v-model.number="editFormData.rowNumber" type="number" required />
          </div>
          <div class="form-group">
            <label>座号</label>
            <input v-model.number="editFormData.colNumber" type="number" required />
          </div>
          <div class="form-group">
            <label>座位类型</label>
            <select v-model="editFormData.seatType" required>
              <option value="STANDARD">标准座</option>
              <option value="PREMIUM">高级座</option>
              <option value="VIP">VIP座</option>
            </select>
          </div>
          <div class="form-group">
            <label>价格</label>
            <input v-model.number="editFormData.price" type="number" step="0.01" required />
          </div>
          <div class="form-group">
            <label>是否可用</label>
            <select v-model="editFormData.isAvailable">
              <option :value="true">可用</option>
              <option :value="false">不可用</option>
            </select>
          </div>
          <div v-if="editMessage" class="message" :class="{ error: isEditError, success: !isEditError }">
            {{ editMessage }}
          </div>
          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeEditDialog">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="updating">
              {{ updating ? '更新中...' : '确定' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminAPI } from '@/api'

export default {
  name: 'AdminSeatManagement',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const matches = ref([])
    const selectedMatchId = ref('')
    const seats = ref([])
    const selectedSeatIds = ref([])
    const showGenerateDialogFlag = ref(false)
    const showEditDialogFlag = ref(false)
    const generating = ref(false)
    const updating = ref(false)
    const generateMessage = ref('')
    const isGenerateError = ref(false)
    const editMessage = ref('')
    const isEditError = ref(false)

    const generateConfig = ref('')
    const editFormData = reactive({
      seatId: null,
      seatNumber: '',
      zone: '',
      rowNumber: 0,
      colNumber: 0,
      seatType: 'STANDARD',
      price: 0,
      isAvailable: true
    })

    /**
     * 返回管理后台首页
     */
    const goToAdminDashboard = () => {
      router.push({ name: 'AdminDashboard' })
    }

    /**
     * 加载赛事列表
     */
    const loadMatches = async () => {
      try {
        const response = await adminAPI.getAllMatches()
        if (response.success) {
          matches.value = response.data || []
        }
      } catch (error) {
        console.error('加载赛事列表失败:', error)
      }
    }

    /**
     * 加载座位列表
     */
    const loadSeats = async () => {
      if (!selectedMatchId.value) {
        seats.value = []
        return
      }

      loading.value = true
      try {
        const response = await adminAPI.getSeatsByMatch(selectedMatchId.value)
        if (response.success) {
          seats.value = response.data || []
          selectedSeatIds.value = []
        } else {
          alert(response.message || '获取座位列表失败')
        }
      } catch (error) {
        alert(error.message || '获取座位列表失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 切换座位选择
     */
    const toggleSeatSelection = (seatId) => {
      const seat = seats.value.find(s => s.seatId === seatId)
      if (seat && seat.isBooked) {
        return // 已预订的座位不能选中
      }
      const index = selectedSeatIds.value.indexOf(seatId)
      if (index > -1) {
        selectedSeatIds.value.splice(index, 1)
      } else {
        selectedSeatIds.value.push(seatId)
      }
    }

    /**
     * 获取可选座位列表（排除已预订的）
     */
    const getSelectableSeats = () => {
      return seats.value.filter(seat => !seat.isBooked)
    }

    /**
     * 判断是否全选（只针对可选座位）
     */
    const isAllSelected = () => {
      const selectableSeats = getSelectableSeats()
      return selectableSeats.length > 0 && selectedSeatIds.value.length === selectableSeats.length
    }

    /**
     * 全选/取消全选（只针对可选座位）
     */
    const handleSelectAll = () => {
      if (isAllSelected()) {
        // 取消全选
        selectedSeatIds.value = []
      } else {
        // 全选所有可选座位（排除已预订的）
        selectedSeatIds.value = getSelectableSeats().map(seat => seat.seatId)
      }
    }

    /**
     * 显示生成座位对话框
     */
    const showGenerateDialog = () => {
      generateConfig.value = ''
      generateMessage.value = ''
      showGenerateDialogFlag.value = true
    }

    /**
     * 关闭生成座位对话框
     */
    const closeGenerateDialog = () => {
      showGenerateDialogFlag.value = false
      generateConfig.value = ''
      generateMessage.value = ''
    }

    /**
     * 批量生成座位
     */
    const handleGenerateSeats = async () => {
      generating.value = true
      generateMessage.value = ''
      isGenerateError.value = false

      try {
        let config
        try {
          config = JSON.parse(generateConfig.value)
        } catch (e) {
          generateMessage.value = 'JSON格式错误，请检查配置'
          isGenerateError.value = true
          generating.value = false
          return
        }

        const response = await adminAPI.generateSeats(selectedMatchId.value, { zones: config })
        if (response.success) {
          generateMessage.value = '座位生成成功'
          isGenerateError.value = false
          setTimeout(() => {
            closeGenerateDialog()
            loadSeats()
          }, 1000)
        } else {
          generateMessage.value = response.message || '生成失败'
          isGenerateError.value = true
        }
      } catch (error) {
        generateMessage.value = error.message || '生成失败，请重试'
        isGenerateError.value = true
      } finally {
        generating.value = false
      }
    }

    /**
     * 显示编辑对话框
     */
    const showEditDialog = (seat) => {
      editFormData.seatId = seat.seatId
      editFormData.seatNumber = seat.seatNumber || ''
      editFormData.zone = seat.zone || ''
      editFormData.rowNumber = seat.rowNumber || 0
      editFormData.colNumber = seat.colNumber || 0
      editFormData.seatType = seat.seatType || 'STANDARD'
      editFormData.price = seat.price || 0
      editFormData.isAvailable = seat.isAvailable !== false
      editMessage.value = ''
      showEditDialogFlag.value = true
    }

    /**
     * 关闭编辑对话框
     */
    const closeEditDialog = () => {
      showEditDialogFlag.value = false
      editMessage.value = ''
    }

    /**
     * 更新座位
     */
    const handleUpdateSeat = async () => {
      updating.value = true
      editMessage.value = ''
      isEditError.value = false

      try {
        const response = await adminAPI.updateSeat(editFormData.seatId, {
          zone: editFormData.zone,
          rowNumber: editFormData.rowNumber,
          colNumber: editFormData.colNumber,
          seatType: editFormData.seatType,
          price: editFormData.price,
          isAvailable: editFormData.isAvailable
        })

        if (response.success) {
          editMessage.value = '更新成功'
          isEditError.value = false
          setTimeout(() => {
            closeEditDialog()
            loadSeats()
          }, 1000)
        } else {
          editMessage.value = response.message || '更新失败'
          isEditError.value = true
        }
      } catch (error) {
        editMessage.value = error.message || '更新失败，请重试'
        isEditError.value = true
      } finally {
        updating.value = false
      }
    }

    /**
     * 删除单个座位
     */
    const handleDeleteSeat = async (seatId) => {
      if (!confirm('确定要删除此座位吗？')) {
        return
      }

      try {
        const response = await adminAPI.deleteSeat(seatId)
        if (response.success) {
          alert('删除成功')
          loadSeats()
        } else {
          alert(response.message || '删除失败')
        }
      } catch (error) {
        alert(error.message || '删除失败')
      }
    }

    /**
     * 批量删除座位
     */
    const handleBatchDelete = async () => {
      if (selectedSeatIds.value.length === 0) {
        return
      }

      if (!confirm(`确定要删除选中的 ${selectedSeatIds.value.length} 个座位吗？`)) {
        return
      }

      try {
        const response = await adminAPI.batchDeleteSeats(selectedSeatIds.value)
        if (response.success) {
          alert('批量删除成功')
          selectedSeatIds.value = []
          loadSeats()
        } else {
          alert(response.message || '批量删除失败')
        }
      } catch (error) {
        alert(error.message || '批量删除失败')
      }
    }

    /**
     * 格式化价格
     */
    const formatPrice = (price) => {
      if (!price) return '0.00'
      return Number(price).toFixed(2)
    }

    /**
     * 获取座位状态文本
     */
    const getStatusText = (seat) => {
      if (!seat.isAvailable) return '不可用'
      if (seat.isBooked) return '已预订'
      return '可用'
    }

    /**
     * 获取座位状态样式类
     */
    const getStatusClass = (seat) => {
      if (!seat.isAvailable) return 'status-unavailable'
      if (seat.isBooked) return 'status-booked'
      return 'status-available'
    }

    onMounted(() => {
      loadMatches()
    })

    return {
      loading,
      matches,
      selectedMatchId,
      seats,
      selectedSeatIds,
      showGenerateDialogFlag,
      showEditDialogFlag,
      generating,
      updating,
      generateConfig,
      generateMessage,
      isGenerateError,
      editFormData,
      editMessage,
      isEditError,
      isAllSelected,
      goToAdminDashboard,
      loadSeats,
      toggleSeatSelection,
      handleSelectAll,
      showGenerateDialog,
      closeGenerateDialog,
      handleGenerateSeats,
      showEditDialog,
      closeEditDialog,
      handleUpdateSeat,
      handleDeleteSeat,
      handleBatchDelete,
      formatPrice,
      getStatusText,
      getStatusClass
    }
  }
}
</script>

<style scoped>
.admin-seat-management-container {
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

.match-select {
  padding: 8px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.seat-management-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.seats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.seats-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.select-all-label {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.seat-count {
  margin-left: 10px;
  font-size: 14px;
  color: #909399;
}

.seats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.seat-card {
  border: 2px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.seat-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.seat-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.seat-card.booked {
  border-color: #e6a23c;
  background: #fdf6ec;
  cursor: not-allowed;
}

.seat-card.booked:hover {
  border-color: #e6a23c;
  box-shadow: none;
}

.seat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.seat-number {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.seat-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.seat-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.seat-info .label {
  color: #606266;
}

.seat-info .value {
  color: #303133;
  font-weight: 500;
}

.seat-info .value.price {
  color: #f56c6c;
  font-weight: 600;
}

.status-available {
  color: #67c23a;
}

.status-booked {
  color: #e6a23c;
}

.status-unavailable {
  color: #f56c6c;
}

.seat-footer {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #66b1ff;
}

.btn-danger {
  background: #f56c6c;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #f78989;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  padding: 30px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dialog-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #409eff;
}

.form-group input.disabled {
  background: #f5f7fa;
  color: #909399;
  cursor: not-allowed;
}

.form-hint {
  font-size: 12px;
  color: #909399;
}

.message {
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 14px;
}

.message.error {
  background: #fef0f0;
  color: #f56c6c;
}

.message.success {
  background: #f0f9ff;
  color: #67c23a;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-secondary {
  background: #909399;
  color: white;
}

.btn-secondary:hover {
  background: #a6a9ad;
}
</style>
