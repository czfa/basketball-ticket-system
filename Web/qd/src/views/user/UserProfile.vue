<template>
  <div class="user-profile-container">
    <div class="profile-header">
      <button class="back-btn" @click="goToMatches">← 返回篮球赛事</button>
      <h1 class="page-title">个人中心</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <div v-else class="profile-content">
      <!-- 个人信息卡片 -->
      <div class="profile-card">
        <h2 class="card-title">个人信息</h2>
        <form @submit.prevent="handleUpdateProfile" class="profile-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
              type="text"
              id="username"
              v-model="profile.username"
              disabled
              class="form-input disabled"
            />
            <span class="form-hint">用户名不可修改</span>
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
              type="email"
              id="email"
              v-model="profile.email"
              placeholder="请输入邮箱"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label for="phone">手机号</label>
            <input
              type="tel"
              id="phone"
              v-model="profile.phone"
              placeholder="请输入手机号"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label for="realName">真实姓名</label>
            <input
              type="text"
              id="realName"
              v-model="profile.realName"
              placeholder="请输入真实姓名"
              class="form-input"
            />
          </div>
          <div v-if="message" class="message" :class="{ error: isError, success: !isError }">
            {{ message }}
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </form>
      </div>

      <!-- 修改密码卡片 -->
      <div class="profile-card">
        <h2 class="card-title">修改密码</h2>
        <form @submit.prevent="handleChangePassword" class="profile-form">
          <div class="form-group">
            <label for="oldPassword">原密码</label>
            <input
              type="password"
              id="oldPassword"
              v-model="passwordForm.oldPassword"
              placeholder="请输入原密码"
              class="form-input"
              required
            />
          </div>
          <div class="form-group">
            <label for="newPassword">新密码</label>
            <input
              type="password"
              id="newPassword"
              v-model="passwordForm.newPassword"
              placeholder="请输入新密码（至少6位）"
              class="form-input"
              required
              minlength="6"
            />
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认新密码</label>
            <input
              type="password"
              id="confirmPassword"
              v-model="passwordForm.confirmPassword"
              placeholder="请再次输入新密码"
              class="form-input"
              required
            />
          </div>
          <div v-if="passwordMessage" class="message" :class="{ error: isPasswordError, success: !isPasswordError }">
            {{ passwordMessage }}
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="changingPassword">
              {{ changingPassword ? '修改中...' : '修改密码' }}
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
import { authAPI } from '@/api'

export default {
  name: 'UserProfile',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const saving = ref(false)
    const changingPassword = ref(false)
    const message = ref('')
    const isError = ref(false)
    const passwordMessage = ref('')
    const isPasswordError = ref(false)

    const profile = reactive({
      username: '',
      email: '',
      phone: '',
      realName: ''
    })

    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })

    /**
     * 返回篮球赛事页面
     */
    const goToMatches = () => {
      router.push({ name: 'MatchList' })
    }

    /**
     * 加载用户信息
     */
    const loadProfile = async () => {
      loading.value = true
      
      // 先检查 localStorage 中是否有用户信息
      const userInfoStr = localStorage.getItem('user_info')
      if (!userInfoStr) {
        // 如果没有用户信息，直接跳转到登录页
        alert('请先登录')
        router.push({ name: 'UserLogin', query: { redirect: '/profile' } })
        loading.value = false
        return
      }

      try {
        // 从 localStorage 获取用户信息（优先显示本地数据）
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo && (userInfo.userId || userInfo.user_id || userInfo.username)) {
          profile.username = userInfo.username || ''
          profile.email = userInfo.email || ''
          profile.phone = userInfo.phone || ''
          profile.realName = userInfo.realName || userInfo.real_name || ''
          
          // 如果有用户信息，即使后端 API 失败也不跳转
          // 尝试从后端获取最新信息（可选，失败不影响显示）
          try {
            const response = await authAPI.getProfile()
            if (response.success && response.user) {
              const data = response.user
              profile.email = data.email || profile.email
              profile.phone = data.phone || profile.phone
              profile.realName = data.realName || data.real_name || profile.realName
              // 更新 localStorage 中的信息
              const updatedUserInfo = { ...userInfo, ...data }
              localStorage.setItem('user_info', JSON.stringify(updatedUserInfo))
            }
          } catch (apiError) {
            // 后端 API 失败不影响显示，只记录错误
            console.warn('从后端获取最新信息失败，使用本地缓存:', apiError)
          }
        } else {
          // user_info 格式无效
          alert('用户信息格式错误，请重新登录')
          localStorage.removeItem('user_info')
          router.push({ name: 'UserLogin', query: { redirect: '/profile' } })
        }
      } catch (error) {
        console.error('解析用户信息失败:', error)
        // 解析失败，清除无效数据并跳转登录
        localStorage.removeItem('user_info')
        alert('请先登录')
        router.push({ name: 'UserLogin', query: { redirect: '/profile' } })
      } finally {
        loading.value = false
      }
    }

    /**
     * 更新个人信息
     */
    const handleUpdateProfile = async () => {
      saving.value = true
      message.value = ''
      isError.value = false

      try {
        const response = await authAPI.updateProfile({
          email: profile.email,
          phone: profile.phone,
          realName: profile.realName
        })

        if (response.success) {
          message.value = '个人信息更新成功'
          isError.value = false
          // 更新 localStorage 中的用户信息
          const userInfoStr = localStorage.getItem('user_info')
          if (userInfoStr) {
            const userInfo = JSON.parse(userInfoStr)
            userInfo.email = profile.email
            userInfo.phone = profile.phone
            userInfo.realName = profile.realName
            localStorage.setItem('user_info', JSON.stringify(userInfo))
          }
        } else {
          message.value = response.message || '更新失败'
          isError.value = true
        }
      } catch (error) {
        message.value = error.message || '更新失败，请重试'
        isError.value = true
      } finally {
        saving.value = false
      }
    }

    /**
     * 修改密码
     */
    const handleChangePassword = async () => {
      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        passwordMessage.value = '两次输入的密码不一致'
        isPasswordError.value = true
        return
      }

      if (passwordForm.newPassword.length < 6) {
        passwordMessage.value = '新密码长度至少为6位'
        isPasswordError.value = true
        return
      }

      changingPassword.value = true
      passwordMessage.value = ''
      isPasswordError.value = false

      try {
        const response = await authAPI.changePassword(
          passwordForm.oldPassword,
          passwordForm.newPassword
        )

        if (response.success) {
          passwordMessage.value = '密码修改成功，请重新登录'
          isPasswordError.value = false
          // 清空表单
          passwordForm.oldPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
          // 延迟跳转到登录页
          setTimeout(() => {
            router.push({ name: 'UserLogin' })
          }, 2000)
        } else {
          passwordMessage.value = response.message || '密码修改失败'
          isPasswordError.value = true
        }
      } catch (error) {
        passwordMessage.value = error.message || '密码修改失败，请重试'
        isPasswordError.value = true
      } finally {
        changingPassword.value = false
      }
    }

    onMounted(() => {
      loadProfile()
    })

    return {
      loading,
      saving,
      changingPassword,
      profile,
      passwordForm,
      message,
      isError,
      passwordMessage,
      isPasswordError,
      goToMatches,
      handleUpdateProfile,
      handleChangePassword
    }
  }
}
</script>

<style scoped>
.user-profile-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.profile-header {
  margin-bottom: 20px;
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

.profile-content {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 25px 0;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.form-input {
  padding: 10px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #409eff;
}

.form-input.disabled {
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
  border: 1px solid #fde2e2;
}

.message.success {
  background: #f0f9ff;
  color: #67c23a;
  border: 1px solid #b3e19d;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.btn {
  padding: 10px 25px;
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

.btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
  color: white;
}

@media (max-width: 768px) {
  .profile-content {
    padding: 0 10px;
  }

  .profile-card {
    padding: 20px;
  }
}
</style>
