const DEFAULT_HOME_URL = 'http://127.0.0.1:5173/'
const HOME_URL_KEY = 'homeUrl'

/**
 * DOM 就绪后执行回调。
 *
 * @param {() => void} callback 回调
 */
function onReady(callback) {
  // 1. head 脚本会早于 body 执行,失败兜底需要等 DOM 出现
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', callback, { once: true })
    return
  }

  callback()
}

/**
 * 展示跳转失败兜底界面。
 *
 * @param {string} message 失败原因
 */
function showFallback(message) {
  // 1. 仅在跳转失败时显示界面,正常跳转时保持空白
  onReady(() => {
    document.body.classList.remove('redirecting')
    const fallback = document.getElementById('fallback')
    const fallbackMessage = document.getElementById('fallbackMessage')
    if (fallbackMessage) fallbackMessage.textContent = message
    if (fallback) fallback.hidden = false
  })
}

/**
 * 判断字符串是否为可跳转的 HTTP(S) 地址。
 *
 * @param {string} value 用户配置的地址
 * @returns {boolean} 是否有效
 */
function isValidHomeUrl(value) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

/**
 * 跳转到配置的个人导航首页。
 */
async function openHomePage() {
  try {
    // 1. 读取用户配置,异常时回退到本地默认地址
    const stored = await chrome.storage.sync.get({ [HOME_URL_KEY]: DEFAULT_HOME_URL })
    const homeUrl = String(stored[HOME_URL_KEY] || DEFAULT_HOME_URL).trim()

    // 2. 只允许 HTTP(S),避免把新标签页带到不可访问的内部协议
    if (isValidHomeUrl(homeUrl)) {
      window.location.replace(homeUrl)
      return
    }

    // 3. 配置无效时才显示兜底界面
    showFallback('首页地址无效,请在扩展选项中填写 http 或 https 地址。')
  } catch {
    // 4. 读取配置失败时也给用户一个可操作入口
    showFallback('读取扩展配置失败,请重新打开扩展选项检查首页地址。')
  }
}

onReady(() => {
  document.getElementById('optionsButton')?.addEventListener('click', () => {
    chrome.runtime.openOptionsPage()
  })
})

openHomePage()
