<template>
  <AppLayout>
    <section class="workspace-page greeting-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">Boss greeting</p>
          <h1>打招呼话术</h1>
          <p>基于你的简历和岗位要求，生成自然、真诚、匹配度高的沟通开场白。</p>
        </div>
        <el-tag size="large">Boss Greeting</el-tag>
      </header>

      <section class="control-panel premium-card" v-loading="optionsLoading">
        <div class="section-title">
          <div>
            <h2>生成沟通开场白</h2>
            <p>选择简历和岗位，Agent 会生成适合复制到 Boss 直聘的自然话术。</p>
          </div>
        </div>

        <div v-if="!hasRequiredData && !optionsLoading" class="setup-hints">
          <button v-if="!resumes.length" type="button" class="setup-card" @click="router.push('/resumes')">
            <strong>还没有简历</strong>
            <span>先创建一份简历，让话术更贴合你的真实经历。</span>
          </button>
          <button v-if="!jobs.length" type="button" class="setup-card" @click="router.push('/jobs')">
            <strong>还没有岗位</strong>
            <span>先完成岗位分析，Agent 才能提炼匹配点。</span>
          </button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="selector-grid">
            <el-form-item label="选择简历" prop="resumeId">
              <el-select v-model="form.resumeId" filterable placeholder="选择用于生成话术的简历">
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
        </el-form>

        <div class="control-actions">
          <el-button type="primary" :loading="generating" :disabled="!hasRequiredData" @click="runGenerate">
            生成话术
          </el-button>
        </div>
      </section>

      <section class="greeting-stage" v-loading="generating || detailLoading">
        <article v-if="currentGreeting" class="greeting-card premium-card">
          <div class="greeting-card-head">
            <div>
              <span>Generated message</span>
              <h2>可直接复制到 Boss 直聘</h2>
            </div>
            <el-tag>80-150 字</el-tag>
          </div>

          <p class="greeting-text">{{ currentGreeting.greetingText || '暂无话术内容' }}</p>

          <div class="greeting-meta">
            <span>{{ getResumeName(currentGreeting.resumeId) }}</span>
            <span>{{ getJobName(currentGreeting.jobPostId) }}</span>
          </div>

          <div class="greeting-actions">
            <el-button type="primary" plain @click="copyText(currentGreeting.greetingText)">复制话术</el-button>
            <el-button plain type="danger" @click="removeGreeting(currentGreeting)">删除记录</el-button>
          </div>
        </article>

        <article v-else class="greeting-empty premium-card">
          <div class="empty-orb">Hi</div>
          <strong>等待生成沟通话术</strong>
          <p>选择简历和岗位后，Agent 会把技能、项目和岗位匹配点整理成自然开场白。</p>
        </article>
      </section>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史话术</h2>
            <p>保存不同岗位下的沟通版本，投递前可以快速复制和复用。</p>
          </div>
        </div>

        <div v-if="!historyLoading && greetings.length === 0" class="history-empty">
          <strong>还没有话术记录</strong>
          <p>生成第一条沟通话术后，这里会形成你的求职沟通素材库。</p>
        </div>

        <div v-else class="greeting-history">
          <article
            v-for="item in greetings"
            :key="item.id"
            class="history-card"
            :class="{ active: item.id === selectedGreetingId }"
            @click="loadGreetingDetail(item.id)"
          >
            <div>
              <span>{{ getJobName(item.jobPostId) }}</span>
              <strong>{{ getResumeName(item.resumeId) }}</strong>
              <p>{{ formatDate(item.createTime) }}</p>
            </div>
            <div class="history-actions">
              <el-button text @click.stop="copyText(item.greetingText)">复制</el-button>
              <el-button text type="danger" @click.stop="removeGreeting(item)">删除</el-button>
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
import { getResumePage } from '../api/resume'
import { deleteGreeting, generateGreeting, getGreetingDetail, getGreetingPage } from '../api/greeting'

const router = useRouter()
const formRef = ref()
const optionsLoading = ref(false)
const historyLoading = ref(false)
const generating = ref(false)
const detailLoading = ref(false)
const resumes = ref([])
const jobs = ref([])
const greetings = ref([])
const currentGreeting = ref(null)
const selectedGreetingId = ref(null)

const form = reactive({
  resumeId: null,
  jobPostId: null
})

const rules = {
  resumeId: [{ required: true, message: '请选择简历', trigger: 'change' }],
  jobPostId: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

const hasRequiredData = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

onMounted(async () => {
  await Promise.all([loadOptions(), loadGreetings()])
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

async function loadGreetings() {
  historyLoading.value = true
  try {
    const page = await getGreetingPage({ pageNo: 1, pageSize: 10 })
    greetings.value = page.records || []
    if (!currentGreeting.value && greetings.value.length > 0) {
      currentGreeting.value = greetings.value[0]
      selectedGreetingId.value = greetings.value[0].id
      form.resumeId = greetings.value[0].resumeId
      form.jobPostId = greetings.value[0].jobPostId
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史话术加载失败')
  } finally {
    historyLoading.value = false
  }
}

async function runGenerate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  generating.value = true
  try {
    const result = await generateGreeting({
      resumeId: form.resumeId,
      jobPostId: form.jobPostId
    })
    currentGreeting.value = result
    selectedGreetingId.value = result.id
    await loadGreetings()
    ElMessage.success('打招呼话术已生成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '话术生成失败')
  } finally {
    generating.value = false
  }
}

async function loadGreetingDetail(id) {
  detailLoading.value = true
  try {
    selectedGreetingId.value = id
    const detail = await getGreetingDetail(id)
    currentGreeting.value = detail
    form.resumeId = detail.resumeId
    form.jobPostId = detail.jobPostId
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '话术详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeGreeting(item) {
  if (!item?.id) return
  try {
    await ElMessageBox.confirm('删除后该话术记录将不可恢复，确认删除吗？', '删除话术', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteGreeting(item.id)
    if (selectedGreetingId.value === item.id) {
      selectedGreetingId.value = null
      currentGreeting.value = null
    }
    await loadGreetings()
    ElMessage.success('话术记录已删除')
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
.control-panel,
.history-panel,
.greeting-card,
.greeting-empty {
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
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium);
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

.greeting-stage {
  display: grid;
}

.greeting-card {
  display: grid;
  gap: 24px;
  border-color: rgba(79, 110, 247, 0.16);
  background:
    linear-gradient(145deg, rgba(238, 242, 255, 0.66), rgba(255, 255, 255, 0.94)),
    #fff;
}

.greeting-card-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 18px;
}

.greeting-card-head span,
.history-card span {
  display: block;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.greeting-card-head h2 {
  margin: 8px 0 0;
  font-size: 24px;
  line-height: 1.25;
}

.greeting-text {
  position: relative;
  margin: 0;
  border: 1px solid rgba(79, 110, 247, 0.12);
  border-radius: 20px;
  background:
    radial-gradient(circle at 88% 16%, rgba(79, 110, 247, 0.1), transparent 24%),
    rgba(255, 255, 255, 0.86);
  color: var(--color-text);
  font-size: 18px;
  line-height: 2;
  padding: 34px 26px 26px;
  white-space: pre-wrap;
}

.greeting-text::before {
  position: absolute;
  top: 14px;
  right: 18px;
  border-radius: 999px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  content: "Ready to send";
  font-size: 11px;
  font-weight: 760;
  line-height: 1;
  padding: 6px 9px;
}

.greeting-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.greeting-meta span {
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-soft);
  font-size: 13px;
  font-weight: 650;
  padding: 7px 12px;
}

.greeting-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.greeting-empty,
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

.greeting-empty strong,
.history-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.greeting-empty p,
.history-empty p {
  max-width: 420px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.greeting-history {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.history-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid var(--color-line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow-card);
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

.history-actions {
  display: grid;
  justify-items: end;
  gap: 8px;
}

@media (max-width: 1120px) {
  .greeting-history {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .selector-grid,
  .setup-hints {
    grid-template-columns: 1fr;
  }

  .greeting-card-head,
  .greeting-actions,
  .history-card {
    flex-direction: column;
  }

  .greeting-actions .el-button {
    width: 100%;
  }

  .history-actions {
    justify-items: start;
  }
}
</style>
