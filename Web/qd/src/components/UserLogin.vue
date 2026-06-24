<template>
  <div class="auth-container">
    <h2>用户登录</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="login-username">用户名</label>
        <input type="text" id="login-username" v-model="form.username" required />
      </div>

      <div class="form-group">
        <label for="login-password">密码</label>
        <input type="password" id="login-password" v-model="form.password" required />
      </div>

      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p v-if="message" :class="{ 'error': isError, 'success': !isError }">{{ message }}</p>
    </form>
    <div class="link">
      还没有账号？<router-link to="/register">去注册</router-link>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'UserLogin',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      loading: false,
      message: '',
      isError: false
    };
  },
  methods: {
    async handleLogin() {
      this.loading = true;
      this.message = '';
      this.isError = false;

      try {
        // 后端端口是 8088，请求 /api/auth/login
        const response = await axios.post('http://localhost:8088/api/auth/login', this.form);

        this.message = response.data.message || '登录成功！';
        this.isError = false;

        // 关键：登录成功后，后端会在浏览器中设置 Session Cookie。
        // Vue 客户端需要将用户数据存储到全局状态 (如 Vuex/Pinia) 或 LocalStorage 中。
        // 示例：将用户信息存储到 LocalStorage
        localStorage.setItem('user_info', JSON.stringify(response.data.user_info));


        // 登录成功后跳转到首页
        this.$router.push('/index');

      } catch (error) {
        this.isError = true;
        // 从后端返回的错误信息中获取
        this.message = error.response?.data?.message || '登录失败，请检查用户名和密码。';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
/* 样式同上，保持一致 */
.auth-container { max-width: 400px; margin: 50px auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
.form-group { margin-bottom: 15px; }
label { display: block; margin-bottom: 5px; font-weight: bold; }
input { width: 100%; padding: 10px; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
button { width: 100%; padding: 10px; background-color: #28a745; color: white; border: none; border-radius: 4px; cursor: pointer; }
.error { color: red; }
.success { color: green; }
.link { text-align: center; margin-top: 15px; }
</style>