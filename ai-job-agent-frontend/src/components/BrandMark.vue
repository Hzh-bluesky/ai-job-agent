<template>
  <div class="brand-mark" :class="{ compact }">
    <div class="brand-symbol">
      <span>AI</span>
    </div>
    <div class="brand-copy">
      <strong>Job Agent</strong>
      <span v-if="!compact">AI 求职投递助手</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-symbol {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(79, 110, 247, 0.2);
  border-radius: 14px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(238, 242, 255, 0.92)),
    #fff;
  color: var(--color-brand);
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgba(79, 110, 247, 0.14);
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-copy strong {
  font-size: 16px;
  font-weight: 760;
  letter-spacing: 0;
}

.brand-copy span {
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.4;
}

.brand-mark.compact .brand-symbol {
  width: 38px;
  height: 38px;
  border-radius: 12px;
}
</style>
