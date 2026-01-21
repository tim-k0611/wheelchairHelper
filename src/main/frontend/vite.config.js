import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    base: '/',  // Wichtig für Spring Boot
    root: './',
    server: {
        port: 3000,
        open: true,
        proxy: {
            // Lokale Entwicklung: API-Calls zum Spring Boot Backend
            '/api': {
                target: 'http://localhost:8083',
                changeOrigin: true
            }
        }
    },
    build: {
        outDir: 'dist',
        emptyOutDir: true
    }
});