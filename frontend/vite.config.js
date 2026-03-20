import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    watch: {
      usePolling: true,
    },
    // Fix cho Vite 5: Chấp nhận mọi host gửi yêu cầu đến
    cors: true,
    hmr: {
      clientPort: 80, // Ép HMR chạy qua cổng 80 của Nginx
      host: 'localhost',
    },
    proxy: {
      // Sửa tên service thành localhost cho môi trường local
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});