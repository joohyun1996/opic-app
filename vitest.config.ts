import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
  },
  resolve: {
    // Next.js의 @/ alias를 vitest에서도 동일하게 사용
    alias: {
      '@': path.resolve(__dirname, '.'),
    },
  },
})
