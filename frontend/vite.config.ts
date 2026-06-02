import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 自动按需引入 Element Plus 的 API(如 ElMessage)及其样式
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    // 自动按需注册模板用到的 Element Plus 组件及其样式;dirs 置空,本地组件仍用显式 import
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
      dirs: [],
    }),
  ],
  resolve: {
    alias: {
      // @ 指向 src,简化导入路径
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 开发期把 /api 转发到后端,避免跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
