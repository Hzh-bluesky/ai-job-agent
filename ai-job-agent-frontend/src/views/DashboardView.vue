<template>
  <AppLayout>
    <section class="dashboard">
      <div class="dashboard-hero">
        <div>
          <p class="eyebrow">AI Job Agent</p>
          <h1>让 AI 帮你更聪明地求职</h1>
          <p>
            从岗位 JD 到简历表达、沟通话术和面试准备，把投递前最耗心力的判断交给一个清晰、克制、可追踪的 AI 工作台。
          </p>
        </div>
        <div class="hero-panel premium-card">
          <div class="panel-kicker">Next best action</div>
          <strong>先解析一个目标岗位</strong>
          <p>用 JD 洞察驱动后续匹配、优化、话术生成、面试准备和投递记录。</p>
          <div class="hero-actions">
            <el-button type="primary" :icon="Plus" @click="router.push('/jobs')">新增岗位分析</el-button>
            <el-button plain :icon="Document" @click="router.push('/resumes')">管理简历</el-button>
          </div>
        </div>
      </div>

      <div class="status-row">
        <article v-for="item in stats" :key="item.label" class="status-card premium-card">
          <div class="status-meta">
            <span>{{ item.label }}</span>
            <em>{{ item.delta }}</em>
          </div>
          <strong>{{ item.value }}</strong>
          <small>{{ item.caption }}</small>
        </article>
      </div>

      <div class="section-head">
        <div>
          <span>Core workflow</span>
          <h2>从岗位理解到面试准备</h2>
        </div>
        <p>围绕真实求职动作组织信息，而不是堆叠后台菜单。</p>
      </div>

      <div class="feature-grid">
        <FeatureCard
          v-for="feature in features"
          :key="feature.title"
          :icon="feature.icon"
          :title="feature.title"
          :description="feature.description"
          :tag="feature.tag"
          :to="feature.path"
        />
      </div>

      <section class="insight-panel premium-card">
        <div class="insight-copy">
          <p class="eyebrow">Today’s focus</p>
          <h2>先把岗位读懂，再决定怎么表达自己。</h2>
          <p>
            这个产品的界面不是堆功能，而是围绕求职者的实际路径组织：粘贴 JD、选择简历、获得判断、生成表达、准备面试、保存投递进度。
          </p>
        </div>
        <div class="timeline">
          <div v-for="step in workflow" :key="step.title" class="timeline-item">
            <span>{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <p>{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </section>
    </section>
  </AppLayout>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  DataAnalysis,
  Document,
  MagicStick,
  Memo,
  Plus,
  Promotion,
  Reading
} from '@element-plus/icons-vue'
import AppLayout from '../layouts/AppLayout.vue'
import FeatureCard from '../components/FeatureCard.vue'

const router = useRouter()

const stats = [
  { label: '简历管理', value: '多份', caption: '支持默认简历与数据隔离', delta: 'Ready' },
  { label: 'AI 能力', value: '5项', caption: '分析、匹配、优化、话术、面试', delta: 'Agent' },
  { label: '投递流程', value: '闭环', caption: '从岗位洞察到状态管理', delta: 'Pipeline' }
]

const features = [
  {
    title: '岗位分析',
    description: '粘贴 JD，自动提取公司、岗位、技术栈、职责、加分项和风险点。',
    tag: 'Insight',
    icon: DataAnalysis,
    path: '/jobs'
  },
  {
    title: '简历匹配',
    description: '选择简历和岗位，生成综合评分、优势、不足和补强建议。',
    tag: 'Score',
    icon: Memo,
    path: '/match'
  },
  {
    title: '简历优化',
    description: '基于已有项目经历做表达优化，突出技术栈、职责边界和业务价值。',
    tag: 'Rewrite',
    icon: MagicStick,
    path: '/resume-rewrite'
  },
  {
    title: '打招呼话术',
    description: '生成自然、不夸张、适合直接发给招聘者的 Boss 直聘开场话术。',
    tag: 'Prompt',
    icon: ChatDotRound,
    path: '/greetings'
  },
  {
    title: '面试准备',
    description: '围绕岗位和项目生成技术题、项目追问、HR 问题和回答思路。',
    tag: 'Interview',
    icon: Reading,
    path: '/interviews'
  },
  {
    title: '投递记录',
    description: '保存投递状态、匹配分数和备注，跟踪从未投递到面试中的完整进展。',
    tag: 'Pipeline',
    icon: Promotion,
    path: '/applications'
  }
]

const workflow = [
  { index: '01', title: '读取岗位', desc: '把非结构化 JD 转成可判断的岗位信息。' },
  { index: '02', title: '匹配简历', desc: '用评分和分析帮助用户判断是否值得投递。' },
  { index: '03', title: '优化表达', desc: '只基于已有经历做更准确、更岗位化的描述。' },
  { index: '04', title: '准备沟通', desc: '生成话术、面试题和投递记录，让投递动作更完整。' }
]
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 30px;
  max-width: 1240px;
  margin: 0 auto;
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 30px;
  align-items: end;
  padding: 22px 2px 8px;
}

.dashboard-hero h1 {
  max-width: 780px;
  margin: 16px 0 0;
  font-size: clamp(40px, 4.7vw, 62px);
  font-weight: 760;
  letter-spacing: 0;
  line-height: 1.08;
}

.dashboard-hero p:last-child {
  max-width: 720px;
  margin: 20px 0 0;
  color: var(--color-text-soft);
  font-size: 17px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.hero-panel {
  align-self: stretch;
  padding: 22px;
}

.panel-kicker {
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 760;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.hero-panel strong {
  display: block;
  margin-top: 14px;
  font-size: 22px;
  line-height: 1.25;
}

.hero-panel p {
  margin: 10px 0 22px;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.status-card {
  padding: 20px;
}

.status-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-card span,
.status-card small {
  color: var(--color-text-muted);
}

.status-card span {
  display: block;
  font-size: 13px;
  font-weight: 650;
}

.status-card em {
  border-radius: 999px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 11px;
  font-style: normal;
  font-weight: 760;
  padding: 5px 8px;
}

.status-card strong {
  display: block;
  margin-top: 12px;
  font-size: 36px;
  font-weight: 760;
  letter-spacing: 0;
}

.status-card small {
  display: block;
  margin-top: 8px;
  font-size: 13px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-top: 10px;
}

.section-head span {
  color: var(--color-brand);
  font-size: 13px;
  font-weight: 750;
  text-transform: uppercase;
}

.section-head h2 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: 0;
}

.section-head p {
  max-width: 420px;
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.feature-grid :deep(.feature-card:first-child) {
  grid-column: span 2;
}

.insight-panel {
  display: grid;
  grid-template-columns: 0.8fr 1fr;
  gap: 36px;
  padding: 30px;
}

.insight-copy h2 {
  max-width: 460px;
  margin: 16px 0 0;
  font-size: 34px;
  line-height: 1.15;
  letter-spacing: 0;
}

.insight-copy p:last-child {
  max-width: 520px;
  margin: 18px 0 0;
  color: var(--color-text-soft);
  line-height: 1.8;
}

.timeline {
  display: grid;
  gap: 12px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 14px;
  align-items: start;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(251, 251, 249, 0.86);
  padding: 16px;
  transition: border-color 170ms var(--ease-premium), transform 170ms var(--ease-premium), background-color 170ms var(--ease-premium);
}

.timeline-item:hover {
  border-color: rgba(79, 110, 247, 0.2);
  background: #fff;
  transform: translateY(-1px);
}

.timeline-item > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 12px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 800;
}

.timeline-item strong {
  display: block;
  font-size: 16px;
}

.timeline-item p {
  margin: 6px 0 0;
  color: var(--color-text-soft);
  line-height: 1.65;
}

@media (max-width: 980px) {
  .dashboard-hero,
  .section-head,
  .insight-panel {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .section-head {
    align-items: start;
  }

  .status-row,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .feature-grid :deep(.feature-card:first-child) {
    grid-column: auto;
  }
}

@media (max-width: 640px) {
  .hero-actions {
    flex-direction: column;
  }

  .hero-actions .el-button {
    width: 100%;
  }
}
</style>
