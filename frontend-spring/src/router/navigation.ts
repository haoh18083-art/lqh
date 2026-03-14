export interface NavItem {
  label: string
  to: string
  summary: string
}

export const studentNavigation: NavItem[] = [
  { label: '健康总览', to: '/student/dashboard', summary: '指标看板与提醒位' },
  { label: '问诊助手', to: '/student/consultation', summary: '挂号流程与病历入口' },
  { label: '自助药房', to: '/student/pharmacy', summary: 'Agent 药房与购物流程' },
  { label: '预约管理', to: '/student/appointments', summary: '预约查询、取消、改期' },
  { label: '购药记录', to: '/student/medicine-records', summary: 'Agent 订单记录与回查' }
]

export const doctorNavigation: NavItem[] = [
  { label: '接诊台', to: '/doctor/dashboard', summary: '待诊列表、开始接诊、诊断提交' },
  { label: '我的排班', to: '/doctor/schedule', summary: '排班查询与周视图' }
]

export const adminNavigation: NavItem[] = [
  { label: '运营总览', to: '/admin/dashboard', summary: '真实统计、图表和预警通道' },
  { label: '排班管理', to: '/admin/schedules', summary: '医生排班批量维护' },
  { label: '医生管理', to: '/admin/doctors', summary: '医生与科室治理' },
  { label: '学生管理', to: '/admin/students', summary: '学生与健康档案入口' },
  { label: '药品管理', to: '/admin/medicines', summary: '库存维护与药品治理' },
  { label: '系统设置', to: '/admin/settings', summary: 'LLM 设置与运行配置' }
]
