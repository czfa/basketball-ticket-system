<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">用户登录</h2>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <div class="login-footer">
            <span>还没有账号？</span>
            <el-link type="primary" @click="goToRegister">立即注册</el-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserLogin',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loginFormRef = ref(null)
    const loading = ref(false)

    // 登录表单数据
    const loginForm = reactive({
      username: '',
      password: ''
    })

    // 表单验证规则
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ]
    }

    /**
     * 处理用户登录
     */
    const handleLogin = async () => {
      if (!loginFormRef.value) return

      // 表单验证
      await loginFormRef.value.validate(async (valid) => {
        if (!valid) {
          return false
        }

        loading.value = true
        try {
          // 调用登录接口
          const response = await authAPI.login(loginForm.username, loginForm.password)

          if (response.success) {
            // 登录成功，保存用户信息到 localStorage
            localStorage.setItem('user_info', JSON.stringify(response.user_info))
            
            ElMessage.success(response.message || '登录成功')

            // 跳转到原本要访问的页面，或默认跳转到赛事列表
            const redirect = route.query.redirect || '/matches'
            router.push(redirect)
          } else {
            ElMessage.error(response.message || '登录失败')
          }
        } catch (error) {
          ElMessage.error(error.message || '登录失败，请检查用户名和密码')
        } finally {
          loading.value = false
        }
      })
    }

    /**
     * 跳转到注册页
     */
    const goToRegister = () => {
      router.push({ name: 'UserRegister' })
    }

    return {
      loginFormRef,
      loading,
      loginForm,
      loginRules,
      handleLogin,
      goToRegister
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.login-form {
  margin-top: 20px;
}

.login-footer {
  width: 100%;
  text-align: center;
  color: #666;
}

.login-footer .el-link {
  margin-left: 5px;
}
</style>
