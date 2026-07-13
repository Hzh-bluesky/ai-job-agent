import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import ResumeView from '../views/ResumeView.vue'
import JobAnalyzeView from '../views/JobAnalyzeView.vue'
import MatchReportView from '../views/MatchReportView.vue'
import ResumeRewriteView from '../views/ResumeRewriteView.vue'
import GreetingView from '../views/GreetingView.vue'
import InterviewView from '../views/InterviewView.vue'
import ApplicationRecordView from '../views/ApplicationRecordView.vue'
import AgentWorkspaceView from '../views/AgentWorkspaceView.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      public: true
    }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardView
  },
  {
    path: '/agent-workspace',
    name: 'agentWorkspace',
    component: AgentWorkspaceView
  },
  {
    path: '/resumes',
    name: 'resumes',
    component: ResumeView
  },
  {
    path: '/jobs',
    name: 'jobs',
    component: JobAnalyzeView
  },
  {
    path: '/match',
    name: 'match',
    component: MatchReportView
  },
  {
    path: '/resume-rewrite',
    name: 'resumeRewrite',
    component: ResumeRewriteView
  },
  {
    path: '/greetings',
    name: 'greetings',
    component: GreetingView
  },
  {
    path: '/interviews',
    name: 'interviews',
    component: InterviewView
  },
  {
    path: '/applications',
    name: 'applications',
    component: ApplicationRecordView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && authStore.isLoggedIn) {
    return '/dashboard'
  }
  return true
})

export default router
