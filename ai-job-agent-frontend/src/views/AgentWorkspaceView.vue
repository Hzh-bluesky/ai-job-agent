<template>
  <AppLayout>
    <section class="workspace-page agent-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">One click apply agent</p>
          <h1>Agent 工作台</h1>
          <p>选择一份简历和一个目标岗位，让 Agent 自动生成匹配报告、简历优化、打招呼话术、面试题，并创建投递记录。</p>
        </div>
        <el-tag size="large">Apply Plan</el-tag>
      </header>

      <section class="agent-command premium-card" v-loading="optionsLoading">
        <div class="section-title">
          <div>
            <h2>一键生成求职方案</h2>
            <p>适合面试演示的完整链路：从岗位评估到投递记录，集中完成一次求职准备。</p>
          </div>
        </div>

        <div v-if="!hasRequiredData && !optionsLoading" class="setup-hints">
          <button v-if="!resumes.length" type="button" class="setup-card" @click="router.push('/resumes')">
            <strong>还没有简历</strong>
            <span>先维护一份简历，Agent 才能理解你的项目经历和技术栈。</span>
          </button>
          <button v-if="!jobs.length" type="button" class="setup-card" @click="router.push('/jobs')">
            <strong>还没有岗位</strong>
            <span>先粘贴 JD 完成岗位分析，再生成完整求职方案。</span>
          </button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="selector-grid">
            <el-form-item label="选择简历" prop="resumeId">
              <el-select v-model="form.resumeId" filterable placeholder="选择用于生成方案的简历">
                <el-option
                  v-for="resume in resumes"
                  :key="resume.id"
                  :label="resumeOptionLabel(resume)"
                  :value="Number(resume.id)"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择岗位" prop="jobPostId">
              <el-select v-model="form.jobPostId" filterable placeholder="选择已经分析过的岗位">
                <el-option
                  v-for="job in jobs"
                  :key="job.id"
                  :label="jobOptionLabel(job)"
                  :value="Number(job.id)"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div class="command-actions">
          <el-button plain :disabled="!hasRequiredData || generating" @click="confirmRegenerate">
            重新生成
          </el-button>
          <el-button type="primary" :loading="generating" :disabled="!hasRequiredData || generating" @click="runAgent(false)">
            启动一键求职方案
          </el-button>
          <span>Agent 会串联已有能力，不会改变原有模块数据结构。</span>
        </div>
        <p v-if="generating" class="agent-waiting-tip">Agent 正在生成方案，可能需要 30-60 秒。</p>
      </section>

      <section class="agent-grid">
        <article class="steps-panel premium-card">
          <div class="section-title">
            <div>
              <h2>执行步骤</h2>
              <p>每一步都来自已有后端能力，工作台只负责统一编排和展示。</p>
            </div>
          </div>

          <div class="agent-steps">
            <div
              v-for="(step, index) in steps"
              :key="step.key"
              :class="['agent-step', getStepStatus(step, index)]"
            >
              <div class="step-index">{{ index + 1 }}</div>
              <div>
                <strong>{{ step.title }}</strong>
                <span>{{ step.description }}</span>
              </div>
              <el-tag size="small">{{ getStepLabel(step, index) }}</el-tag>
            </div>
          </div>
        </article>

        <article class="plan-summary premium-card" v-loading="generating">
          <template v-if="currentPlan">
            <div class="summary-head">
              <div>
                <span>综合匹配度</span>
                <strong>{{ currentPlan.matchReport?.overallScore ?? '-' }}</strong>
              </div>
              <el-tag :type="currentPlan.matchReport?.isRecommended === 1 ? 'success' : 'warning'" size="large">
                {{ currentPlan.matchReport?.isRecommended === 1 ? '建议投递' : '谨慎投递' }}
              </el-tag>
            </div>
            <p>{{ currentPlan.nextStepSuggestion }}</p>
          </template>

          <template v-else>
            <div class="summary-empty">
              <div class="empty-orb">AI</div>
              <strong>等待 Agent 生成方案</strong>
              <p>完成后，这里会展示最终判断、下一步建议，以及各模块的核心产物。</p>
            </div>
          </template>
        </article>
      </section>

      <section v-if="currentPlan" class="result-grid">
        <article class="result-card premium-card match-result">
          <span>匹配报告</span>
          <div class="result-score">
            <strong>{{ currentPlan.matchReport?.overallScore ?? 0 }}</strong>
            <small>/100</small>
          </div>
          <p>{{ currentPlan.matchReport?.suggestion || '暂无补强建议' }}</p>
          <button type="button" class="inline-link" @click="router.push('/match')">查看匹配报告</button>
        </article>

        <article class="result-card premium-card rewrite-result">
          <span>简历优化</span>
          <p>{{ currentPlan.resumeRewrite?.resumeVersion || currentPlan.resumeRewrite?.rewrittenProject || '暂无优化结果' }}</p>
          <button type="button" class="inline-link" @click="copyText(currentPlan.resumeRewrite?.resumeVersion)">复制简历版本</button>
        </article>

        <article class="result-card premium-card greeting-result">
          <span>打招呼话术</span>
          <p>{{ currentPlan.greeting?.greetingText || '暂无话术' }}</p>
          <button type="button" class="inline-link" @click="copyText(currentPlan.greeting?.greetingText)">复制话术</button>
        </article>

        <article class="result-card premium-card interview-result">
          <span>面试准备</span>
          <div class="question-metrics">
            <div>
              <strong>{{ questionCount(currentPlan.interviewQuestions?.technicalQuestions) }}</strong>
              <small>技术题</small>
            </div>
            <div>
              <strong>{{ questionCount(currentPlan.interviewQuestions?.projectQuestions) }}</strong>
              <small>项目题</small>
            </div>
            <div>
              <strong>{{ questionCount(currentPlan.interviewQuestions?.hrQuestions) }}</strong>
              <small>HR 题</small>
            </div>
          </div>
          <button type="button" class="inline-link" @click="router.push('/interviews')">进入面试准备</button>
        </article>

        <article class="result-card premium-card application-result">
          <span>投递记录</span>
          <h2>{{ currentPlan.applicationRecord?.companyName || '未识别公司' }}</h2>
          <p>{{ currentPlan.applicationRecord?.jobName || '未识别岗位' }}</p>
          <el-tag size="small">{{ statusLabel(currentPlan.applicationRecord?.status) }}</el-tag>
          <button type="button" class="inline-link" @click="router.push('/applications')">查看投递 Pipeline</button>
        </article>
      </section>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史方案</h2>
            <p>每次一键生成都会留下方案记录，方便回看完整求职准备链路。</p>
          </div>
          <el-button plain @click="loadHistory">刷新</el-button>
        </div>

        <div v-if="!historyLoading && plans.length === 0" class="history-empty">
          <strong>还没有一键求职方案</strong>
          <p>选择简历和岗位后启动 Agent，这里会形成你的方案记录。</p>
        </div>

        <div v-else class="plan-history">
          <article
            v-for="plan in plans"
            :key="plan.id"
            :class="['plan-history-card', { active: currentPlan?.id === plan.id }]"
            @click="loadPlanDetail(plan.id)"
          >
            <div>
              <strong>{{ getJobName(plan.jobPostId) }}</strong>
              <p>{{ getResumeName(plan.resumeId) }} · {{ formatDate(plan.createTime) }}</p>
            </div>
            <el-tag size="small">{{ plan.status || 'SUCCESS' }}</el-tag>
          </article>
        </div>
      </section>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../layouts/AppLayout.vue'
import { generateApplyPlan, getApplyPlanDetail, getApplyPlanPage } from '../api/applyPlan'
import { getJobPage } from '../api/job'
import { getResumePage } from '../api/resume'

const router = useRouter()
const formRef = ref()
const optionsLoading = ref(false)
const historyLoading = ref(false)
const generating = ref(false)
const activeStep = ref(-1)
const resumes = ref([])
const jobs = ref([])
const plans = ref([])
const currentPlan = ref(null)
let progressTimer = null

const form = reactive({
  resumeId: null,
  jobPostId: null
})

const rules = {
  resumeId: [{ required: true, message: '请选择简历', trigger: 'change' }],
  jobPostId: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

const steps = [
  { key: 'matchReport', title: '生成匹配报告', description: '评估综合匹配度、优势、不足与补强建议。' },
  { key: 'resumeRewrite', title: '优化项目经历', description: '基于岗位要求改写项目表达，保持真实不夸大。' },
  { key: 'greeting', title: '生成沟通话术', description: '输出适合 Boss 直聘直接发送的自然开场白。' },
  { key: 'interviewQuestions', title: '准备面试问题', description: '生成技术题、项目追问和 HR 问题回答思路。' },
  { key: 'applicationRecord', title: '创建投递记录', description: '把目标岗位保存到投递 Pipeline，默认未投递。' }
]

const hasRequiredData = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

onMounted(async () => {
  await Promise.all([loadOptions(), loadHistory()])
})

onBeforeUnmount(() => {
  stopProgress()
})

async function loadOptions() {
  optionsLoading.value = true
  try {
    const [resumePage, jobPage] = await Promise.all([
      getResumePage({ pageNo: 1, pageSize: 50 }),
      getJobPage({ pageNo: 1, pageSize: 50 })
    ])
    resumes.value = resumePage.records || []
    jobs.value = jobPage.records || []
    form.resumeId = toPositiveNumber(resumes.value.find((item) => item.isDefault === 1)?.id || resumes.value[0]?.id)
    form.jobPostId = toPositiveNumber(jobs.value[0]?.id)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '基础数据加载失败')
  } finally {
    optionsLoading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const page = await getApplyPlanPage({ pageNo: 1, pageSize: 10 })
    plans.value = page.records || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史方案加载失败')
  } finally {
    historyLoading.value = false
  }
}

async function runAgent(forceRegenerate = false) {
  const shouldForceRegenerate = forceRegenerate === true
  if (!hasRequiredData.value) {
    ElMessage.warning('请先准备简历和岗位')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const resumeId = toPositiveNumber(form.resumeId)
  const jobPostId = toPositiveNumber(form.jobPostId)
  if (!resumeId || !jobPostId) {
    ElMessage.warning('请选择有效的简历和岗位')
    return
  }

  generating.value = true
  currentPlan.value = null
  startProgress()
  try {
    const payload = {
      resumeId,
      jobPostId,
      forceRegenerate: shouldForceRegenerate
    }
    currentPlan.value = await generateApplyPlan(payload)
    activeStep.value = steps.length
    ElMessage.success(shouldForceRegenerate ? '一键求职方案已重新生成' : '一键求职方案已就绪')
    await loadHistory()
  } catch (error) {
    if (isTimeoutError(error)) {
      ElMessage.warning('Agent 执行时间较长，请稍后刷新历史方案查看结果')
      await loadHistory()
    } else {
      ElMessage.error(error.response?.data?.message || error.message || 'Agent 执行失败')
    }
  } finally {
    generating.value = false
    stopProgress()
  }
}

async function confirmRegenerate() {
  try {
    await ElMessageBox.confirm(
      '重新生成会创建新的匹配报告、简历优化、话术、面试题和投递记录，确定继续吗？',
      '确认重新生成',
      {
        confirmButtonText: '重新生成',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAgent(true)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.response?.data?.message || error.message || '重新生成失败')
    }
  }
}

function toPositiveNumber(value) {
  const numberValue = Number(value)
  return Number.isInteger(numberValue) && numberValue > 0 ? numberValue : null
}

function isTimeoutError(error) {
  return error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
}

async function loadPlanDetail(id) {
  historyLoading.value = true
  try {
    currentPlan.value = await getApplyPlanDetail(id)
    activeStep.value = steps.length
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '方案详情加载失败')
  } finally {
    historyLoading.value = false
  }
}

function startProgress() {
  stopProgress()
  activeStep.value = 0
  progressTimer = window.setInterval(() => {
    if (activeStep.value < steps.length - 1) {
      activeStep.value += 1
    }
  }, 900)
}

function stopProgress() {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = null
  }
}

function getStepStatus(step, index) {
  if (currentPlan.value?.[step.key]) return 'done'
  if (generating.value && index === activeStep.value) return 'running'
  if (generating.value && index < activeStep.value) return 'done'
  return 'waiting'
}

function getStepLabel(step, index) {
  const status = getStepStatus(step, index)
  if (status === 'done') return '已完成'
  if (status === 'running') return '执行中'
  return '等待'
}

function resumeOptionLabel(resume) {
  return `${resume.title || resume.name || '未命名简历'} · ${resume.school || '学校未填写'}`
}

function jobOptionLabel(job) {
  return `${job.companyName || '未识别公司'} · ${job.jobName || '未识别岗位'}`
}

function getResumeName(resumeId) {
  const resume = resumes.value.find((item) => item.id === resumeId)
  return resume?.title || resume?.name || `简历 #${resumeId}`
}

function getJobName(jobPostId) {
  const job = jobs.value.find((item) => item.id === jobPostId)
  return job?.jobName || `岗位 #${jobPostId}`
}

function formatDate(value) {
  if (!value) return '刚刚'
  return String(value).replace('T', ' ').slice(0, 16)
}

function questionCount(value) {
  const list = safeParseQuestions(value)
  return Array.isArray(list) ? list.length : 0
}

function safeParseQuestions(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

function statusLabel(status) {
  const map = {
    NOT_APPLIED: '未投递',
    APPLIED: '已投递',
    COMMUNICATING: '待沟通',
    INTERVIEWING: '面试中',
    REJECTED: '已拒绝',
    PASSED: '已通过'
  }
  return map[status] || '未投递'
}

async function copyText(text) {
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}
</script>

<style scoped>
.agent-command,
.steps-panel,
.plan-summary,
.history-panel,
.result-card {
  padding: 24px;
}

.selector-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.setup-hints {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.setup-card {
  text-align: left;
  border: 1px solid rgba(79, 110, 247, 0.16);
  border-radius: 18px;
  background: rgba(238, 242, 255, 0.7);
  color: var(--color-text);
  padding: 18px;
  cursor: pointer;
  transition: transform 180ms var(--ease-premium), border-color 180ms var(--ease-premium), box-shadow 180ms var(--ease-premium);
}

.setup-card:hover {
  transform: translateY(-2px);
  border-color: rgba(79, 110, 247, 0.28);
  box-shadow: var(--shadow-card);
}

.setup-card strong,
.setup-card span {
  display: block;
}

.setup-card span {
  margin-top: 8px;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.command-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 4px;
}

.command-actions span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.agent-waiting-tip {
  width: fit-content;
  margin: 14px 0 0;
  border: 1px solid rgba(79, 110, 247, 0.16);
  border-radius: 999px;
  background: rgba(238, 242, 255, 0.72);
  color: var(--color-brand);
  font-size: 13px;
  font-weight: 680;
  padding: 8px 12px;
}

.agent-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 22px;
}

.agent-steps {
  display: grid;
  gap: 12px;
}

.agent-step {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(251, 251, 249, 0.72);
  padding: 14px;
  transition: transform 180ms var(--ease-premium), border-color 180ms var(--ease-premium), background-color 180ms var(--ease-premium);
}

.agent-step.done {
  border-color: rgba(63, 143, 104, 0.22);
  background: rgba(240, 253, 244, 0.58);
}

.agent-step.running {
  border-color: rgba(79, 110, 247, 0.32);
  background: rgba(238, 242, 255, 0.72);
  transform: translateY(-1px);
}

.step-index {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: var(--color-brand);
  font-weight: 760;
  box-shadow: 0 8px 20px rgba(23, 25, 31, 0.055);
}

.agent-step strong,
.agent-step span {
  display: block;
}

.agent-step span {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.plan-summary {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: center;
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.summary-head span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 650;
}

.summary-head strong {
  display: block;
  margin-top: 8px;
  font-size: 72px;
  font-weight: 780;
  line-height: 0.95;
}

.plan-summary p {
  margin: 20px 0 0;
  color: var(--color-text-soft);
  font-size: 15px;
  line-height: 1.85;
}

.summary-empty {
  display: grid;
  justify-items: center;
  text-align: center;
  padding: 36px 18px;
}

.empty-orb {
  display: grid;
  width: 60px;
  height: 60px;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(145deg, var(--color-brand), var(--color-brand-deep));
  color: #fff;
  font-weight: 800;
  box-shadow: var(--shadow-button);
}

.summary-empty strong {
  margin-top: 18px;
  font-size: 18px;
}

.summary-empty p {
  max-width: 360px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.result-card {
  display: flex;
  min-height: 260px;
  flex-direction: column;
}

.result-card > span {
  color: var(--color-brand);
  font-size: 13px;
  font-weight: 720;
}

.result-card p {
  color: var(--color-text-soft);
  line-height: 1.78;
}

.result-card h2 {
  margin: 16px 0 8px;
  font-size: 22px;
}

.result-score {
  margin: 18px 0;
}

.result-score strong {
  font-size: 58px;
  line-height: 1;
}

.result-score small {
  color: var(--color-text-muted);
  font-weight: 650;
}

.question-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 22px 0;
}

.question-metrics div {
  border-radius: 14px;
  background: var(--color-brand-soft);
  padding: 14px 10px;
  text-align: center;
}

.question-metrics strong,
.question-metrics small {
  display: block;
}

.question-metrics strong {
  color: var(--color-brand);
  font-size: 24px;
}

.question-metrics small {
  margin-top: 4px;
  color: var(--color-text-soft);
}

.inline-link {
  width: fit-content;
  margin-top: auto;
  border: 0;
  background: transparent;
  color: var(--color-brand);
  font-weight: 720;
  cursor: pointer;
  padding: 0;
  transition: transform 160ms var(--ease-premium), color 160ms var(--ease-premium);
}

.inline-link:hover {
  transform: translateX(2px);
  color: var(--color-brand-deep);
}

.plan-history {
  display: grid;
  gap: 12px;
}

.plan-history-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(251, 251, 249, 0.72);
  padding: 16px;
  cursor: pointer;
  transition: transform 180ms var(--ease-premium), border-color 180ms var(--ease-premium), background-color 180ms var(--ease-premium);
}

.plan-history-card:hover,
.plan-history-card.active {
  transform: translateY(-1px);
  border-color: rgba(79, 110, 247, 0.24);
  background: #fff;
}

.plan-history-card strong,
.plan-history-card p {
  display: block;
  margin: 0;
}

.plan-history-card p {
  margin-top: 6px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.history-empty {
  border: 1px dashed var(--color-line-strong);
  border-radius: 18px;
  background: rgba(251, 251, 249, 0.72);
  padding: 28px;
  text-align: center;
}

.history-empty p {
  margin: 8px 0 0;
  color: var(--color-text-muted);
}

@media (max-width: 1180px) {
  .result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .agent-grid,
  .selector-grid,
  .setup-hints {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .command-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .result-grid {
    grid-template-columns: 1fr;
  }

  .agent-step {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .agent-step .el-tag {
    grid-column: 2;
    width: fit-content;
  }
}
</style>
