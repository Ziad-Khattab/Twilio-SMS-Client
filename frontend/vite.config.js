import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    svelte(),
    tailwindcss()
  ],
  build: {
    outDir: path.resolve(__dirname, '../src/main/webapp'),
    emptyOutDir: false // Prevent deleting WEB-INF/web.xml
  }
})
