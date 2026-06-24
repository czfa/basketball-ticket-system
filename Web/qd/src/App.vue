<!-- E:/实训/项目/LangChain-ticket11/LangChain-ticket/Web/qd/src/App.vue -->
<template>
  <div id="app">
    <!-- 删除或注释掉导航菜单 -->
    <!--
    <nav>
      <router-link to="/">Home</router-link> |
      <router-link to="/about">About</router-link>
    </nav>
    -->
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 基础样式 */
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  height: 100vh;
}

/* 删除导航相关样式 */
/*
nav {
  padding: 30px;
}

nav a {
  font-weight: bold;
  color: #2c3e50;
}

nav a.router-link-exact-active {
  color: #42b983;
}
*/
</style>
