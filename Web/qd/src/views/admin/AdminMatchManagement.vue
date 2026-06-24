<template>
  <div class="admin-match-management-container">
    <div class="admin-header">
      <div class="header-left">
        <button class="back-btn" @click="goToAdminDashboard">← 返回管理后台首页</button>
        <h1 class="page-title">赛事管理</h1>
      </div>
      <button class="btn btn-primary" @click="showAddDialog">新增赛事</button>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <div v-else class="match-management-content">
      <!-- 赛事列表表格 -->
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>赛事ID</th>
              <th>赛事名称</th>
              <th>对阵双方</th>
              <th>场馆</th>
              <th>比赛时间</th>
              <th>基础票价</th>
              <th>可用座位</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="match in matches" :key="match.matchId">
              <td>{{ match.matchId }}</td>
              <td>{{ match.matchName }}</td>
              <td>{{ match.homeTeam }} VS {{ match.awayTeam }}</td>
              <td>{{ match.venue }}</td>
              <td>{{ formatDateTime(match.matchTime) }}</td>
              <td class="price">¥{{ formatPrice(match.basePrice) }}</td>
              <td>{{ match.availableSeats }}/{{ match.totalSeats }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(match.status)">
                  {{ getStatusText(match.status) }}
                </span>
              </td>
              <td class="actions">
                <button class="btn btn-small btn-primary" @click="showEditDialog(match)">编辑</button>
                <button class="btn btn-small btn-danger" @click="handleDelete(match.matchId)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 新增/编辑对话框 -->
      <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
        <div class="dialog-content" @click.stop>
          <h2 class="dialog-title">{{ isEdit ? '编辑赛事' : '新增赛事' }}</h2>
          <form @submit.prevent="handleSubmit" class="dialog-form">
            <div class="form-group">
              <label>赛事名称 *</label>
              <input v-model="formData.matchName" type="text" required placeholder="请输入赛事名称" />
            </div>
            <div class="form-group">
              <label>主队 *</label>
              <input v-model="formData.homeTeam" type="text" required placeholder="请输入主队名称" />
            </div>
            <div class="form-group">
              <label>客队 *</label>
              <input v-model="formData.awayTeam" type="text" required placeholder="请输入客队名称" />
            </div>
            <div class="form-group">
              <label>场馆 *</label>
              <input v-model="formData.venue" type="text" required placeholder="请输入场馆名称" />
            </div>
            <div class="form-group">
              <label>比赛时间 *</label>
              <input v-model="formData.matchTime" type="datetime-local" required />
            </div>
            <div class="form-group">
              <label>基础票价 *</label>
              <input v-model.number="formData.basePrice" type="number" step="0.01" required placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>总座位数 *</label>
              <input v-model.number="formData.totalSeats" type="number" required placeholder="0" />
            </div>
            <div class="form-group">
              <label>可用座位数 *</label>
              <input v-model.number="formData.availableSeats" type="number" required placeholder="0" />
            </div>
            <div class="form-group">
              <label>状态 *</label>
              <select v-model="formData.status" required>
                <option value="UPCOMING">即将开始</option>
                <option value="ONGOING">进行中</option>
                <option value="FINISHED">已结束</option>
                <option value="CANCELLED">已取消</option>
              </select>
            </div>
            <div class="form-group">
              <label>赛事描述</label>
              <textarea v-model="formData.description" rows="3" placeholder="请输入赛事描述"></textarea>
            </div>
            <div v-if="dialogMessage" class="message" :class="{ error: isError, success: !isError }">
              {{ dialogMessage }}
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="submitting">
                {{ submitting ? '提交中...' : '确定' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminAPI } from '@/api'

export default {
  name: 'AdminMatchManagement',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const matches = ref([])
    const showDialog = ref(false)
    const isEdit = ref(false)
    const submitting = ref(false)
    const dialogMessage = ref('')
    const isError = ref(false)

    const formData = reactive({
      matchName: '',
      homeTeam: '',
      awayTeam: '',
      venue: '',
      matchTime: '',
      basePrice: 0,
      totalSeats: 0,
      availableSeats: 0,
      status: 'UPCOMING',
      description: ''
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
      loading.value = true
      try {
        const response = await adminAPI.getAllMatches()
        if (response.success) {
          matches.value = response.data || []
        } else {
          alert(response.message || '获取赛事列表失败')
        }
      } catch (error) {
        alert(error.message || '获取赛事列表失败')
      } finally {
        loading.value = false
      }
    }

    /**
     * 显示新增对话框
     */
    const showAddDialog = () => {
      isEdit.value = false
      resetForm()
      showDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    const showEditDialog = (match) => {
      isEdit.value = true
      formData.matchName = match.matchName || ''
      formData.homeTeam = match.homeTeam || ''
      formData.awayTeam = match.awayTeam || ''
      formData.venue = match.venue || ''
      formData.matchTime = formatDateTimeForInput(match.matchTime)
      formData.basePrice = match.basePrice || 0
      formData.totalSeats = match.totalSeats || 0
      formData.availableSeats = match.availableSeats || 0
      formData.status = match.status || 'UPCOMING'
      formData.description = match.description || ''
      formData.matchId = match.matchId
      showDialog.value = true
    }

    /**
     * 关闭对话框
     */
    const closeDialog = () => {
      showDialog.value = false
      resetForm()
      dialogMessage.value = ''
    }

    /**
     * 重置表单
     */
    const resetForm = () => {
      formData.matchName = ''
      formData.homeTeam = ''
      formData.awayTeam = ''
      formData.venue = ''
      formData.matchTime = ''
      formData.basePrice = 0
      formData.totalSeats = 0
      formData.availableSeats = 0
      formData.status = 'UPCOMING'
      formData.description = ''
      delete formData.matchId
    }

    /**
     * 提交表单（新增或编辑）
     */
    const handleSubmit = async () => {
      submitting.value = true
      dialogMessage.value = ''
      isError.value = false

      try {
        const matchData = {
          matchName: formData.matchName,
          homeTeam: formData.homeTeam,
          awayTeam: formData.awayTeam,
          venue: formData.venue,
          matchTime: formData.matchTime,
          basePrice: formData.basePrice,
          totalSeats: formData.totalSeats,
          availableSeats: formData.availableSeats,
          status: formData.status,
          description: formData.description
        }

        let response
        if (isEdit.value) {
          response = await adminAPI.updateMatch(formData.matchId, matchData)
        } else {
          response = await adminAPI.addMatch(matchData)
        }

        if (response.success) {
          dialogMessage.value = isEdit.value ? '更新成功' : '新增成功'
          isError.value = false
          setTimeout(() => {
            closeDialog()
            loadMatches()
          }, 1000)
        } else {
          dialogMessage.value = response.message || '操作失败'
          isError.value = true
        }
      } catch (error) {
        dialogMessage.value = error.message || '操作失败，请重试'
        isError.value = true
      } finally {
        submitting.value = false
      }
    }

    /**
     * 删除赛事
     */
    const handleDelete = async (matchId) => {
      if (!confirm('确定要删除此赛事吗？删除后无法恢复！')) {
        return
      }

      try {
        const response = await adminAPI.deleteMatch(matchId)
        if (response.success) {
          alert('删除成功')
          loadMatches()
        } else {
          alert(response.message || '删除失败')
        }
      } catch (error) {
        alert(error.message || '删除失败')
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
     * 格式化日期时间为 input[type="datetime-local"] 格式
     */
    const formatDateTimeForInput = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}`
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

    onMounted(() => {
      loadMatches()
    })

    return {
      loading,
      matches,
      showDialog,
      isEdit,
      submitting,
      dialogMessage,
      isError,
      formData,
      goToAdminDashboard,
      loadMatches,
      showAddDialog,
      showEditDialog,
      closeDialog,
      handleSubmit,
      handleDelete,
      formatDateTime,
      formatPrice,
      getStatusText,
      getStatusClass
    }
  }
}
</script>

<style scoped>
.admin-match-management-container {
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

.loading-state {
  text-align: center;
  padding: 100px 20px;
  color: #606266;
}

.match-management-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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

.actions {
  display: flex;
  gap: 8px;
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

.btn-secondary {
  background: #909399;
  color: white;
}

.btn-secondary:hover {
  background: #a6a9ad;
}

.btn-danger {
  background: #f56c6c;
  color: white;
}

.btn-danger:hover {
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

@media (max-width: 768px) {
  .data-table {
    font-size: 12px;
  }

  .data-table th,
  .data-table td {
    padding: 8px;
  }

  .dialog-content {
    padding: 20px;
  }
}
</style>
