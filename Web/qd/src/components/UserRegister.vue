<template>
  <div class="auth-container">
    <h2>用户注册</h2>
    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label for="reg-username">用户名</label>
        <input type="text" id="reg-username" v-model="form.username" required />
      </div>

      <div class="form-group">
        <label for="reg-email">邮箱</label>
        <input type="email" id="reg-email" v-model="form.email" required />
      </div>

      <div class="form-group">
        <label for="reg-password">密码</label>
        <input type="password" id="reg-password" v-model="form.password" required />
      </div>

      <div class="form-group">
        <label for="reg-confirm-password">确认密码</label>
        <input type="password" id="reg-confirm-password" v-model="form.confirmPassword" required />
      </div>

      <button type="submit" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <p v-if="message" :class="{ 'error': isError, 'success': !isError }">{{ message }}</p>
    </form>
    <div class="link">
      已有账号？<router-link to="/login">去登录</router-link>
    </div>
  </div>
</template>

<script>
// 假设使用 axios 作为 HTTP 客户端
import axios from 'axios';

export default {
  name: 'UserRegister',
  data() {
    return {
      form: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      loading: false,
      message: '',
      isError: false
    };
  },
  methods: {
    async handleRegister() {
      if (this.form.password !== this.form.confirmPassword) {
        this.message = '两次输入的密码不一致！';
        this.isError = true;
        return;
      }

      this.loading = true;
      this.message = '';
      this.isError = false;

      // 清除不必要的字段，只发送后端需要的字段
      const postData = {
        username: this.form.username,
        email: this.form.email,
        password: this.form.password
      };

      try {
        // 后端端口是 8088，请求 /api/auth/register
        const response = await axios.post('http://localhost:8088/api/auth/register', postData);

        this.message = response.data.message || '注册成功！';
        this.isError = false;

        // 注册成功后跳转到登录页
        this.$router.push('/login');

      } catch (error) {
        this.isError = true;
        // 从后端返回的错误信息中获取
        this.message = error.response?.data?.message || '注册失败，请检查网络或联系管理员。';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
/* 样式复用，可以参考上一回答中的基础 CSS */
.auth-container { max-width: 400px; margin: 50px auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
.form-group { margin-bottom: 15px; }
label { display: block; margin-bottom: 5px; font-weight: bold; }
input { width: 100%; padding: 10px; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
button { width: 100%; padding: 10px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
.error { color: red; }
.success { color: green; }
.link { text-align: center; margin-top: 15px; }
</style>