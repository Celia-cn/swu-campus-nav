<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerAdmin } from '@/api/admin'
import { setToken } from '@/api/http'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { show } = useToast()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function handleRegister() {
  const name = username.value.trim()
  if (!name || !password.value) {
    show('请输入账号和密码', 'error')
    return
  }
  if (password.value.length < 6) {
    show('密码至少 6 位', 'error')
    return
  }
  if (password.value !== confirmPassword.value) {
    show('两次输入的密码不一致', 'error')
    return
  }
  loading.value = true
  try {
    const auth = await registerAdmin(name, password.value)
    if (auth.user.role !== 'ADMIN') {
      show('注册失败：未获得管理员角色', 'error')
      return
    }
    setToken(auth.token)
    show('管理员注册成功')
    await router.replace('/dashboard')
  } catch (e) {
    const msg = e instanceof Error ? e.message : '注册失败'
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
      <span class="logo-bubbles" aria-hidden="true" />
      <h1>注册管理员</h1>
      <p class="subtitle">西南大学智畅游 · 管理后台</p>

      <form @submit.prevent="handleRegister">
        <div class="field">
          <label for="username">管理员账号</label>
          <input id="username" v-model="username" autocomplete="username" placeholder="3–50 个字符" />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="new-password"
            placeholder="至少 6 位"
          />
        </div>
        <div class="field">
          <label for="confirmPassword">确认密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="再次输入密码"
          />
        </div>
        <button class="btn btn-primary login-btn" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注册并登录' }}
        </button>
      </form>

      <p class="register-link">
        已有账号？
        <router-link to="/login">返回登录</router-link>
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
  margin: 20px 0 0;
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
