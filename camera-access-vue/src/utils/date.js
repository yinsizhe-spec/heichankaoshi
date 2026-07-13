/**
 * 格式化为：2026年7月13日
 */
export function formatChineseDate(dateValue) {
  if (!dateValue) {
    return '长期有效'
  }

  const date = new Date(`${dateValue}T00:00:00`)

  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/**
 * 格式化为：7月13日
 */
export function formatMonthDay(dateValue) {
  if (!dateValue) {
    return '长期有效'
  }

  const date = new Date(`${dateValue}T00:00:00`)

  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return `${date.getMonth() + 1}月${date.getDate()}日`
}