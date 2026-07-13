<template>
  <AppLayout>
    <section class="workspace-page rewrite-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">Resume rewrite</p>
          <h1>简历优化</h1>
          <p>基于岗位要求优化项目经历，让简历表达更贴合目标岗位。</p>
        </div>
        <el-tag size="large">Resume Rewrite</el-tag>
      </header>

      <section class="rewrite-control premium-card" v-loading="optionsLoading">
        <div class="section-title">
          <div>
            <h2>项目经历输入</h2>
            <p>选择简历后会自动带入项目经历，你也可以只粘贴其中一段重点项目。</p>
          </div>
        </div>

        <div v-if="!hasRequiredData && !optionsLoading" class="setup-hints">
          <button v-if="!resumes.length" type="button" class="setup-card" @click="router.push('/resumes')">
            <strong>还没有简历</strong>
            <span>先创建简历，Agent 才能理解你的项目基础。</span>
          </button>
          <button v-if="!jobs.length" type="button" class="setup-card" @click="router.push('/jobs')">
            <strong>还没有岗位</strong>
            <span>先完成岗位分析，再围绕岗位要求优化表达。</span>
          </button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="selector-grid">
            <el-form-item label="选择简历" prop="resumeId">
              <el-select v-model="form.resumeId" filterable placeholder="选择简历" @change="fillProjectFromResume">
                <el-option
                  v-for="resume in resumes"
                  :key="resume.id"
                  :label="resumeOptionLabel(resume)"
                  :value="resume.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择岗位" prop="jobPostId">
              <el-select v-model="form.jobPostId" filterable placeholder="选择目标岗位">
                <el-option v-for="job in jobs" :key="job.id" :label="jobOptionLabel(job)" :value="job.id" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="原始项目经历" prop="projectExperience">
            <el-input
              v-model="form.projectExperience"
              type="textarea"
              :rows="8"
              placeholder="粘贴你想优化的项目经历。系统只会基于已有内容做表达优化，不会伪造经历。"
            />
          </el-form-item>
        </el-form>

        <div class="control-actions">
          <el-button type="primary" :loading="rewriting" :disabled="!hasRequiredData" @click="runRewrite">
            开始优化
          </el-button>
        </div>
      </section>

      <section class="rewrite-stage" v-loading="rewriting || detailLoading">
        <article v-if="!currentRecord" class="rewrite-empty premium-card">
          <div class="empty-orb">RW</div>
          <strong>等待生成优化结果</strong>
          <p>输入项目经历后，优化前后对比、优化理由和可直接放入简历的版本会展示在这里。</p>
        </article>

        <template v-else>
          <div class="compare-grid">
            <article class="compare-card original">
              <span>原始项目描述</span>
              <p>{{ currentRecord.originalProject || '暂无原始项目描述' }}</p>
            </article>
            <article class="compare-card rewritten">
              <span>优化后项目描述</span>
              <p>{{ currentRecord.rewrittenProject || '暂无优化后项目描述' }}</p>
            </article>
          </div>

          <div class="rewrite-result-grid">
            <article class="result-card reason">
              <span>优化理由</span>
              <p>{{ currentRecord.rewriteReason || '暂无优化理由' }}</p>
            </article>
            <article class="result-card resume-version">
              <span>可直接放入简历的版本</span>
              <p>{{ currentRecord.resumeVersion || '暂无简历版本' }}</p>
            </article>
          </div>

          <div class="rewrite-actions">
            <el-button plain @click="copyText(currentRecord.rewrittenProject)">复制优化后描述</el-button>
            <el-button plain @click="copyText(currentRecord.resumeVersion)">复制简历版本</el-button>
            <el-button plain type="danger" @click="removeRewrite(currentRecord)">删除记录</el-button>
          </div>
        </template>
      </section>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史优化记录</h2>
            <p>沉淀不同岗位下的项目表达版本，面试和投递前可以快速回看。</p>
          </div>
        </div>

        <div v-if="!historyLoading && records.length === 0" class="history-empty">
          <strong>还没有优化记录</strong>
          <p>完成第一次项目优化后，这里会保存你的表达版本。</p>
        </div>

        <div v-else class="rewrite-history">
          <article
            v-for="record in records"
            :key="record.id"
            class="history-card"
            :class="{ active: record.id === selectedRecordId }"
            @click="loadRewriteDetail(record.id)"
          >
            <div>
              <span>{{ getJobName(record.jobPostId) }}</span>
              <strong>{{ getResumeName(record.resumeId) }}</strong>
              <p>{{ formatDate(record.createTime) }}</p>
            </div>
            <el-button text type="danger" @click.stop="removeRewrite(record)">删除</el-button>
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
import { getResumePage } from '../api/resume'
import {
  createResumeRewrite,
  deleteResumeRewrite,
  getResumeRewriteDetail,
  getResumeRewritePage
} from '../api/resumeRewrite'

const router = useRouter()
const formRef = ref()
const optionsLoading = ref(false)
const historyLoading = ref(false)
const rewriting = ref(false)
const detailLoading = ref(false)
const resumes = ref([])
const jobs = ref([])
const records = ref([])
const currentRecord = ref(null)
const selectedRecordId = ref(null)

const form = reactive({
  resumeId: null,
  jobPostId: null,
  projectExperience: ''
})

const rules = {
  resumeId: [{ required: true, message: '请选择简历', trigger: 'change' }],
  jobPostId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  projectExperience: [{ required: true, message: '请输入原始项目经历', trigger: 'blur' }]
}

const hasRequiredData = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

onMounted(async () => {
  await Promise.all([loadOptions(), loadRecords()])
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
    if (form.resumeId && !form.projectExperience) {
      fillProjectFromResume(form.resumeId)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '基础数据加载失败')
  } finally {
    optionsLoading.value = false
  }
}

async function loadRecords() {
  historyLoading.value = true
  try {
    const page = await getResumeRewritePage({ pageNo: 1, pageSize: 10 })
    records.value = page.records || []
    if (!currentRecord.value && records.value.length > 0) {
      currentRecord.value = records.value[0]
      selectedRecordId.value = records.value[0].id
      form.resumeId = records.value[0].resumeId
      form.jobPostId = records.value[0].jobPostId
      form.projectExperience = records.value[0].originalProject || ''
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史优化记录加载失败')
  } finally {
    historyLoading.value = false
  }
}

function fillProjectFromResume(resumeId) {
  const resume = resumes.value.find((item) => item.id === resumeId)
  form.projectExperience = resume?.projectExperience || ''
}

async function runRewrite() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  rewriting.value = true
  try {
    const result = await createResumeRewrite({
      resumeId: form.resumeId,
      jobPostId: form.jobPostId,
      projectExperience: form.projectExperience
    })
    currentRecord.value = result
    selectedRecordId.value = result.id
    await loadRecords()
    ElMessage.success('简历项目经历已优化')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '简历优化失败')
  } finally {
    rewriting.value = false
  }
}

async function loadRewriteDetail(id) {
  detailLoading.value = true
  try {
    selectedRecordId.value = id
    const detail = await getResumeRewriteDetail(id)
    currentRecord.value = detail
    form.resumeId = detail.resumeId
    form.jobPostId = detail.jobPostId
    form.projectExperience = detail.originalProject || ''
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '优化记录详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeRewrite(record) {
  if (!record?.id) return
  try {
    await ElMessageBox.confirm('删除后该优化记录将不可恢复，确认删除吗？', '删除优化记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteResumeRewrite(record.id)
    if (selectedRecordId.value === record.id) {
      selectedRecordId.value = null
      currentRecord.value = null
    }
    await loadRecords()
    ElMessage.success('优化记录已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

async function copyText(text) {
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择文本复制')
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
.rewrite-control,
.history-panel,
.rewrite-empty {
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

.control-actions,
.rewrite-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.rewrite-stage {
  display: grid;
  gap: 18px;
}

.rewrite-empty,
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

.rewrite-empty strong,
.history-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.rewrite-empty p,
.history-empty p {
  max-width: 420px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.compare-card,
.result-card,
.history-card {
  border: 1px solid var(--color-line);
  border-radius: 20px;
  box-shadow: var(--shadow-card);
}

.compare-card {
  position: relative;
  overflow: hidden;
  min-height: 280px;
  padding: 24px;
}

.compare-card::before {
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: #d9dde7;
  content: "";
}

.compare-card span,
.result-card span,
.history-card span {
  display: block;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.compare-card p,
.result-card p {
  margin: 14px 0 0;
  color: var(--color-text-soft);
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.compare-card.original {
  background: rgba(251, 251, 249, 0.94);
}

.compare-card.original::before {
  background: #d9dde7;
}

.compare-card.rewritten {
  border-color: rgba(79, 110, 247, 0.16);
  background:
    radial-gradient(circle at 88% 12%, rgba(79, 110, 247, 0.12), transparent 24%),
    linear-gradient(145deg, rgba(238, 242, 255, 0.86), rgba(255, 255, 255, 0.92) 48%),
    #fff;
}

.compare-card.rewritten::before {
  background: linear-gradient(90deg, var(--color-brand), rgba(79, 110, 247, 0.22));
}

.compare-card.rewritten p {
  color: var(--color-text);
}

.rewrite-result-grid {
  display: grid;
  grid-template-columns: 0.86fr 1.14fr;
  gap: 18px;
}

.result-card {
  padding: 22px;
}

.result-card.reason {
  background: rgba(255, 247, 237, 0.78);
  border-color: rgba(183, 121, 31, 0.18);
}

.result-card.resume-version {
  background: #fff;
  border-color: rgba(79, 110, 247, 0.18);
}

.result-card.resume-version p {
  color: var(--color-text);
  font-weight: 520;
}

.rewrite-actions {
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  padding: 16px;
}

.rewrite-history {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.history-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  padding: 18px;
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium), background-color 170ms var(--ease-premium);
}

.history-card:hover,
.history-card.active {
  border-color: rgba(79, 110, 247, 0.24);
  background: #fff;
  box-shadow: var(--shadow-hover);
  transform: translateY(-1px);
}

.history-card strong {
  display: block;
  margin-top: 8px;
  font-size: 17px;
}

.history-card p {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

@media (max-width: 1120px) {
  .compare-grid,
  .rewrite-result-grid,
  .rewrite-history {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .selector-grid,
  .setup-hints {
    grid-template-columns: 1fr;
  }

  .control-actions,
  .rewrite-actions {
    flex-direction: column;
  }

  .control-actions .el-button,
  .rewrite-actions .el-button {
    width: 100%;
  }
}
</style>
