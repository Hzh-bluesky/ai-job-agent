<template>
  <article
    class="feature-card premium-card"
    role="button"
    tabindex="0"
    @click="goToFeature"
    @keydown.enter="goToFeature"
    @keydown.space.prevent="goToFeature"
  >
    <div class="feature-icon">
      <component :is="icon" />
    </div>
    <div class="feature-content">
      <div class="feature-head">
        <h3>{{ title }}</h3>
        <el-tag v-if="tag" size="small">{{ tag }}</el-tag>
      </div>
      <p>{{ description }}</p>
    </div>
    <el-button class="feature-action" text aria-label="Open feature" @click.stop="goToFeature">
      <ArrowRight class="icon-arrow" />
    </el-button>
  </article>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  icon: {
    type: Object,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  tag: {
    type: String,
    default: ''
  },
  to: {
    type: String,
    default: ''
  }
})

function goToFeature() {
  if (props.to) {
    router.push(props.to)
  }
}
</script>

<style scoped>
.feature-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 18px;
  align-items: start;
  min-height: 156px;
  padding: 24px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.feature-card:focus-visible {
  outline: 3px solid rgba(79, 110, 247, 0.22);
  outline-offset: 4px;
}

.feature-card:hover {
  transform: translateY(-3px);
}

.feature-card::after {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(79, 110, 247, 0.08), rgba(255, 255, 255, 0));
  content: "";
  opacity: 0;
  pointer-events: none;
  transition: opacity 180ms var(--ease-premium);
}

.feature-card:hover::after {
  opacity: 1;
}

.feature-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 15px;
  background: linear-gradient(145deg, #f7f9ff, var(--color-brand-soft));
  color: var(--color-brand);
  font-size: 20px;
  box-shadow: inset 0 0 0 1px rgba(79, 110, 247, 0.08);
}

.feature-icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.feature-content {
  min-width: 0;
}

.feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feature-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 740;
  letter-spacing: 0;
}

.feature-content p {
  margin: 12px 0 0;
  color: var(--color-text-soft);
  line-height: 1.7;
}

.feature-action {
  width: 34px;
  height: 34px;
  color: var(--color-text-muted);
}

.feature-action :deep(svg) {
  width: 16px;
  height: 16px;
}

.feature-card:hover .feature-action :deep(svg) {
  transform: translateX(2px);
}

@media (max-width: 640px) {
  .feature-card {
    grid-template-columns: auto 1fr;
  }

  .feature-action {
    display: none;
  }
}
</style>
