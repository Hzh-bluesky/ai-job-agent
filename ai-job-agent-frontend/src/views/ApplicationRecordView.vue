<template>
  <AppLayout>
    <section class="workspace-page application-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">Application pipeline</p>
          <h1>投递记录</h1>
          <p>集中管理你的岗位投递进度，记录沟通状态、匹配分数和后续动作。</p>
        </div>
        <div class="hero-actions">
          <el-tag size="large">Application Pipeline</el-tag>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增投递记录</el-button>
        </div>
      </header>

      <section class="status-overview">
        <article v-for="item in statusOptions" :key="item.value" class="status-card premium-card">
          <div class="status-card-head">
            <span :class="['status-dot', item.value]"></span>
            <em>{{ item.label }}</em>
          </div>
          <strong>{{ statusCountMap[item.value] || 0 }}</strong>
          <p>{{ item.caption }}</p>
        </article>
      </section>

      <section class="pipeline-board premium-card" v-loading="loading">
        <div class="section-title">
          <div>
            <h2>投递 Pipeline</h2>
            <p>按更新时间展示最近投递记录，适合每天复盘沟通进展。</p>
          </div>
          <el-button plain :icon="Refresh" @click="refreshAll">刷新</el-button>
        </div>

        <div v-if="!loading && applications.length === 0" class="pipeline-empty">
          <div class="empty-orb">AP</div>
          <strong>还没有投递记录</strong>
          <p>可以先分析岗位、生成匹配报告，再把目标岗位保存到投递 Pipeline。</p>
          <div class="empty-actions">
            <el-button plain @click="router.push('/jobs')">去分析岗位</el-button>
            <el-button plain @click="router.push('/match')">生成匹配报告</el-button>
            <el-button type="primary" @click="openCreateDialog">新增投递记录</el-button>
          </div>
        </div>

        <div v-else class="application-list">
          <article v-for="item in applications" :key="item.id" :class="['application-card', item.status]">
            <div class="application-main">
              <div class="application-head">
                <div>
                  <span>{{ item.companyName }}</span>
                  <h2>{{ item.jobName }}</h2>
                </div>
                <span :class="['status-pill', item.status]">{{ getStatusLabel(item.status) }}</span>
              </div>

              <div class="application-meta">
                <span>{{ item.city || '城市未知' }}</span>
                <span>{{ item.salary || '薪资未知' }}</span>
                <span>更新于 {{ formatDate(item.updateTime) }}</span>
              </div>

              <p class="remark">{{ item.remark || '暂无备注，建议记录沟通节点、面试时间或后续动作。' }}</p>
            </div>

            <div class="application-side">
              <div class="score-badge">
                <span>匹配分数</span>
                <strong>{{ item.matchScore ?? '-' }}</strong>
              </div>

              <el-select
                :model-value="item.status"
                size="small"
                class="status-select"
                @change="(status) => changeStatus(item, status)"
              >
                <el-option
                  v-for="status in statusOptions"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>

              <div class="card-actions">
                <el-button text :icon="View" @click="openDetail(item)">详情</el-button>
                <el-button text :icon="Edit" @click="openEditDialog(item)">编辑</el-button>
                <el-button text type="danger" :icon="Delete" @click="removeApplication(item)">删除</el-button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="applications.length > 0" class="pagination-wrap">
          <el-pagination
            layout="prev, pager, next"
            :current-page="pageNo"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <el-dialog
        v-model="dialogVisible"
        :title="isEditing ? '编辑投递记录' : '新增投递记录'"
        width="760px"
        class="application-dialog"
        destroy-on-close
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-grid">
            <el-form-item label="选择简历" prop="resumeId">
              <el-select v-model="form.resumeId" filterable clearable placeholder="可选，关联一份简历">
                <el-option
                  v-for="resume in resumes"
                  :key="resume.id"
                  :label="resumeOptionLabel(resume)"
                  :value="resume.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择岗位" prop="jobPostId">
              <el-select v-model="form.jobPostId" filterable clearable placeholder="可选，选择后自动填充岗位信息" @change="fillJobInfo">
                <el-option v-for="job in jobs" :key="job.id" :label="jobOptionLabel(job)" :value="job.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="匹配报告" prop="matchReportId">
              <el-select
                v-model="form.matchReportId"
                filterable
                clearable
                placeholder="可选，选择后自动填充分数"
                @change="fillMatchInfo"
              >
                <el-option
                  v-for="report in matchReports"
                  :key="report.id"
                  :label="matchOptionLabel(report)"
                  :value="report.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="投递状态" prop="status">
              <el-select v-model="form.status" placeholder="选择投递状态">
                <el-option
                  v-for="status in statusOptions"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="例如：示例科技有限公司" />
            </el-form-item>

            <el-form-item label="岗位名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="例如：AI Agent 应用开发实习生" />
            </el-form-item>

            <el-form-item label="城市">
              <el-input v-model="form.city" placeholder="例如：广州" />
            </el-form-item>

            <el-form-item label="薪资">
              <el-input v-model="form.salary" placeholder="例如：3K-5K/月" />
            </el-form-item>

            <el-form-item label="匹配分数">
              <el-input-number v-model="form.matchScore" :min="0" :max="100" controls-position="right" />
            </el-form-item>
          </div>

          <el-form-item label="JD 原文">
            <el-input v-model="form.jdText" type="textarea" :rows="5" placeholder="可以保存岗位 JD 原文，便于回看。" />
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="记录沟通节点、投递渠道、面试时间或后续动作。" />
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="dialog-actions">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveApplication">
              {{ isEditing ? '保存修改' : '创建记录' }}
            </el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog v-model="detailVisible" title="投递记录详情" width="720px" class="application-dialog">
        <div v-if="detail" class="detail-panel">
          <div class="detail-head">
            <div>
              <span>{{ detail.companyName }}</span>
              <h2>{{ detail.jobName }}</h2>
            </div>
            <span :class="['status-pill', detail.status]">{{ getStatusLabel(detail.status) }}</span>
          </div>

          <div class="detail-grid">
            <div>
              <span>城市</span>
              <strong>{{ detail.city || '未填写' }}</strong>
            </div>
            <div>
              <span>薪资</span>
              <strong>{{ detail.salary || '未填写' }}</strong>
            </div>
            <div>
              <span>匹配分数</span>
              <strong>{{ detail.matchScore ?? '未填写' }}</strong>
            </div>
            <div>
              <span>更新时间</span>
              <strong>{{ formatDate(detail.updateTime) }}</strong>
            </div>
          </div>

          <section>
            <span>备注</span>
            <p>{{ detail.remark || '暂无备注' }}</p>
          </section>

          <section>
            <span>JD 原文</span>
            <p>{{ detail.jdText || '暂无 JD 原文' }}</p>
          </section>
        </div>
      </el-dialog>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, View } from '@element-plus/icons-vue'
import AppLayout from '../layouts/AppLayout.vue'
import {
  createApplication,
  deleteApplication,
  getApplicationDetail,
  getApplicationPage,
  updateApplication,
  updateApplicationStatus
} from '../api/application'
import { getJobPage } from '../api/job'
import { getMatchPage } from '../api/match'
import { getResumePage } from '../api/resume'

const router = useRouter()
const loading = ref(false)
const optionsLoading = ref(false)
const saving = ref(false)
const applications = ref([])
const summaryRecords = ref([])
const resumes = ref([])
const jobs = ref([])
const matchReports = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const detail = ref(null)
const formRef = ref()

const statusOptions = [
  { value: 'NOT_APPLIED', label: '未投递', caption: '待决策或待准备' },
  { value: 'APPLIED', label: '已投递', caption: '简历已发送' },
  { value: 'COMMUNICATING', label: '待沟通', caption: '等待或正在沟通' },
  { value: 'INTERVIEWING', label: '面试中', caption: '进入面试流程' },
  { value: 'REJECTED', label: '已拒绝', caption: '暂未通过' },
  { value: 'PASSED', label: '已通过', caption: '获得通过结果' }
]

const emptyForm = {
  resumeId: null,
  jobPostId: null,
  matchReportId: null,
  companyName: '',
  jobName: '',
  city: '',
  salary: '',
  jdText: '',
  matchScore: null,
  status: 'NOT_APPLIED',
  remark: ''
}

const form = reactive({ ...emptyForm })

const rules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  jobName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择投递状态', trigger: 'change' }]
}

const statusCountMap = computed(() => {
  return statusOptions.reduce((result, item) => {
    result[item.value] = summaryRecords.value.filter((record) => record.status === item.value).length
    return result
  }, {})
})

onMounted(async () => {
  await Promise.all([loadOptions(), loadApplications(), loadSummary()])
})

async function refreshAll() {
  await Promise.all([loadApplications(), loadSummary(), loadOptions()])
}

async function loadApplications() {
  loading.value = true
  try {
    const page = await getApplicationPage({ pageNo: pageNo.value, pageSize: pageSize.value })
    applications.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '投递记录加载失败')
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    const page = await getApplicationPage({ pageNo: 1, pageSize: 100 })
    summaryRecords.value = page.records || []
  } catch (error) {
    summaryRecords.value = applications.value
  }
}

async function loadOptions() {
  optionsLoading.value = true
  try {
    const [resumePage, jobPage, matchPage] = await Promise.all([
      getResumePage({ pageNo: 1, pageSize: 50 }),
      getJobPage({ pageNo: 1, pageSize: 50 }),
      getMatchPage({ pageNo: 1, pageSize: 50 })
    ])
    resumes.value = resumePage.records || []
    jobs.value = jobPage.records || []
    matchReports.value = matchPage.records || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '基础数据加载失败')
  } finally {
    optionsLoading.value = false
  }
}

function handlePageChange(value) {
  pageNo.value = value
  loadApplications()
}

function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  Object.assign(form, emptyForm)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function openEditDialog(item) {
  try {
    const data = await getApplicationDetail(item.id)
    isEditing.value = true
    editingId.value = data.id
    Object.assign(form, emptyForm, data)
    dialogVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '投递记录详情加载失败')
  }
}

async function openDetail(item) {
  try {
    detail.value = await getApplicationDetail(item.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '投递记录详情加载失败')
  }
}

async function saveApplication() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEditing.value) {
      await updateApplication(editingId.value, buildPayload(false))
      await updateApplicationStatus(editingId.value, { status: form.status })
      ElMessage.success('投递记录已更新')
    } else {
      await createApplication(buildPayload())
      pageNo.value = 1
      ElMessage.success('投递记录已创建')
    }
    dialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(item, status) {
  if (item.status === status) return
  try {
    const updated = await updateApplicationStatus(item.id, { status })
    Object.assign(item, updated)
    const summaryItem = summaryRecords.value.find((record) => record.id === item.id)
    if (summaryItem) {
      Object.assign(summaryItem, updated)
    }
    ElMessage.success('投递状态已更新')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '状态更新失败')
  }
}

async function removeApplication(item) {
  try {
    await ElMessageBox.confirm(`确认删除「${item.companyName} · ${item.jobName}」吗？`, '删除投递记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteApplication(item.id)
    ElMessage.success('投递记录已删除')
    if (applications.value.length === 1 && pageNo.value > 1) {
      pageNo.value -= 1
    }
    await refreshAll()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function fillJobInfo(jobPostId) {
  const job = jobs.value.find((item) => item.id === jobPostId)
  if (!job) return
  form.companyName = job.companyName || form.companyName
  form.jobName = job.jobName || form.jobName
  form.city = job.city || form.city
  form.salary = job.salary || form.salary
  form.jdText = job.jdText || form.jdText
}

function fillMatchInfo(matchReportId) {
  const report = matchReports.value.find((item) => item.id === matchReportId)
  if (!report) return
  form.resumeId = report.resumeId || form.resumeId
  form.jobPostId = report.jobPostId || form.jobPostId
  form.matchScore = report.overallScore ?? form.matchScore
  fillJobInfo(form.jobPostId)
}

function buildPayload(includeStatus = true) {
  const payload = {
    resumeId: form.resumeId,
    jobPostId: form.jobPostId,
    matchReportId: form.matchReportId,
    companyName: form.companyName,
    jobName: form.jobName,
    city: form.city,
    salary: form.salary,
    jdText: form.jdText,
    matchScore: form.matchScore,
    remark: form.remark
  }
  if (includeStatus) {
    payload.status = form.status
  }
  return payload
}

function getStatusLabel(status) {
  return statusOptions.find((item) => item.value === status)?.label || status || '未知'
}

function resumeOptionLabel(resume) {
  const defaultText = resume.isDefault === 1 ? ' · 默认' : ''
  return `${resume.title || '未命名简历'}${defaultText}`
}

function jobOptionLabel(job) {
  return `${job.companyName || '未识别公司'} · ${job.jobName || '未识别岗位'}`
}

function matchOptionLabel(report) {
  return `${getJobName(report.jobPostId)} · ${report.overallScore ?? 0}分`
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
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.status-overview {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.status-card {
  padding: 18px;
}

.status-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--color-text-muted);
}

.status-card em {
  color: var(--color-text-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 720;
}

.status-card strong {
  display: block;
  margin-top: 12px;
  font-size: 32px;
  line-height: 1;
}

.status-card p {
  margin: 10px 0 0;
  color: var(--color-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.pipeline-board {
  padding: 24px;
}

.application-list {
  display: grid;
  gap: 14px;
}

.application-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  gap: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--color-line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-card);
  padding: 20px;
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium);
}

.application-card::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 4px;
  background: #8a90a0;
  content: "";
}

.application-card.NOT_APPLIED::before {
  background: #8a90a0;
}

.application-card.APPLIED::before {
  background: var(--color-brand);
}

.application-card.COMMUNICATING::before {
  background: #7057c9;
}

.application-card.INTERVIEWING::before {
  background: #b7791f;
}

.application-card.REJECTED::before {
  background: #c64b4b;
}

.application-card.PASSED::before {
  background: #3f8f68;
}

.application-card:hover {
  border-color: rgba(79, 110, 247, 0.24);
  box-shadow: var(--shadow-hover);
  transform: translateY(-1px);
}

.application-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}

.application-head span,
.detail-head span,
.detail-panel section > span,
.detail-grid span {
  display: block;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.application-head h2 {
  margin: 8px 0 0;
  font-size: 22px;
  line-height: 1.25;
}

.application-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.application-meta span {
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: rgba(251, 251, 249, 0.86);
  color: var(--color-text-soft);
  font-size: 12px;
  font-weight: 650;
  padding: 6px 10px;
}

.remark {
  margin: 16px 0 0;
  color: var(--color-text-soft);
  line-height: 1.75;
}

.application-side {
  display: grid;
  gap: 12px;
  align-content: start;
}

.score-badge {
  border: 1px solid rgba(79, 110, 247, 0.14);
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(238, 242, 255, 0.78), rgba(255, 255, 255, 0.92));
  padding: 16px;
}

.score-badge span {
  display: block;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 720;
}

.score-badge strong {
  display: block;
  margin-top: 8px;
  color: var(--color-brand);
  font-size: 34px;
  line-height: 1;
}

.status-select {
  width: 100%;
}

.card-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
}

.status-pill {
  width: fit-content;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 760;
  padding: 7px 11px;
  white-space: nowrap;
}

.status-pill.NOT_APPLIED {
  background: #f2f3f5;
  color: #626875;
}

.status-pill.APPLIED {
  background: var(--color-brand-soft);
  color: var(--color-brand);
}

.status-pill.COMMUNICATING {
  background: #f2edff;
  color: #7057c9;
}

.status-pill.INTERVIEWING {
  background: #fff3df;
  color: #b7791f;
}

.status-pill.REJECTED {
  background: #fff0f0;
  color: #c64b4b;
}

.status-pill.PASSED {
  background: #edf9f2;
  color: #3f8f68;
}

.status-dot.NOT_APPLIED {
  background: #8a90a0;
}

.status-dot.APPLIED {
  background: var(--color-brand);
}

.status-dot.COMMUNICATING {
  background: #7057c9;
}

.status-dot.INTERVIEWING {
  background: #b7791f;
}

.status-dot.REJECTED {
  background: #c64b4b;
}

.status-dot.PASSED {
  background: #3f8f68;
}

.pipeline-empty {
  display: grid;
  justify-items: center;
  min-height: 320px;
  color: var(--color-text-soft);
  text-align: center;
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

.pipeline-empty strong {
  margin-top: 12px;
  color: var(--color-text);
  font-size: 18px;
}

.pipeline-empty p {
  max-width: 420px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.empty-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-panel {
  display: grid;
  gap: 18px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--color-line);
  padding-bottom: 18px;
}

.detail-head h2 {
  margin: 8px 0 0;
  font-size: 24px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid div,
.detail-panel section {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(251, 251, 249, 0.86);
  padding: 14px;
}

.detail-grid strong {
  display: block;
  margin-top: 8px;
  line-height: 1.45;
}

.detail-panel section p {
  margin: 10px 0 0;
  color: var(--color-text-soft);
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .status-overview {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .application-card {
    grid-template-columns: 1fr;
  }

  .application-side {
    grid-template-columns: 150px minmax(0, 1fr) auto;
    align-items: center;
  }
}

@media (max-width: 760px) {
  .hero-actions,
  .application-head,
  .detail-head,
  .empty-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .status-overview,
  .form-grid,
  .detail-grid,
  .application-side,
  .card-actions {
    grid-template-columns: 1fr;
  }

  .empty-actions .el-button,
  .hero-actions .el-button {
    width: 100%;
  }
}
</style>
