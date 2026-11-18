/*用来显示实验封面或宣传图片的组件，用于展示实验封面或宣传图片。*/

<template>
  <div class="preview-box">
    <img :src="Images.banner" class="preview-img" />
  </div>
</template>

<script setup>
import Images from '@/constants/images'
</script>

<style scoped>
.preview-box {
  width: min(760px, 46vw);
  aspect-ratio: 8 / 5;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--admin-border, #ebeef5);
  box-shadow: 0 14px 40px rgba(26, 179, 148, 0.14);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 图片自适应填充区域 */
}

@media (max-width: 1100px) {
  .preview-box {
    width: min(560px, 86vw);
  }
}
</style>
