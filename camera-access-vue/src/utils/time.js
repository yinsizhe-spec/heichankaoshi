const SECOND_MS = 1000
const DAY_MS = 24 * 60 * 60 * SECOND_MS

/**
 * 判断字符串是否包含完整日期。
 */
function hasDatePart(value) {
  return /^\d{4}-\d{2}-\d{2}/.test(String(value || '').trim())
}

/**
 * 解析后端返回的日期时间。
 *
 * 支持：
 * 2026-07-13T18:00:00+08:00
 * 2026-07-13T18:00:00
 * 2026-07-13 18:00:00
 */
export function parseDateTime(value) {
  if (!value) {
    return null
  }

  let text = String(value).trim()

  // Safari 对 YYYY-MM-DD HH:mm:ss 的兼容性不好，
  // 转换为 YYYY-MM-DDTHH:mm:ss。
  if (/^\d{4}-\d{2}-\d{2}\s/.test(text)) {
    text = text.replace(' ', 'T')
  }

  const date = new Date(text)

  if (Number.isNaN(date.getTime())) {
    return null
  }

  return date
}

/**
 * 将 HH:mm 或 HH:mm:ss 设置为指定日期的时间。
 */
export function parseTimeOnDate(timeValue, baseDate = new Date()) {
  if (!timeValue) {
    return null
  }

  const text = String(timeValue).trim()
  const parts = text.split(':')

  const hour = Number(parts[0])
  const minute = Number(parts[1] || 0)
  const second = Number(parts[2] || 0)

  if (
    !Number.isInteger(hour) ||
    !Number.isInteger(minute) ||
    !Number.isInteger(second) ||
    hour < 0 ||
    hour > 23 ||
    minute < 0 ||
    minute > 59 ||
    second < 0 ||
    second > 59
  ) {
    return null
  }

  const date = new Date(baseDate)
  date.setHours(hour, minute, second, 0)

  return date
}

/**
 * 兼容旧代码。
 */
export function parseTodayTime(timeValue) {
  return parseTimeOnDate(timeValue, new Date())
}

/**
 * 计算后端时间与浏览器时间的偏差。
 *
 * 返回值示例：
 * 后端比浏览器快 5 秒，返回 5000。
 */
export function getServerTimeOffset(serverTime) {
  const serverDate = parseDateTime(serverTime)

  if (!serverDate) {
    return 0
  }

  return serverDate.getTime() - Date.now()
}

/**
 * 获取校正后的当前时间。
 */
export function getCorrectedNow(serverTimeOffset = 0) {
  return new Date(Date.now() + Number(serverTimeOffset || 0))
}

/**
 * 获取真正的结束日期时间。
 *
 * endTime 可以是：
 * 18:00
 * 18:00:00
 * 2026-07-13T18:00:00+08:00
 *
 * validUntil 可以是：
 * 2026-07-13
 */
export function resolveAccessEndDate({
  endTime,
  validUntil,
  now = new Date(),
  accessStartTime
}) {
  if (!endTime) {
    return null
  }

  // 后端直接返回完整日期时间时，直接使用。
  if (hasDatePart(endTime)) {
    return parseDateTime(endTime)
  }

  let baseDate = new Date(now)

  // 有授权截止日期时，以授权截止日期为准。
  if (validUntil) {
    const dateText = String(validUntil).substring(0, 10)
    const dateParts = dateText.split('-').map(Number)

    if (
      dateParts.length === 3 &&
      dateParts.every(Number.isInteger)
    ) {
      baseDate = new Date(
        dateParts[0],
        dateParts[1] - 1,
        dateParts[2],
        0,
        0,
        0,
        0
      )
    }
  }

  let endDate = parseTimeOnDate(endTime, baseDate)

  if (!endDate) {
    return null
  }

  /*
   * 处理跨天时间段，例如：
   * 访问时间 22:00 - 02:00
   *
   * 当结束时间小于等于开始时间时，
   * 说明结束时间在第二天。
   */
  if (accessStartTime && !validUntil) {
    const startDate = parseTimeOnDate(accessStartTime, baseDate)

    if (startDate && endDate.getTime() <= startDate.getTime()) {
      const nowTime = now.getTime()

      // 当前处于凌晨跨天区间时，结束时间应为今天 02:00。
      if (nowTime < startDate.getTime()) {
        return endDate
      }

      endDate = new Date(endDate.getTime() + DAY_MS)
    }
  }

  return endDate
}

/**
 * 计算距离授权结束还有多少秒。
 */
export function getSecondsUntil(
  endTime,
  {
    validUntil,
    accessStartTime,
    serverTimeOffset = 0
  } = {}
) {
  const now = getCorrectedNow(serverTimeOffset)

  const endDate = resolveAccessEndDate({
    endTime,
    validUntil,
    accessStartTime,
    now
  })

  if (!endDate) {
    return 0
  }

  const remainingMilliseconds = endDate.getTime() - now.getTime()

  // ceil 防止倒计时提前少一秒。
  return Math.max(
    0,
    Math.ceil(remainingMilliseconds / SECOND_MS)
  )
}

/**
 * 格式化秒数。
 *
 * 小于一天：02:05:09
 * 超过一天：2天 02:05:09
 */
export function formatSeconds(totalSeconds) {
  const seconds = Math.max(
    0,
    Math.ceil(Number(totalSeconds) || 0)
  )

  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainSeconds = seconds % 60

  const timeText = [
    String(hours).padStart(2, '0'),
    String(minutes).padStart(2, '0'),
    String(remainSeconds).padStart(2, '0')
  ].join(':')

  return days > 0 ? `${days}天 ${timeText}` : timeText
}