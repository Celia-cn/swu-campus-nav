<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/admin'
import { setToken } from '@/api/http'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const { show } = useToast()

const username = ref('admin')
const password = ref('')
const loading = ref(false)

onMounted(() => {
  if (route.query.error === 'admin' || route.query.error === 'user') {
    show('普通用户无法登录管理后台，请使用手机 App', 'error')
  }
})

async function handleLogin() {
  if (!username.value.trim() || !password.value) {
    show('请输入账号和密码', 'error')
    return
  }
  loading.value = true
  try {
    const auth = await login(username.value.trim(), password.value)
    if (auth.user.role !== 'ADMIN') {
      show('普通用户无法登录管理后台，请使用手机 App', 'error')
      return
    }
    setToken(auth.token)
    show('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '登录失败'
    show(msg, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="decor decor-yellow" />
    <div class="decor decor-cyan" />

    <div class="login-card card">
      <!-- logo -->
      <span class="logo-bubbles" aria-hidden="true" />
      <h1>您好，欢迎登录！</h1>
      <p class="subtitle">西南大学智畅游 · 管理后台</p>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="username">账号</label>
          <input id="username" v-model="username" autocomplete="username" placeholder="管理员账号" />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="密码"
          />
        </div>
        <button class="btn btn-primary login-btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <p class="register-link">
        没有管理员账号？
        <router-link to="/register">注册管理员</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, var(--brand-gradient-start) 0%, var(--brand-gradient-mid) 45%, var(--brand-gradient-end) 100%);
}

.decor {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.decor-yellow {
  width: 220px;
  height: 220px;
  top: -70px;
  right: -60px;
  background: rgba(253, 224, 141, 0.55);
}

.decor-cyan {
  width: 120px;
  height: 120px;
  top: 36px;
  left: -30px;
  background: rgba(204, 245, 248, 0.45);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 36px 32px 28px;
}

.logo-bubbles {
  width: 80px;
  height: 52px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, var(--brand-accent) 40%, var(--brand-gradient-end) 100%);
  border-radius: 26px 26px 8px 26px;
  opacity: 0.9;
}

h1 {
  margin: 0;
  font-size: 26px;
}

.subtitle {
  margin: 8px 0 28px;
  color: var(--text-sub);
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
}

.register-link {
  margin: 16px 0 0;
  font-size: 13px;
  color: var(--text-sub);
  text-align: center;
}

.register-link a {
  color: var(--brand-accent-dark);
  font-weight: 600;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
