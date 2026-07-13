<template>
  <RouterView v-slot="{ Component }">
    <Transition name="fade-slide" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
</template>
