/*用来显示实验封面或宣传图片的组件，用于展示实验封面或宣传图片。*/ 

<template>
  <div class="preview-box">
    <img src="@/assets/banner.png" class="preview-img" />
  </div>
</template>

<script setup></script>

<style scoped>
.preview-box{
  width:800px;
  height:500px;
  background:#fff;
  border:1px solid #ccc;
  padding:15px;
  box-sizing:border-box;
}
.preview-img{
  width:100%;
  height:100%;
  object-fit:cover;      /* 图片自适应填充区域 */
}
</style>