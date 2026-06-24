<template>
  <div class="admin-login-container">
    <div class="admin-login-box">
      <h2 class="login-title">管理员登录</h2>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="admin-username">用户名</label>
          <input
            type="text"
            id="admin-username"
            v-model="form.username"
            placeholder="请输入管理员用户名"
            required
          />
        </div>
        <div class="form-group">
          <label for="admin-password">密码</label>
          <input
            type="password"
            id="admin-password"
            v-model="form.password"
            placeholder="请输入密码"
            required
          />
        </div>
        <div v-if="message" class="message" :class="{ error: isError, success: !isError }">
          {{ message }}
        </div>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <div class="login-footer">
        <router-link to="/login">普通用户登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { adminAPI } from '@/api'

export default {
  name: 'AdminLogin',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const message = ref('')
    const isError = ref(false)
    const form = reactive({
      username: '',
      password: ''
    })

    /**
     * 处理管理员登录
     */
    const handleLogin = async () => {
      loading.value = true
      message.value = ''
      isError.value = false

      try {
        const response = await adminAPI.login(form.username, form.password)
        if (response.success) {
          // 保存管理员信息到 localStorage
          localStorage.setItem('admin_info', JSON.stringify(response.admin_info || {}))
          message.value = '登录成功'
          isError.value = false

          // 跳转到管理后台首页
          const redirect = route.query.redirect || '/admin'
          setTimeout(() => {
            router.push(redirect)
          }, 1000)
        } else {
          message.value = response.message || '登录失败'
          isError.value = true
        }
      } catch (error) {
        message.value = error.message || '登录失败，请检查用户名和密码'
        isError.value = true
      } finally {
        loading.value = false
      }
    }

    return {
      loading,
      message,
      isError,
      form,
      handleLogin
    }
  }
}
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.admin-login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.login-form {
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #606266;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #409eff;
}

.message {
  margin-bottom: 15px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

.message.error {
  background: #fef0f0;
  color: #f56c6c;
}

.message.success {
  background: #f0f9ff;
  color: #67c23a;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.login-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.login-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.login-footer a {
  color: #409eff;
  text-decoration: none;
}

.login-footer a:hover {
  text-decoration: underline;
}
</style>
