<template>
  <AppLayout>
    <section class="workspace-page job-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">JD insight</p>
          <h1>岗位分析</h1>
          <p>粘贴岗位 JD，让 Agent 提取技术栈、岗位要求、加分项与风险点。</p>
        </div>
        <div class="hero-actions">
          <el-tag size="large">JD Insight</el-tag>
          <el-button class="import-entry" @click="openImportDialog">批量导入</el-button>
        </div>
      </header>

      <div class="job-grid">
        <section class="jd-input premium-card">
          <div class="section-title">
            <div>
              <h2>粘贴岗位 JD</h2>
              <p>建议保留岗位职责、任职要求、薪资、城市和实习周期。</p>
            </div>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="来源平台" prop="source">
              <el-select v-model="form.source" placeholder="选择来源" class="source-select">
                <el-option label="Boss直聘" value="Boss直聘" />
                <el-option label="牛客" value="牛客" />
                <el-option label="实习僧" value="实习僧" />
                <el-option label="拉勾" value="拉勾" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>

            <el-form-item label="岗位 JD" prop="jdText">
              <el-input
                v-model="form.jdText"
                type="textarea"
                :rows="16"
                placeholder="在这里粘贴岗位 JD，Agent 会解析公司、岗位、城市、薪资、技术栈、职责、要求、加分项和风险点。"
              />
            </el-form-item>
          </el-form>

          <el-button class="analyze-button hover-arrow" type="primary" :loading="analyzing" @click="runAnalyze">
            <span>开始分析</span>
          </el-button>
        </section>

        <section class="report-panel premium-card" v-loading="analyzing || detailLoading">
          <div class="section-title">
            <div>
              <h2>岗位洞察报告</h2>
              <p>结构化理解岗位，让后续匹配和优化更准确。</p>
            </div>
          </div>

          <div v-if="!currentAnalysis" class="report-empty">
            <div class="empty-orb">JD</div>
            <strong>等待生成岗位洞察</strong>
            <p>粘贴 JD 并点击开始分析，结果会显示在这里。</p>
          </div>

          <div v-else class="report-content">
            <div class="report-title">
              <div>
                <span>{{ currentAnalysis.companyName || '未识别公司' }}</span>
                <h2>{{ currentAnalysis.jobName || '未识别岗位' }}</h2>
              </div>
              <el-tag>{{ currentAnalysis.city || '城市未知' }}</el-tag>
            </div>

            <div class="metric-grid">
              <div>
                <span>薪资</span>
                <strong>{{ currentAnalysis.salary || '未填写' }}</strong>
              </div>
              <div>
                <span>学历</span>
                <strong>{{ currentAnalysis.education || '未填写' }}</strong>
              </div>
              <div>
                <span>实习周期</span>
                <strong>{{ currentAnalysis.internshipCycle || '未填写' }}</strong>
              </div>
            </div>

            <div class="report-block">
              <span>技术栈</span>
              <div class="tag-cloud">
                <el-tag v-for="tag in techTags" :key="tag">{{ tag }}</el-tag>
                <span v-if="techTags.length === 0" class="muted">暂无技术栈信息</span>
              </div>
            </div>

            <div class="report-block">
              <span>岗位职责</span>
              <p>{{ currentAnalysis.responsibilities || '暂无职责信息' }}</p>
            </div>

            <div class="report-block">
              <span>任职要求</span>
              <p>{{ currentAnalysis.requirements || '暂无要求信息' }}</p>
            </div>

            <div class="insight-cards">
              <article class="insight-card positive">
                <span>加分项</span>
                <p>{{ currentAnalysis.bonusPoints || '暂无加分项' }}</p>
              </article>
              <article class="insight-card risk">
                <span>风险点</span>
                <p>{{ currentAnalysis.riskPoints || '暂无风险点' }}</p>
              </article>
            </div>
          </div>
        </section>
      </div>

      <section class="history-panel premium-card" v-loading="historyLoading">
        <div class="section-title">
          <div>
            <h2>历史岗位</h2>
            <p>回看已经分析过的岗位，继续生成匹配报告和后续准备。</p>
          </div>
        </div>

        <div v-if="!historyLoading && jobs.length === 0" class="history-empty">
          <strong>还没有岗位记录</strong>
          <p>分析第一条 JD 后，这里会出现岗位历史。</p>
        </div>

        <div v-else class="job-history">
          <article
            v-for="job in jobs"
            :key="job.id"
            class="job-card"
            :class="{ active: job.id === selectedJobId }"
            @click="loadJobDetail(job.id)"
          >
            <div>
              <span>{{ job.companyName || '未识别公司' }}</span>
              <strong>{{ job.jobName || '未识别岗位' }}</strong>
              <p>{{ job.city || '城市未知' }} · {{ job.salary || '薪资未知' }}</p>
            </div>
            <el-button text type="danger" @click.stop="removeJob(job)">删除</el-button>
          </article>
        </div>

        <div v-if="total > 0" class="history-footer">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            class="history-pagination"
            :page-sizes="[10, 20, 50]"
            :total="total"
            :disabled="historyLoading"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <el-dialog
        v-model="importDialogVisible"
        title="批量导入岗位"
        width="860px"
        class="job-import-dialog"
        :close-on-click-modal="!importing"
      >
        <div class="import-dialog-body">
          <section class="import-guide">
            <div>
              <p class="eyebrow">Batch import</p>
              <h3>上传 Excel / CSV 文件</h3>
              <p>模板字段：公司名称、岗位名称、城市、薪资、JD、来源链接。导入后会自动创建岗位并调用现有岗位分析能力。</p>
            </div>
            <el-tag type="info">最多 100 条</el-tag>
          </section>

          <el-upload
            class="import-upload"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls,.csv"
            :file-list="importFileList"
            :on-change="handleImportFileChange"
            :on-exceed="handleImportFileExceed"
            :on-remove="handleImportFileRemove"
          >
            <div class="upload-copy">
              <strong>拖拽文件到这里，或点击选择文件</strong>
              <span>支持 .xlsx / .xls / .csv，导入过程可能需要一些时间。</span>
            </div>
          </el-upload>

          <div class="template-fields">
            <span>公司名称</span>
            <span>岗位名称</span>
            <span>城市</span>
            <span>薪资</span>
            <span>JD</span>
            <span>来源链接</span>
          </div>

          <div v-if="importResult" class="import-result">
            <div class="import-summary">
              <div>
                <span>总数</span>
                <strong>{{ importResult.totalCount || 0 }}</strong>
              </div>
              <div class="success">
                <span>成功</span>
                <strong>{{ importResult.successCount || 0 }}</strong>
              </div>
              <div class="failed">
                <span>失败</span>
                <strong>{{ importResult.failureCount || 0 }}</strong>
              </div>
            </div>

            <el-table :data="importResult.items || []" max-height="320" class="import-table">
              <el-table-column prop="rowNumber" label="行号" width="76" />
              <el-table-column label="结果" width="92">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'danger'">
                    {{ row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="companyName" label="公司" min-width="130" />
              <el-table-column prop="jobName" label="岗位" min-width="160" />
              <el-table-column prop="failureReason" label="失败原因" min-width="220">
                <template #default="{ row }">
                  <span class="failure-reason">{{ row.failureReason || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="importDialogVisible = false" :disabled="importing">关闭</el-button>
            <el-button type="primary" :loading="importing" :disabled="!importFile" @click="submitImport">
              {{ importing ? 'Agent 正在解析岗位...' : '开始导入' }}
            </el-button>
          </div>
        </template>
      </el-dialog>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../layouts/AppLayout.vue'
import { analyzeJob, deleteJob, getJobDetail, getJobPage, importJobs } from '../api/job'

const formRef = ref()
const analyzing = ref(false)
const historyLoading = ref(false)
const detailLoading = ref(false)
const importing = ref(false)
const importDialogVisible = ref(false)
const importFile = ref(null)
const importFileList = ref([])
const importResult = ref(null)
const jobs = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentAnalysis = ref(null)
const selectedJobId = ref(null)

const form = reactive({
  source: 'Boss直聘',
  jdText: ''
})

const rules = {
  source: [{ required: true, message: '请选择来源平台', trigger: 'change' }],
  jdText: [{ required: true, message: '请粘贴岗位 JD', trigger: 'blur' }]
}

const techTags = computed(() => {
  const text = currentAnalysis.value?.techStack || ''
  return text
    .split(/[,，、/;；\n\r]+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

onMounted(() => loadJobs())

async function loadJobs({ force = false } = {}) {
  if (historyLoading.value && !force) return
  historyLoading.value = true
  try {
    const page = await getJobPage({ pageNo: pageNo.value, pageSize: pageSize.value })
    jobs.value = page.records || page.list || []
    total.value = Number(page.total ?? page.totalCount ?? page.count ?? jobs.value.length)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '岗位历史加载失败')
  } finally {
    historyLoading.value = false
  }
}

function handlePageChange(value) {
  if (historyLoading.value) return
  pageNo.value = value
  loadJobs()
}

function handlePageSizeChange(value) {
  if (historyLoading.value) return
  pageSize.value = value
  pageNo.value = 1
  loadJobs()
}

async function runAnalyze() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  analyzing.value = true
  try {
    const payload = buildAnalyzePayload()
    const result = await analyzeJob({
      source: payload.source,
      jdText: payload.jdText
    })
    currentAnalysis.value = result
    selectedJobId.value = result.jobPostId
    await loadJobs({ force: true })
    ElMessage.success('岗位分析完成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '岗位分析失败')
  } finally {
    analyzing.value = false
  }
}

function buildAnalyzePayload() {
  const rawText = form.jdText.trim()
  try {
    const parsed = JSON.parse(rawText)
    if (parsed && typeof parsed === 'object' && typeof parsed.jdText === 'string') {
      const jdText = parsed.jdText.trim()
      const source = typeof parsed.source === 'string' && parsed.source.trim() ? parsed.source.trim() : form.source
      form.jdText = jdText
      form.source = source
      return { source, jdText }
    }
  } catch (error) {
    // 普通 JD 文本不是 JSON 时保持原逻辑。
  }
  return {
    source: form.source,
    jdText: form.jdText
  }
}

async function loadJobDetail(id) {
  detailLoading.value = true
  try {
    selectedJobId.value = id
    const detail = await getJobDetail(id)
    currentAnalysis.value = detail.analysis
    form.source = detail.source || form.source
    form.jdText = detail.jdText || ''
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '岗位详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeJob(job) {
  try {
    await ElMessageBox.confirm(`确认删除「${job.jobName || '该岗位'}」吗？`, '删除岗位', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteJob(job.id)
    if (selectedJobId.value === job.id) {
      selectedJobId.value = null
      currentAnalysis.value = null
    }
    if (jobs.value.length === 1 && pageNo.value > 1) {
      pageNo.value -= 1
    }
    await loadJobs({ force: true })
    ElMessage.success('岗位已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function openImportDialog() {
  importDialogVisible.value = true
}

function handleImportFileChange(uploadFile, uploadFiles) {
  importFile.value = uploadFile.raw || null
  importFileList.value = uploadFiles.slice(-1)
  importResult.value = null
}

function handleImportFileExceed(files) {
  const file = files?.[0]
  if (!file) return
  importFile.value = file
  importFileList.value = [{ name: file.name, raw: file }]
  importResult.value = null
}

function handleImportFileRemove() {
  importFile.value = null
  importFileList.value = []
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择 Excel 或 CSV 文件')
    return
  }

  importing.value = true
  try {
    importResult.value = await importJobs(importFile.value)
    await loadJobs({ force: true })
    const successCount = importResult.value?.successCount || 0
    const failureCount = importResult.value?.failureCount || 0
    ElMessage.success(`导入完成：成功 ${successCount} 条，失败 ${failureCount} 条`)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '批量导入失败')
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.job-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.82fr) minmax(0, 1.18fr);
  gap: 22px;
  align-items: start;
}

.jd-input,
.report-panel,
.history-panel {
  padding: 24px;
}

.source-select,
.analyze-button {
  width: 100%;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.import-entry {
  height: 40px;
  border-radius: 999px;
  border-color: rgba(79, 110, 247, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-brand);
  font-weight: 720;
  transition: transform 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), border-color 170ms var(--ease-premium);
}

.import-entry:hover {
  border-color: rgba(79, 110, 247, 0.32);
  box-shadow: 0 12px 26px rgba(79, 110, 247, 0.12);
  transform: translateY(-1px);
}

.analyze-button {
  height: 46px;
}

.report-panel {
  min-height: 680px;
}

.report-empty,
.history-empty {
  display: grid;
  place-items: center;
  min-height: 390px;
  color: var(--color-text-soft);
  text-align: center;
}

.history-empty {
  min-height: 180px;
}

.report-empty strong,
.history-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.report-empty p,
.history-empty p {
  max-width: 320px;
  margin: 8px 0 0;
  line-height: 1.7;
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

.report-content {
  display: grid;
  gap: 20px;
}

.report-title {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--color-line);
  padding-bottom: 20px;
}

.report-title span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 680;
}

.report-title h2 {
  margin: 8px 0 0;
  font-size: 30px;
  line-height: 1.18;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-grid div {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-surface-soft);
  padding: 15px;
}

.metric-grid span,
.report-block > span,
.insight-card span {
  display: block;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 720;
}

.metric-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 16px;
  line-height: 1.45;
}

.report-block {
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  padding: 18px;
}

.report-block p {
  margin: 10px 0 0;
  color: var(--color-text-soft);
  line-height: 1.8;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.insight-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.insight-card {
  border-radius: 18px;
  padding: 18px;
}

.insight-card p {
  margin: 10px 0 0;
  line-height: 1.75;
}

.insight-card.positive {
  border: 1px solid rgba(63, 143, 104, 0.18);
  background: rgba(239, 250, 244, 0.76);
}

.insight-card.risk {
  border: 1px solid rgba(183, 121, 31, 0.2);
  background: rgba(255, 247, 237, 0.82);
}

.job-history {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.history-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--color-line);
}

.history-pagination {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  padding: 8px 10px;
  box-shadow: 0 10px 24px rgba(23, 25, 31, 0.035);
}

.job-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  padding: 18px;
  transition: border-color 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium), transform 170ms var(--ease-premium), background-color 170ms var(--ease-premium);
}

.job-card:hover,
.job-card.active {
  border-color: rgba(79, 110, 247, 0.24);
  background: #fff;
  box-shadow: 0 12px 30px rgba(23, 25, 31, 0.055);
  transform: translateY(-1px);
}

.job-card span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 680;
}

.job-card strong {
  display: block;
  margin-top: 8px;
  font-size: 17px;
}

.job-card p {
  margin: 8px 0 0;
  color: var(--color-text-soft);
  line-height: 1.6;
}

.import-dialog-body {
  display: grid;
  gap: 18px;
}

.import-guide {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: var(--color-surface-soft);
  padding: 18px;
}

.import-guide h3 {
  margin: 4px 0 8px;
  font-size: 21px;
}

.import-guide p {
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.import-upload :deep(.el-upload-dragger) {
  border-radius: 18px;
  border-color: rgba(79, 110, 247, 0.2);
  background: rgba(255, 255, 255, 0.72);
  padding: 28px 18px;
  transition: border-color 170ms var(--ease-premium), background-color 170ms var(--ease-premium), transform 170ms var(--ease-premium);
}

.import-upload :deep(.el-upload-dragger:hover) {
  border-color: rgba(79, 110, 247, 0.42);
  background: #fff;
  transform: translateY(-1px);
}

.upload-copy {
  display: grid;
  gap: 8px;
}

.upload-copy strong {
  color: var(--color-text);
  font-size: 16px;
}

.upload-copy span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.template-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-fields span {
  border: 1px solid rgba(79, 110, 247, 0.16);
  border-radius: 999px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 720;
  padding: 7px 11px;
}

.import-result {
  display: grid;
  gap: 16px;
}

.import-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.import-summary div {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  padding: 14px;
}

.import-summary span {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 720;
}

.import-summary strong {
  display: block;
  margin-top: 7px;
  font-size: 24px;
}

.import-summary .success strong {
  color: #2f8f68;
}

.import-summary .failed strong {
  color: #c0564a;
}

.import-table {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  overflow: hidden;
}

.failure-reason {
  color: var(--color-text-soft);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1120px) {
  .job-grid,
  .job-history {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .insight-cards {
    grid-template-columns: 1fr;
  }

  .history-footer {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .history-pagination {
    min-width: max-content;
  }
}
</style>
