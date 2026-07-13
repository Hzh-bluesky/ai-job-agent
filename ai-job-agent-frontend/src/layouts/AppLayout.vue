<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <BrandMark />
        <span>Agent workspace</span>
      </div>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.label"
          class="nav-item"
          :class="{ active: item.path === $route.path }"
          :to="item.path"
        >
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <div class="user-chip">
          <div class="avatar">{{ userInitial }}</div>
          <div>
            <strong>{{ authStore.user?.nickname || '求职者' }}</strong>
            <span>Preview workspace</span>
          </div>
        </div>
        <el-button plain :icon="SwitchButton" @click="handleLogout">退出</el-button>
      </div>
    </aside>

    <main class="main-stage">
      <div class="stage-frame page-motion">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  Document,
  Grid,
  MagicStick,
  Memo,
  Promotion,
  Reading,
  Search,
  SwitchButton
} from '@element-plus/icons-vue'
import BrandMark from '../components/BrandMark.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const navItems = [
  { label: '工作台', path: '/dashboard', icon: Grid },
  { label: 'Agent工作台', path: '/agent-workspace', icon: MagicStick },
  { label: '我的简历', path: '/resumes', icon: Document },
  { label: '岗位分析', path: '/jobs', icon: Search },
  { label: '匹配报告', path: '/match', icon: Memo },
  { label: '简历优化', path: '/resume-rewrite', icon: MagicStick },
  { label: '打招呼话术', path: '/greetings', icon: ChatDotRound },
  { label: '面试准备', path: '/interviews', icon: Reading },
  { label: '投递记录', path: '/applications', icon: Promotion }
]

const userInitial = computed(() => {
  const name = authStore.user?.nickname || authStore.user?.username || 'A'
  return name.slice(0, 1).toUpperCase()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  display: grid;
  grid-template-columns: 292px 1fr;
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.52), rgba(247, 247, 244, 0.94)),
    var(--color-bg);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 28px;
  border-right: 1px solid rgba(230, 231, 235, 0.82);
  background: rgba(255, 255, 255, 0.7);
  padding: 28px;
  backdrop-filter: blur(22px);
}

.sidebar-brand {
  display: grid;
  gap: 12px;
}

.sidebar-brand > span {
  width: fit-content;
  border: 1px solid rgba(79, 110, 247, 0.14);
  border-radius: 999px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 680;
  padding: 6px 10px;
}

.nav-list {
  display: grid;
  gap: 8px;
}

.nav-item {
  display: flex;
  position: relative;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  border-radius: 14px;
  color: var(--color-text-soft);
  font-weight: 650;
  padding: 0 13px;
  transition: background-color 160ms var(--ease-premium), color 160ms var(--ease-premium), transform 160ms var(--ease-premium);
}

.nav-item :deep(svg) {
  width: 18px;
  height: 18px;
}

.nav-item.active,
.nav-item:hover {
  background: rgba(238, 242, 255, 0.9);
  color: var(--color-brand);
}

.nav-item:hover {
  transform: translateX(2px);
}

.sidebar-foot {
  display: grid;
  gap: 14px;
  margin-top: auto;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
  box-shadow: 0 10px 26px rgba(23, 25, 31, 0.045);
}

.avatar {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(145deg, var(--color-brand), var(--color-brand-deep));
  color: #fff;
  font-weight: 800;
}

.user-chip strong,
.user-chip span {
  display: block;
}

.user-chip strong {
  font-size: 14px;
}

.user-chip span {
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.main-stage {
  min-width: 0;
  padding: 34px;
}

.stage-frame {
  min-height: calc(100vh - 68px);
}

@media (max-width: 980px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
    padding: 20px;
  }

  .nav-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-stage {
    padding: 20px;
  }
}

@media (max-width: 640px) {
  .nav-list {
    grid-template-columns: 1fr;
  }
}
</style>
