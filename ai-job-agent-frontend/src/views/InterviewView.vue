<template>
  <AppLayout>
    <section class="workspace-page interview-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">Interview coach</p>
          <h1>面试准备</h1>
          <p>根据目标岗位生成技术面试题、项目追问题和 HR 问题，让你面试前更有准备。</p>
        </div>
        <el-tag size="large">Interview Coach</el-tag>
      </header>

      <section class="control-panel premium-card" v-loading="optionsLoading">
        <div class="section-title">
          <div>
            <h2>生成面试题</h2>
            <p>选择一个已经分析过的岗位，Agent 会围绕 JD 和岗位要求生成准备清单。</p>
          </div>
        </div>

        <div v-if="!jobs.length && !optionsLoading" class="setup-hints">
          <button type="button" class="setup-card" @click="router.push('/jobs')">
            <strong>还没有岗位</strong>
            <span>先粘贴 JD 完成岗位分析，再生成面试准备内容。</span>
          </button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="selector-row">
            <el-form-item label="选择岗位" prop="jobPostId">
              <el-select v-model="form.jobPostId" filterable placeholder="选择目标岗位">
                <el-option v-for="job in jobs" :key="job.id" :label="jobOptionLabel(job)" :value="job.id" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="generating" :disabled="jobs.length === 0" @click="runGenerate">
              生成面试题
            </el-button>
          </div>
        </el-form>
      </section>

      <section class="coach-stage" v-loading="generating || detailLoading">
        <article v-if="!currentRecord" class="coach-empty premium-card">
          <div class="empty-orb">QA</div>
          <strong>等待生成面试准备内容</strong>
          <p>选择岗位后，技术题、项目追问和 HR 问题会以知识卡片形式展示。</p>
        </article>

        <template v-else>
          <div class="coach-summary premium-card">
            <div>
              <span>Preparation set</span>
              <h2>{{ getJobName(currentRecord.jobPostId) }}</h2>
              <p>{{ formatDate(currentRecord.createTime) }} · {{ totalQuestionCount }} 道准备问题</p>
            </div>
            <el-button plain type="danger" @click="removeInterview(currentRecord)">删除记录</el-button>
          </div>

          <div class="question-groups">
            <section
              v-for="group in questionGroups"
              :key="group.key"
              class="question-section premium-card"
              :class="group.key"
            >
              <div class="question-section-head">
                <div>
                  <el-tag>{{ group.tag }}</el-tag>
                  <h2>{{ group.title }}</h2>
                  <p>{{ group.description }}</p>
                </div>
                <strong>{{ group.items.length }}</strong>
              </div>

              <div v-if="group.items.length === 0" class="question-empty">
                暂无{{ group.title }}内容
              </div>

              <div v-else class="question-stack">
                <article v-for="(item, index) in group.items" :key="`${group.key}-${index}`" class="question-card">
                  <button type="button" class="question-trigger" @click="toggleQuestion(group.key, index)">
                    <span>{{ index + 1 }}</span>
                    <strong>{{ item.question }}</strong>
                    <em>{{ isExpanded(group.key, index) ? '收起' : '展开' }}</em>
                  </button>

                  <div v-if="isExpanded(group.key, index)" class="answer-box">
                    <p>{{ item.answerIdea }}</p>
                    <el-button text @click="copyText(item.answerIdea)">复制回答思路</el-button>
                  </div>
                </article>
              </div>
            </section>
          </div>
        </template>
      </section>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史面试准备</h2>
            <p>保存每个岗位的准备清单，面试前可以快速回看重点问题。</p>
          </div>
        </div>

        <div v-if="!historyLoading && records.length === 0" class="history-empty">
          <strong>还没有面试准备记录</strong>
          <p>生成第一组面试题后，这里会沉淀你的面试复习路径。</p>
        </div>

        <div v-else class="interview-history">
          <article
            v-for="record in records"
            :key="record.id"
            class="history-card"
            :class="{ active: record.id === selectedRecordId }"
            @click="loadInterviewDetail(record.id)"
          >
            <div>
              <span>{{ getJobName(record.jobPostId) }}</span>
              <strong>{{ getQuestionCount(record) }} 道准备题</strong>
              <p>{{ formatDate(record.createTime) }}</p>
            </div>
            <el-button text type="danger" @click.stop="removeInterview(record)">删除</el-button>
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
import {
  deleteInterviewRecord,
  generateInterviewQuestions,
  getInterviewDetail,
  getInterviewPage
} from '../api/interview'

const router = useRouter()
const formRef = ref()
const optionsLoading = ref(false)
const historyLoading = ref(false)
const generating = ref(false)
const detailLoading = ref(false)
const jobs = ref([])
const records = ref([])
const currentRecord = ref(null)
const selectedRecordId = ref(null)
const expandedKeys = ref(new Set())

const form = reactive({
  jobPostId: null
})

const rules = {
  jobPostId: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

const technicalQuestions = computed(() => parseQuestionList(currentRecord.value?.technicalQuestions))
const projectQuestions = computed(() => parseQuestionList(currentRecord.value?.projectQuestions))
const hrQuestions = computed(() => parseQuestionList(currentRecord.value?.hrQuestions))

const questionGroups = computed(() => [
  {
    key: 'technical',
    tag: 'Technical',
    title: '技术面试题',
    description: '围绕 Java、Spring Boot、MyBatis-Plus、JWT、LLM 和 Agent 架构展开。',
    items: technicalQuestions.value
  },
  {
    key: 'project',
    tag: 'Project',
    title: '项目追问题',
    description: '帮助你讲清楚项目职责、架构拆分、数据隔离和 AI 调用链。',
    items: projectQuestions.value
  },
  {
    key: 'hr',
    tag: 'HR',
    title: 'HR 常见问题',
    description: '准备求职动机、岗位匹配度、学习方式和实习期待。',
    items: hrQuestions.value
  }
])

const totalQuestionCount = computed(() =>
  technicalQuestions.value.length + projectQuestions.value.length + hrQuestions.value.length
)

onMounted(async () => {
  await Promise.all([loadJobs(), loadRecords()])
})

async function loadJobs() {
  optionsLoading.value = true
  try {
    const page = await getJobPage({ pageNo: 1, pageSize: 50 })
    jobs.value = page.records || []
    form.jobPostId = form.jobPostId || jobs.value[0]?.id || null
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '岗位列表加载失败')
  } finally {
    optionsLoading.value = false
  }
}

async function loadRecords() {
  historyLoading.value = true
  try {
    const page = await getInterviewPage({ pageNo: 1, pageSize: 10 })
    records.value = page.records || []
    if (!currentRecord.value && records.value.length > 0) {
      currentRecord.value = records.value[0]
      selectedRecordId.value = records.value[0].id
      form.jobPostId = records.value[0].jobPostId
      resetExpanded()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史面试准备加载失败')
  } finally {
    historyLoading.value = false
  }
}

async function runGenerate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  generating.value = true
  try {
    const result = await generateInterviewQuestions({
      jobPostId: form.jobPostId
    })
    currentRecord.value = result
    selectedRecordId.value = result.id
    resetExpanded()
    await loadRecords()
    ElMessage.success('面试准备内容已生成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '面试题生成失败')
  } finally {
    generating.value = false
  }
}

async function loadInterviewDetail(id) {
  detailLoading.value = true
  try {
    selectedRecordId.value = id
    const detail = await getInterviewDetail(id)
    currentRecord.value = detail
    form.jobPostId = detail.jobPostId
    resetExpanded()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '面试准备详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeInterview(record) {
  if (!record?.id) return
  try {
    await ElMessageBox.confirm('删除后该面试准备记录将不可恢复，确认删除吗？', '删除面试准备', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteInterviewRecord(record.id)
    if (selectedRecordId.value === record.id) {
      selectedRecordId.value = null
      currentRecord.value = null
      expandedKeys.value = new Set()
    }
    await loadRecords()
    ElMessage.success('面试准备记录已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function parseQuestionList(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map(normalizeQuestion).filter(Boolean)
  if (typeof raw === 'object') {
    if (Array.isArray(raw.questions)) return raw.questions.map(normalizeQuestion).filter(Boolean)
    if (raw.question || raw.answerIdea) return [normalizeQuestion(raw)].filter(Boolean)
    return []
  }

  const text = String(raw).trim()
  if (!text) return []
  try {
    return parseQuestionList(JSON.parse(text))
  } catch (error) {
    return [
      {
        question: '原始内容',
        answerIdea: text
      }
    ]
  }
}

function normalizeQuestion(item) {
  if (!item) return null
  if (typeof item === 'string') {
    return {
      question: item,
      answerIdea: '可以围绕岗位要求、项目经历和个人理解进行结构化回答。'
    }
  }
  return {
    question: item.question || item.title || '未命名问题',
    answerIdea: item.answerIdea || item.answer || item.idea || '暂无回答思路'
  }
}

function resetExpanded() {
  const next = new Set()
  if (technicalQuestions.value.length > 0) next.add(questionKey('technical', 0))
  if (projectQuestions.value.length > 0) next.add(questionKey('project', 0))
  if (hrQuestions.value.length > 0) next.add(questionKey('hr', 0))
  expandedKeys.value = next
}

function questionKey(group, index) {
  return `${group}-${index}`
}

function isExpanded(group, index) {
  return expandedKeys.value.has(questionKey(group, index))
}

function toggleQuestion(group, index) {
  const key = questionKey(group, index)
  const next = new Set(expandedKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedKeys.value = next
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

function getQuestionCount(record) {
  return (
    parseQuestionList(record.technicalQuestions).length +
    parseQuestionList(record.projectQuestions).length +
    parseQuestionList(record.hrQuestions).length
  )
}

function jobOptionLabel(job) {
  return `${job.companyName || '未识别公司'} · ${job.jobName || '未识别岗位'}`
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
.coach-empty,
.coach-summary,
.question-section {
  padding: 24px;
}

.selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.selector-row :deep(.el-select) {
  width: 100%;
}

.selector-row .el-button {
  margin-bottom: 18px;
}

.setup-hints {
  margin-bottom: 18px;
}

.setup-card {
  width: 100%;
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

.coach-stage {
  display: grid;
  gap: 18px;
}

.coach-empty,
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

.coach-empty strong,
.history-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.coach-empty p,
.history-empty p {
  max-width: 420px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.coach-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.coach-summary span,
.question-section-head p,
.history-card span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.coach-summary h2 {
  margin: 8px 0 0;
  font-size: 26px;
  line-height: 1.25;
}

.coach-summary p {
  margin: 8px 0 0;
  color: var(--color-text-soft);
}

.question-groups {
  display: grid;
  gap: 18px;
}

.question-section {
  border-color: rgba(79, 110, 247, 0.12);
  background:
    linear-gradient(145deg, rgba(238, 242, 255, 0.55), rgba(255, 255, 255, 0.92) 42%),
    #fff;
}

.question-section.project {
  border-color: rgba(63, 143, 104, 0.16);
  background:
    linear-gradient(145deg, rgba(237, 249, 242, 0.58), rgba(255, 255, 255, 0.92) 42%),
    #fff;
}

.question-section.hr {
  border-color: rgba(183, 121, 31, 0.16);
  background:
    linear-gradient(145deg, rgba(255, 243, 223, 0.62), rgba(255, 255, 255, 0.92) 42%),
    #fff;
}

.question-section.technical :deep(.el-tag) {
  border-color: rgba(79, 110, 247, 0.16);
  background: var(--color-brand-soft);
  color: var(--color-brand);
}

.question-section.project :deep(.el-tag) {
  border-color: rgba(63, 143, 104, 0.16);
  background: #edf9f2;
  color: #3f8f68;
}

.question-section.hr :deep(.el-tag) {
  border-color: rgba(183, 121, 31, 0.16);
  background: #fff3df;
  color: #b7791f;
}

.question-section-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
  border-bottom: 1px solid var(--color-line);
  padding-bottom: 18px;
}

.question-section-head h2 {
  margin: 12px 0 0;
  font-size: 24px;
}

.question-section-head p {
  max-width: 720px;
  margin: 8px 0 0;
  line-height: 1.7;
}

.question-section-head strong {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 16px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 22px;
}

.question-section.project .question-section-head strong,
.question-section.project .question-trigger span {
  background: #edf9f2;
  color: #3f8f68;
}

.question-section.hr .question-section-head strong,
.question-section.hr .question-trigger span {
  background: #fff3df;
  color: #b7791f;
}

.question-stack {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.question-card {
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  overflow: hidden;
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium);
}

.question-card:hover {
  border-color: rgba(79, 110, 247, 0.22);
  box-shadow: 0 12px 30px rgba(23, 25, 31, 0.05);
  transform: translateY(-1px);
}

.question-trigger {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
  padding: 18px;
  text-align: left;
}

.question-trigger span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 11px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 800;
}

.question-trigger strong {
  line-height: 1.65;
}

.question-trigger em {
  color: var(--color-brand);
  font-size: 13px;
  font-style: normal;
  font-weight: 720;
}

.answer-box {
  border-top: 1px solid var(--color-line);
  background: linear-gradient(145deg, rgba(248, 248, 246, 0.96), rgba(255, 255, 255, 0.88));
  padding: 18px 18px 16px 66px;
}

.answer-box p {
  margin: 0 0 12px;
  color: var(--color-text-soft);
  line-height: 1.85;
}

.question-empty {
  margin-top: 18px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(251, 251, 249, 0.88);
  color: var(--color-text-muted);
  padding: 18px;
  text-align: center;
}

.interview-history {
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

@media (max-width: 1120px) {
  .interview-history {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .selector-row,
  .question-section-head {
    grid-template-columns: 1fr;
  }

  .selector-row .el-button {
    width: 100%;
    margin-bottom: 0;
  }

  .coach-summary,
  .history-card {
    flex-direction: column;
    align-items: stretch;
  }

  .question-trigger {
    grid-template-columns: 30px minmax(0, 1fr);
  }

  .question-trigger em {
    grid-column: 2;
  }

  .answer-box {
    padding-left: 18px;
  }
}
</style>
