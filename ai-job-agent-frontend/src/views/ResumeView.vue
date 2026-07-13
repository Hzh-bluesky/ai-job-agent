<template>
  <AppLayout>
    <section class="workspace-page resume-page">
      <header class="page-hero">
        <div>
          <p class="eyebrow">Resume studio</p>
          <h1>我的简历</h1>
          <p>维护你的求职基础资料，让 Agent 更准确地分析岗位和优化项目经历。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="startCreate">新增简历</el-button>
      </header>

      <div class="resume-workspace">
        <aside class="resume-list premium-card" v-loading="loading">
          <div class="section-title">
            <div>
              <h2>简历文档</h2>
              <p>{{ resumes.length ? `${resumes.length} 份简历` : '还没有保存的简历' }}</p>
            </div>
          </div>

          <div v-if="!loading && resumes.length === 0" class="resume-empty">
            <div class="empty-orb">CV</div>
            <strong>创建你的第一份简历</strong>
            <p>保存基础资料后，岗位分析、匹配报告和项目优化都会更准确。</p>
            <el-button type="primary" :icon="Plus" @click="startCreate">新增简历</el-button>
          </div>

          <div v-else class="resume-items">
            <button
              v-for="item in resumes"
              :key="item.id"
              class="resume-item"
              :class="{ active: item.id === selectedId }"
              type="button"
              @click="selectResume(item)"
            >
              <div>
                <div class="resume-item-head">
                  <strong>{{ item.title }}</strong>
                  <el-tag v-if="item.isDefault === 1" size="small">默认</el-tag>
                </div>
                <p>{{ item.school || '未填写学校' }} · {{ item.major || '未填写专业' }}</p>
              </div>
              <span>{{ formatDate(item.updateTime) }}</span>
            </button>
          </div>
        </aside>

        <section class="resume-editor premium-card">
          <div class="section-title">
            <div>
              <h2>{{ isCreating ? '新建简历' : '编辑简历' }}</h2>
              <p>像整理一份可被 Agent 理解的求职档案。</p>
            </div>
            <el-tag v-if="form.isDefault === 1" size="small">默认简历</el-tag>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <div class="form-grid">
              <el-form-item label="简历名称" prop="title">
                <el-input v-model="form.title" placeholder="例如：AI Agent 应用开发实习简历" />
              </el-form-item>
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" placeholder="你的姓名" />
              </el-form-item>
              <el-form-item label="学校">
                <el-input v-model="form.school" placeholder="学校名称" />
              </el-form-item>
              <el-form-item label="专业">
                <el-input v-model="form.major" placeholder="例如：软件工程" />
              </el-form-item>
              <el-form-item label="年级">
                <el-input v-model="form.grade" placeholder="例如：大三" />
              </el-form-item>
              <el-form-item label="技术栈">
                <el-input v-model="form.techStack" placeholder="Java, Spring Boot, MySQL, Vue3, LLM" />
              </el-form-item>
            </div>

            <el-form-item label="项目经历">
              <el-input
                v-model="form.projectExperience"
                type="textarea"
                :rows="7"
                placeholder="描述项目背景、职责、技术栈、成果。"
              />
            </el-form-item>

            <el-form-item label="实习经历">
              <el-input
                v-model="form.internshipExperience"
                type="textarea"
                :rows="5"
                placeholder="没有实习也可以先留空。"
              />
            </el-form-item>

            <el-form-item label="自我介绍">
              <el-input
                v-model="form.selfIntroduction"
                type="textarea"
                :rows="4"
                placeholder="简洁描述你的方向、优势和求职目标。"
              />
            </el-form-item>
          </el-form>

          <div class="editor-actions">
            <el-button type="primary" :loading="saving" @click="saveResume">保存简历</el-button>
            <el-button v-if="!isCreating" plain :disabled="form.isDefault === 1" @click="markDefault">
              设置默认
            </el-button>
            <el-button v-if="!isCreating" plain type="danger" @click="removeResume">删除</el-button>
          </div>
        </section>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AppLayout from '../layouts/AppLayout.vue'
import {
  createResume,
  deleteResume,
  getResumeDetail,
  getResumePage,
  setDefaultResume,
  updateResume
} from '../api/resume'

const loading = ref(false)
const saving = ref(false)
const resumes = ref([])
const selectedId = ref(null)
const formRef = ref()

const emptyForm = {
  title: '',
  name: '',
  school: '',
  major: '',
  grade: '',
  techStack: '',
  projectExperience: '',
  internshipExperience: '',
  selfIntroduction: '',
  isDefault: 0
}

const form = reactive({ ...emptyForm })

const rules = {
  title: [{ required: true, message: '请输入简历名称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const isCreating = computed(() => !selectedId.value)

onMounted(loadResumes)

async function loadResumes(keepSelection = false) {
  loading.value = true
  try {
    const page = await getResumePage({ pageNo: 1, pageSize: 10 })
    resumes.value = page.records || []
    if (!keepSelection && resumes.value.length > 0) {
      await selectResume(resumes.value[0])
    }
    if (resumes.value.length === 0) {
      startCreate()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '简历列表加载失败')
  } finally {
    loading.value = false
  }
}

function startCreate() {
  selectedId.value = null
  Object.assign(form, emptyForm)
  formRef.value?.clearValidate()
}

async function selectResume(item) {
  try {
    selectedId.value = item.id
    const detail = await getResumeDetail(item.id)
    Object.assign(form, emptyForm, detail)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '简历详情加载失败')
  }
}

async function saveResume() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      title: form.title,
      name: form.name,
      school: form.school,
      major: form.major,
      grade: form.grade,
      techStack: form.techStack,
      projectExperience: form.projectExperience,
      internshipExperience: form.internshipExperience,
      selfIntroduction: form.selfIntroduction
    }
    const saved = isCreating.value
      ? await createResume({ ...payload, isDefault: resumes.value.length === 0 ? 1 : 0 })
      : await updateResume(selectedId.value, payload)

    ElMessage.success('简历已保存')
    selectedId.value = saved.id
    Object.assign(form, emptyForm, saved)
    await loadResumes(true)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function markDefault() {
  if (!selectedId.value) return
  try {
    const updated = await setDefaultResume(selectedId.value)
    Object.assign(form, emptyForm, updated)
    await loadResumes(true)
    ElMessage.success('已设置为默认简历')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '设置默认失败')
  }
}

async function removeResume() {
  if (!selectedId.value) return
  try {
    await ElMessageBox.confirm('删除后该简历将不可恢复，确认删除吗？', '删除简历', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteResume(selectedId.value)
    ElMessage.success('简历已删除')
    selectedId.value = null
    await loadResumes()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function formatDate(value) {
  if (!value) return '刚刚'
  return String(value).slice(0, 10)
}
</script>

<style scoped>
.resume-workspace {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.resume-list,
.resume-editor {
  padding: 24px;
}

.resume-list {
  position: sticky;
  top: 34px;
  min-height: 560px;
}

.resume-items {
  display: grid;
  gap: 12px;
}

.resume-item {
  display: grid;
  gap: 14px;
  width: 100%;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text);
  cursor: pointer;
  padding: 16px;
  text-align: left;
  transition: border-color 170ms var(--ease-premium), background-color 170ms var(--ease-premium), transform 170ms var(--ease-premium), box-shadow 170ms var(--ease-premium);
}

.resume-item:hover,
.resume-item.active {
  border-color: rgba(79, 110, 247, 0.24);
  background: #fff;
  box-shadow: 0 12px 30px rgba(23, 25, 31, 0.055);
  transform: translateY(-1px);
}

.resume-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.resume-item strong {
  font-size: 15px;
  line-height: 1.45;
}

.resume-item p,
.resume-item > span {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.resume-empty {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 390px;
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

.resume-empty strong {
  color: var(--color-text);
  font-size: 18px;
}

.resume-empty p {
  max-width: 260px;
  margin: 0;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid var(--color-line);
  margin-top: 8px;
  padding-top: 20px;
}

@media (max-width: 1120px) {
  .resume-workspace {
    grid-template-columns: 1fr;
  }

  .resume-list {
    position: static;
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .editor-actions {
    flex-direction: column;
  }

  .editor-actions .el-button {
    width: 100%;
  }
}
</style>
