<template>
  <div class="page-shell">
    <div class="page-shell__content">
      <section class="glass-card not-found">
        <p class="pill">404</p>
        <h1>页面不存在</h1>
        <p>当前 Vue 3 迁移版尚未实现该路由，或者路径输入有误。</p>
        <RouterLink to="/" class="not-found__link">返回首页</RouterLink>
      </section>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  padding: 32px;
  text-align: center;
}

.not-found__link {
  display: inline-block;
  margin-top: 20px;
  border-radius: 999px;
  padding: 12px 18px;
  background: #18312d;
  color: #fff;
}
</style>
