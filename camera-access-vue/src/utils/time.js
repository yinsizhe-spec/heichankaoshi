export function parseTodayTime(time) {
  const [hour, minute] = String(time || '00:00').split(':').map(Number)
  const date = new Date()
  date.setHours(hour || 0, minute || 0, 0, 0)
  return date
}

export function formatSeconds(totalSeconds) {
  const seconds = Math.max(0, Math.floor(totalSeconds))
  const h = String(Math.floor(seconds / 3600)).padStart(2, '0')
  const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0')
  const s = String(seconds % 60).padStart(2, '0')
  return `${h}:${m}:${s}`
}

export function getSecondsUntil(endTime) {
  const end = parseTodayTime(endTime)
  return Math.floor((end.getTime() - Date.now()) / 1000)
}
