const DEFAULT_HOME_URL = 'http://127.0.0.1:5173/'
const HOME_URL_KEY = 'homeUrl'

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
  // 1. 读取用户配置,异常时回退到本地默认地址
  const stored = await chrome.storage.sync.get({ [HOME_URL_KEY]: DEFAULT_HOME_URL })
  const homeUrl = String(stored[HOME_URL_KEY] || DEFAULT_HOME_URL).trim()

  // 2. 只允许 HTTP(S),避免把新标签页带到不可访问的内部协议
  if (isValidHomeUrl(homeUrl)) {
    window.location.replace(homeUrl)
  }
}

document.getElementById('optionsButton')?.addEventListener('click', () => {
  chrome.runtime.openOptionsPage()
})

openHomePage()
