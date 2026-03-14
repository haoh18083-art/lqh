const SH_TIMEZONE = 'Asia/Shanghai'

export const formatDate = (value?: string | null, options?: Intl.DateTimeFormatOptions): string => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleDateString('zh-CN', {
    timeZone: SH_TIMEZONE,
    ...options
  })
}

export const formatDateTime = (value?: string | null): string => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: SH_TIMEZONE
  })
}

export const weekdayLabel = (value: string): string => {
  const date = new Date(value)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return Number.isNaN(date.getTime()) ? '-' : weekdays[date.getDay()]
}

export const formatMoney = (value?: number | string | null): string => {
  const amount = Number(value ?? 0)
  return `¥${amount.toFixed(2)}`
}
