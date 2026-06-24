<template>
  <div class="register-container">
    <div class="register-box">
      <h2 class="register-title">用户注册</h2>
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="100px"
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名（3-20个字符）"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（6-20个字符）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱（可选）"
            clearable
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（可选）"
            clearable
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input
            v-model="registerForm.realName"
            placeholder="请输入真实姓名（可选）"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>
        <el-form-item>
          <div class="register-footer">
            <span>已有账号？</span>
            <el-link type="primary" @click="goToLogin">立即登录</el-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserRegister',
  setup() {
    const router = useRouter()
    const registerFormRef = ref(null)
    const loading = ref(false)

    // 注册表单数据
    const registerForm = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: '',
      realName: ''
    })

    // 自定义验证：确认密码
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    // 表单验证规则
    const registerRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ],
      email: [
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ]
    }

    /**
     * 处理用户注册
     */
    const handleRegister = async () => {
      if (!registerFormRef.value) return

      // 表单验证
      await registerFormRef.value.validate(async (valid) => {
        if (!valid) {
          return false
        }

        loading.value = true
        try {
          // 准备注册数据（不包含确认密码）
          const registerData = {
            username: registerForm.username,
            password: registerForm.password,
            email: registerForm.email || null,
            phone: registerForm.phone || null,
            realName: registerForm.realName || null
          }

          // 调用注册接口
          const response = await authAPI.register(registerData)

          if (response.success) {
            ElMessage.success(response.message || '注册成功，请登录')
            // 注册成功后跳转到登录页
            setTimeout(() => {
              router.push({ name: 'UserLogin' })
            }, 1500)
          } else {
            ElMessage.error(response.message || '注册失败')
          }
        } catch (error) {
          ElMessage.error(error.message || '注册失败，请稍后重试')
        } finally {
          loading.value = false
        }
      })
    }

    /**
     * 跳转到登录页
     */
    const goToLogin = () => {
      router.push({ name: 'UserLogin' })
    }

    return {
      registerFormRef,
      loading,
      registerForm,
      registerRules,
      handleRegister,
      goToLogin
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-box {
  width: 500px;
  max-width: 100%;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.register-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.register-form {
  margin-top: 20px;
}

.register-footer {
  width: 100%;
  text-align: center;
  color: #666;
}

.register-footer .el-link {
  margin-left: 5px;
}
</style>
