# 个人导航新标签页 Chrome 扩展

这个本地扩展会接管 Chrome 的“新建标签页”,并跳转到个人导航首页。

## 加载方式

1. 打开 Chrome 地址栏输入 `chrome://extensions/`
2. 打开右上角“开发者模式”
3. 点击“加载已解压的扩展程序”
4. 选择本目录: `chrome-extension/personal-nav-new-tab`
5. 新建一个标签页验证跳转

## 修改跳转地址

默认地址是:

```text
http://127.0.0.1:5173/
```

如果端口或部署域名变化,在 `chrome://extensions/` 找到“个人导航新标签页”,点击“详情”里的“扩展程序选项”,填写新的首页地址。

## Chrome 限制

- `chrome_url_overrides.newtab` 只能指向扩展包内的 HTML 文件,所以扩展会先打开 `newtab.html`,再跳转到个人导航地址。
- 同一个扩展一次只能覆盖新标签页、历史记录、书签页其中一种页面。
- 隐身窗口中的新标签页不会被扩展覆盖。
