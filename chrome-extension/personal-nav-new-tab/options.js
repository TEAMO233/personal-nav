const DEFAULT_HOME_URL = 'http://127.0.0.1:5173/'
const HOME_URL_KEY = 'homeUrl'

const form = document.getElementById('optionsForm')
const input = document.getElementById('homeUrl')
const resetButton = document.getElementById('resetButton')
const status = document.getElementById('status')

/**
 * 展示保存状态。
 *
 * @param {string} message 状态文案
 * @param {boolean} isError 是否为错误
 */
function showStatus(message, isError = false) {
  // 1. 通过文案和颜色同时反馈保存状态
  status.textContent = message
  status.dataset.state = isError ? 'error' : 'success'
}

/**
 * 规范化首页地址。
 *
 * @param {string} value 用户输入
 * @returns {string} 可保存地址
 */
function normalizeHomeUrl(value) {
  // 1. 用户只输入域名时自动补 http://
  const trimmed = value.trim()
  const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `http://${trimmed}`
  const url = new URL(candidate)

  // 2. 新标签页跳转仅支持普通网页地址
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new Error('unsupported_protocol')
  }

  return url.toString()
}

/**
 * 初始化选项页。
 */
async function initOptions() {
  // 1. 读取已保存地址并填入表单
  const stored = await chrome.storage.sync.get({ [HOME_URL_KEY]: DEFAULT_HOME_URL })
  input.value = stored[HOME_URL_KEY]
}

form.addEventListener('submit', async (event) => {
  event.preventDefault()

  try {
    const homeUrl = normalizeHomeUrl(input.value)
    await chrome.storage.sync.set({ [HOME_URL_KEY]: homeUrl })
    input.value = homeUrl
    showStatus('已保存,下次新建标签页会打开这个地址。')
  } catch {
    showStatus('请输入有效的 http 或 https 地址。', true)
  }
})

resetButton.addEventListener('click', async () => {
  await chrome.storage.sync.set({ [HOME_URL_KEY]: DEFAULT_HOME_URL })
  input.value = DEFAULT_HOME_URL
  showStatus('已恢复默认地址。')
})

initOptions()
