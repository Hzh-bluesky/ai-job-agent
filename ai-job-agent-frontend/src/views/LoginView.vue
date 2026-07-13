<template>
  <main class="login-page">
    <section class="brand-panel">
      <BrandMark />

      <div class="hero-copy">
        <p class="eyebrow">AI career workspace</p>
        <h1>让每一次投递，都更像一次有准备的选择。</h1>
        <p>
          粘贴岗位 JD，连接你的简历项目，生成岗位洞察、匹配报告、项目优化、沟通话术和面试准备。
        </p>
      </div>

      <div class="preview-board premium-card">
        <div class="preview-header">
          <span>Job insight</span>
          <el-tag size="small">78% match</el-tag>
        </div>
        <div class="score-line">
          <strong>AI Agent 应用开发实习生</strong>
          <span>Java · Spring Boot · LLM · Vue3</span>
        </div>
        <div class="mini-grid">
          <div>
            <span>岗位分析</span>
            <strong>已解析</strong>
          </div>
          <div>
            <span>简历优化</span>
            <strong>3 条建议</strong>
          </div>
          <div>
            <span>面试准备</span>
            <strong>20 题</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="form-panel premium-card">
      <div class="form-head">
        <p class="eyebrow">Welcome back</p>
        <h2>登录工作台</h2>
        <p>登录后继续分析岗位、优化简历与准备面试。</p>
      </div>

      <div class="form-note">
        <span>Secure session</span>
        <p>登录成功后会保存 JWT，后续请求将自动携带身份信息。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="remember">保持登录</el-checkbox>
          <button type="button">忘记密码</button>
        </div>

        <el-button class="login-cta hover-arrow" type="primary" size="large" :loading="submitting" @click="handleLogin">
          <span>进入 AI 求职工作台</span>
          <ArrowRight class="icon-arrow" />
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import BrandMark from '../components/BrandMark.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const remember = ref(true)
const submitting = ref(false)

const form = reactive({
  username: 'student01',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名或密码')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.72fr);
  gap: 36px;
  min-height: 100vh;
  padding: 42px;
}

.brand-panel {
  display: flex;
  min-height: calc(100vh - 84px);
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  border: 1px solid var(--color-line);
  border-radius: 28px;
  background:
    linear-gradient(140deg, rgba(255, 255, 255, 0.82), rgba(238, 242, 255, 0.36)),
    var(--color-surface-soft);
  padding: 34px;
}

.hero-copy {
  max-width: 720px;
  padding: 90px 0 56px;
}

.hero-copy h1 {
  max-width: 680px;
  margin: 18px 0 0;
  font-size: clamp(39px, 5.55vw, 72px);
  font-weight: 760;
  letter-spacing: 0;
  line-height: 1.075;
}

.hero-copy > p:last-child {
  max-width: 600px;
  margin: 24px 0 0;
  color: var(--color-text-soft);
  font-size: 18px;
  line-height: 1.8;
}

.preview-board {
  max-width: 680px;
  padding: 24px;
  backdrop-filter: blur(18px);
}

.preview-header,
.score-line,
.mini-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.preview-header {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 650;
  text-transform: uppercase;
}

.score-line {
  margin-top: 22px;
}

.score-line strong {
  font-size: 22px;
}

.score-line span {
  color: var(--color-text-soft);
}

.mini-grid {
  margin-top: 24px;
}

.mini-grid div {
  flex: 1;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: #fff;
  padding: 16px;
}

.mini-grid span,
.mini-grid strong {
  display: block;
}

.mini-grid span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.mini-grid strong {
  margin-top: 8px;
  font-size: 17px;
}

.form-panel {
  align-self: center;
  padding: 36px;
}

.form-head {
  margin-bottom: 18px;
}

.form-head h2 {
  margin: 14px 0 0;
  font-size: 32px;
  letter-spacing: 0;
}

.form-head p:last-child {
  margin: 10px 0 0;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.form-note {
  margin-bottom: 26px;
  border: 1px solid rgba(79, 110, 247, 0.12);
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(238, 242, 255, 0.72), rgba(255, 255, 255, 0.78));
  padding: 14px 15px;
}

.form-note span {
  display: block;
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 760;
  letter-spacing: 0.01em;
}

.form-note p {
  margin: 6px 0 0;
  color: var(--color-text-soft);
  font-size: 13px;
  line-height: 1.65;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 24px;
}

.form-options button {
  border: 0;
  background: transparent;
  color: var(--color-brand);
  cursor: pointer;
  font-weight: 650;
}

.form-panel .el-button {
  width: 100%;
}

.login-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 46px;
  box-shadow: 0 14px 30px rgba(79, 110, 247, 0.18);
}

.login-cta:hover {
  box-shadow: 0 18px 38px rgba(79, 110, 247, 0.24);
}

.login-cta :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.login-cta .icon-arrow {
  width: 16px;
  height: 16px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .brand-panel {
    min-height: auto;
  }

  .hero-copy {
    padding: 70px 0 34px;
  }

  .form-panel {
    align-self: stretch;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px;
  }

  .brand-panel,
  .form-panel {
    border-radius: 20px;
    padding: 22px;
  }

  .mini-grid {
    display: grid;
  }
}
</style>
