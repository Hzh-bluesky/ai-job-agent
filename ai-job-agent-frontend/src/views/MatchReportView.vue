<template>
  <AppLayout>
    <section class="workspace-page match-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">AI match report</p>
          <h1>匹配报告</h1>
          <p>选择简历和岗位，让 Agent 评估匹配度、优势、不足与补强建议。</p>
        </div>
        <el-tag size="large">AI Match Report</el-tag>
      </header>

      <section class="control-panel premium-card" v-loading="optionsLoading">
        <div class="section-title">
          <div>
            <h2>生成评估</h2>
            <p>先选择一份简历和一个已分析岗位，Agent 会生成结构化匹配报告。</p>
          </div>
        </div>

        <div v-if="!hasRequiredData && !optionsLoading" class="setup-hints">
          <button v-if="!resumes.length" type="button" class="setup-card" @click="router.push('/resumes')">
            <strong>还没有简历</strong>
            <span>先创建一份简历，才能进行岗位匹配。</span>
          </button>
          <button v-if="!jobs.length" type="button" class="setup-card" @click="router.push('/jobs')">
            <strong>还没有岗位</strong>
            <span>先粘贴 JD 并完成岗位分析。</span>
          </button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="selector-grid">
            <el-form-item label="选择简历" prop="resumeId">
              <el-select v-model="form.resumeId" filterable placeholder="选择用于匹配的简历">
                <el-option
                  v-for="resume in resumes"
                  :key="resume.id"
                  :label="resumeOptionLabel(resume)"
                  :value="resume.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择岗位" prop="jobPostId">
              <el-select v-model="form.jobPostId" filterable placeholder="选择已分析的岗位">
                <el-option v-for="job in jobs" :key="job.id" :label="jobOptionLabel(job)" :value="job.id" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div class="control-actions">
          <el-button type="primary" :loading="generating" :disabled="!hasRequiredData" @click="generateReport">
            生成匹配报告
          </el-button>
        </div>
      </section>

      <section class="report-stage" v-loading="generating || detailLoading">
        <article v-if="currentReport" class="score-hero premium-card">
          <div class="score-copy">
            <span>综合匹配度</span>
            <strong>{{ currentReport.overallScore ?? 0 }}</strong>
            <p>{{ scoreSummary }}</p>
          </div>
          <div class="score-orb" :style="scoreRingStyle(currentReport.overallScore)">
            <span>{{ currentReport.overallScore ?? 0 }}</span>
            <small>/100</small>
          </div>
          <el-tag :type="currentReport.isRecommended === 1 ? 'success' : 'warning'" size="large">
            {{ currentReport.isRecommended === 1 ? '建议投递' : '谨慎投递' }}
          </el-tag>
        </article>

        <article v-else class="report-empty premium-card">
          <div class="empty-orb">AI</div>
          <strong>等待生成匹配报告</strong>
          <p>选择简历和岗位后，综合分数、优势、不足和补强建议会展示在这里。</p>
        </article>

        <div v-if="currentReport" class="score-breakdown">
          <article class="score-card">
            <span>技术栈匹配</span>
            <strong>{{ currentReport.techScore ?? 0 }}</strong>
            <div class="score-bar">
              <i :style="{ width: `${currentReport.techScore || 0}%` }"></i>
            </div>
          </article>
          <article class="score-card">
            <span>项目经历匹配</span>
            <strong>{{ currentReport.projectScore ?? 0 }}</strong>
            <div class="score-bar">
              <i :style="{ width: `${currentReport.projectScore || 0}%` }"></i>
            </div>
          </article>
          <article class="score-card">
            <span>学历 / 年级匹配</span>
            <strong>{{ currentReport.educationScore ?? 0 }}</strong>
            <div class="score-bar">
              <i :style="{ width: `${currentReport.educationScore || 0}%` }"></i>
            </div>
          </article>
        </div>

        <div v-if="currentReport" class="analysis-grid">
          <article class="analysis-card positive">
            <span>优势分析</span>
            <p>{{ currentReport.advantageAnalysis || '暂无优势分析' }}</p>
          </article>
          <article class="analysis-card warning">
            <span>不足分析</span>
            <p>{{ currentReport.weaknessAnalysis || '暂无不足分析' }}</p>
          </article>
          <article class="analysis-card suggestion">
            <span>补强建议</span>
            <p>{{ currentReport.suggestion || '暂无补强建议' }}</p>
          </article>
        </div>
      </section>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史报告</h2>
            <p>回看已经生成的匹配评估，比较不同简历和岗位的适配情况。</p>
          </div>
        </div>

        <div v-if="!historyLoading && reports.length === 0" class="history-empty">
          <strong>还没有匹配报告</strong>
          <p>生成第一份报告后，这里会形成你的投递判断记录。</p>
        </div>

        <div v-else class="match-history">
          <article
            v-for="report in reports"
            :key="report.id"
            class="history-card"
            :class="{ active: report.id === selectedReportId }"
            @click="loadReportDetail(report.id)"
          >
            <div class="history-score">{{ report.overallScore ?? 0 }}</div>
            <div>
              <strong>{{ getJobName(report.jobPostId) }}</strong>
              <p>{{ getResumeName(report.resumeId) }} · {{ formatDate(report.createTime) }}</p>
            </div>
            <div class="history-actions">
              <el-tag :type="report.isRecommended === 1 ? 'success' : 'warning'" size="small">
                {{ report.isRecommended === 1 ? '推荐' : '谨慎' }}
              </el-tag>
              <el-button text type="danger" @click.stop="removeReport(report)">删除</el-button>
            </div>
          </article>
        </div>
      </section>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../layouts/AppLayout.vue'
import { getJobPage } from '../api/job'
import { createMatchReport, deleteMatchReport, getMatchDetail, getMatchPage } from '../api/match'
import { getResumePage } from '../api/resume'

const router = useRouter()
const formRef = ref()
const optionsLoading = ref(false)
const historyLoading = ref(false)
const generating = ref(false)
const detailLoading = ref(false)
const resumes = ref([])
const jobs = ref([])
const reports = ref([])
const currentReport = ref(null)
const selectedReportId = ref(null)

const form = reactive({
  resumeId: null,
  jobPostId: null
})

const rules = {
  resumeId: [{ required: true, message: '请选择简历', trigger: 'change' }],
  jobPostId: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

const hasRequiredData = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

const scoreSummary = computed(() => {
  const score = currentReport.value?.overallScore || 0
  if (score >= 85) return '匹配度较高，可以优先投递，并在沟通中突出相关项目。'
  if (score >= 70) return '具备投递价值，建议补强不足项后再提高命中率。'
  return '匹配基础偏弱，建议先优化项目表达或选择更贴近的岗位。'
})

onMounted(async () => {
  await Promise.all([loadOptions(), loadReports()])
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
    form.resumeId = form.resumeId || resumes.value.find((item) => item.isDefault === 1)?.id || resumes.value[0]?.id || null
    form.jobPostId = form.jobPostId || jobs.value[0]?.id || null
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '基础数据加载失败')
  } finally {
    optionsLoading.value = false
  }
}

async function loadReports() {
  historyLoading.value = true
  try {
    const page = await getMatchPage({ pageNo: 1, pageSize: 10 })
    reports.value = page.records || []
    if (!currentReport.value && reports.value.length > 0) {
      currentReport.value = reports.value[0]
      selectedReportId.value = reports.value[0].id
      form.resumeId = reports.value[0].resumeId
      form.jobPostId = reports.value[0].jobPostId
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史报告加载失败')
  } finally {
    historyLoading.value = false
  }
}

async function generateReport() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  generating.value = true
  try {
    const result = await createMatchReport({
      resumeId: form.resumeId,
      jobPostId: form.jobPostId
    })
    currentReport.value = result
    selectedReportId.value = result.id
    await loadReports()
    ElMessage.success('匹配报告已生成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '匹配报告生成失败')
  } finally {
    generating.value = false
  }
}

async function loadReportDetail(id) {
  detailLoading.value = true
  try {
    selectedReportId.value = id
    const detail = await getMatchDetail(id)
    currentReport.value = detail
    form.resumeId = detail.resumeId
    form.jobPostId = detail.jobPostId
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '匹配报告详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeReport(report) {
  try {
    await ElMessageBox.confirm('删除后该匹配报告将不可恢复，确认删除吗？', '删除匹配报告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMatchReport(report.id)
    if (selectedReportId.value === report.id) {
      selectedReportId.value = null
      currentReport.value = null
    }
    await loadReports()
    ElMessage.success('匹配报告已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function scoreRingStyle(score = 0) {
  const value = Math.max(0, Math.min(100, Number(score) || 0))
  return {
    background: `conic-gradient(var(--color-brand) ${value * 3.6}deg, rgba(238, 242, 255, 0.92) 0deg)`
  }
}

function resumeOptionLabel(resume) {
  const defaultText = resume.isDefault === 1 ? ' · 默认' : ''
  return `${resume.title || '未命名简历'}${defaultText}`
}

function jobOptionLabel(job) {
  return `${job.companyName || '未识别公司'} · ${job.jobName || '未识别岗位'}`
}

function getResumeName(id) {
  return resumes.value.find((item) => item.id === id)?.title || `简历 #${id}`
}

function getJobName(id) {
  const job = jobs.value.find((item) => item.id === id)
  if (!job) return `岗位 #${id}`
  return `${job.companyName || '未识别公司'} · ${job.jobName || '未识别岗位'}`
}

function formatDate(value) {
  if (!value) return '刚刚'
  return String(value).slice(0, 10)
}
</script>

<style scoped>
.control-panel,
.history-panel,
.score-hero,
.report-empty {
  padding: 24px;
}

.selector-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.selector-grid :deep(.el-select) {
  width: 100%;
}

.control-actions {
  display: flex;
  justify-content: flex-end;
}

.setup-hints {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.setup-card {
  border: 1px solid rgba(79, 110, 247, 0.14);
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(238, 242, 255, 0.74), rgba(255, 255, 255, 0.88));
  color: var(--color-text);
  cursor: pointer;
  padding: 16px;
  text-align: left;
  transition: border-color 170ms var(--ease-premium), transform 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium);
}

.setup-card:hover {
  border-color: rgba(79, 110, 247, 0.28);
  box-shadow: 0 12px 30px rgba(23, 25, 31, 0.055);
  transform: translateY(-1px);
}

.setup-card strong,
.setup-card span {
  display: block;
}

.setup-card span {
  margin-top: 8px;
  color: var(--color-text-soft);
  line-height: 1.65;
}

.report-stage {
  display: grid;
  gap: 18px;
}

.score-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  align-items: center;
  gap: 26px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 24%, rgba(79, 110, 247, 0.12), transparent 28%),
    linear-gradient(145deg, rgba(238, 242, 255, 0.68), rgba(255, 255, 255, 0.92) 46%),
    #fff;
}

.score-hero::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: linear-gradient(180deg, var(--color-brand), rgba(79, 110, 247, 0.24));
  content: "";
}

.score-copy span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.score-copy strong {
  display: block;
  margin-top: 8px;
  font-size: clamp(58px, 8vw, 96px);
  font-weight: 780;
  letter-spacing: 0;
  line-height: 0.95;
}

.score-copy p {
  max-width: 560px;
  margin: 18px 0 0;
  color: var(--color-text-soft);
  line-height: 1.75;
}

.score-orb {
  display: grid;
  position: relative;
  width: 164px;
  height: 164px;
  place-items: center;
  border-radius: 999px;
}

.score-orb::after {
  position: absolute;
  inset: 12px;
  border-radius: inherit;
  background: #fff;
  box-shadow: inset 0 0 0 1px var(--color-line);
  content: "";
}

.score-orb span,
.score-orb small {
  position: relative;
  z-index: 1;
}

.score-orb span {
  align-self: end;
  font-size: 42px;
  font-weight: 800;
}

.score-orb small {
  align-self: start;
  color: var(--color-text-muted);
  font-weight: 700;
}

.report-empty,
.history-empty {
  display: grid;
  place-items: center;
  min-height: 280px;
  color: var(--color-text-soft);
  text-align: center;
}

.history-empty {
  min-height: 170px;
}

.empty-orb {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 20px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-weight: 800;
}

.report-empty strong,
.history-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.report-empty p,
.history-empty p {
  max-width: 380px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.score-breakdown {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.score-card,
.analysis-card,
.history-card {
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow-card);
}

.score-card {
  padding: 18px;
}

.score-card span,
.analysis-card span {
  display: block;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.score-card strong {
  display: block;
  margin-top: 8px;
  font-size: 32px;
  line-height: 1;
}

.score-bar {
  overflow: hidden;
  height: 8px;
  border-radius: 999px;
  background: var(--color-brand-soft);
  margin-top: 16px;
}

.score-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-brand), var(--color-brand-deep));
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.analysis-card {
  padding: 20px;
}

.analysis-card p {
  margin: 12px 0 0;
  color: var(--color-text-soft);
  line-height: 1.85;
}

.analysis-card.positive {
  border-color: rgba(63, 143, 104, 0.18);
  background: rgba(239, 250, 244, 0.82);
}

.analysis-card.warning {
  border-color: rgba(183, 121, 31, 0.2);
  background: rgba(255, 247, 237, 0.86);
}

.analysis-card.suggestion {
  border-color: rgba(79, 110, 247, 0.16);
  background: rgba(238, 242, 255, 0.74);
}

.match-history {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.history-card {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  padding: 16px;
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium), background-color 170ms var(--ease-premium);
}

.history-card:hover,
.history-card.active {
  border-color: rgba(79, 110, 247, 0.24);
  background: #fff;
  box-shadow: var(--shadow-hover);
  transform: translateY(-1px);
}

.history-score {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 16px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 20px;
  font-weight: 800;
}

.history-card strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.history-actions {
  display: grid;
  justify-items: end;
  gap: 8px;
}

@media (max-width: 1120px) {
  .score-hero {
    grid-template-columns: 1fr;
  }

  .score-orb {
    width: 142px;
    height: 142px;
  }

  .score-breakdown,
  .analysis-grid,
  .match-history {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .selector-grid,
  .setup-hints {
    grid-template-columns: 1fr;
  }

  .history-card {
    grid-template-columns: 52px minmax(0, 1fr);
  }

  .history-actions {
    grid-column: 1 / -1;
    justify-items: start;
  }
}
</style>
