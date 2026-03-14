<template>
  <div class="page-shell">
    <div class="page-shell__content section-grid">
      <section class="glass-card home-hero">
        <p class="pill">Campus Medical Portal</p>
        <h1>校园医疗前端已切到独立 Vue 工作台</h1>
        <p>
          这个入口页承接学生、医生、管理员三类业务工作台。前端继续通过独立容器运行，
          统一对接 `backend-spring` 的 `/api/v1`、`/ws/alerts`，以及 `langgraph-app` 的 Agent 药房能力。
        </p>
        <div class="home-hero__actions">
          <RouterLink class="home-hero__button" to="/login">进入登录页</RouterLink>
          <RouterLink class="home-hero__link" to="/student/dashboard">进入学生端</RouterLink>
        </div>
      </section>

      <section class="role-grid">
        <article class="glass-card home-card">
          <h2>学生工作台</h2>
          <ul>
            <li>健康总览、本地 BMI、挂号、预约管理、购药记录保持原有分工。</li>
            <li>预约、病历走 `backend-spring`，药房仍走 Agent。</li>
            <li>支持取消、改期、文档下载与病历回看。</li>
          </ul>
        </article>
        <article class="glass-card home-card">
          <h2>医生工作台</h2>
          <ul>
            <li>待诊队列、开始接诊、历史病历、诊断提交和排班查询已拆分到独立页面。</li>
            <li>药品选择与诊断提交保持和后端字段一致。</li>
            <li>“已在诊”记录不会重复触发开始接诊。</li>
          </ul>
        </article>
        <article class="glass-card home-card">
          <h2>管理工作台</h2>
          <ul>
            <li>排班、医生、科室、学生、健康档案、药品和系统设置都走真实接口。</li>
            <li>运营总览继续保留 ECharts 和 WebSocket 预警入口。</li>
            <li>后端不承载前端资源，前后端完全分离部署。</li>
          </ul>
        </article>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-hero,
.home-card {
  padding: 28px;
}

.home-hero p {
  color: #456660;
  line-height: 1.7;
}

.home-hero__actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.home-hero__button,
.home-hero__link {
  border-radius: 999px;
  padding: 12px 18px;
}

.home-hero__button {
  background: #18312d;
  color: #fff;
}

.home-hero__link {
  background: rgba(24, 49, 45, 0.08);
}

.home-card ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
  color: #456660;
}

.role-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
</style>
