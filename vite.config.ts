import {defineConfig} from 'vite'
import {svelte} from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
    root: "web",
    base: "/static/",
    build: {
        outDir: "../backend/src/main/resources/static",
        emptyOutDir: true,
    },
    plugins: [
        svelte(),
        tailwindcss(),
    ],
    server: {
        port: 8080,
        proxy: {
            "/api": {
                target: "http://localhost:8081",
                rewrite: (path) => path,
            },
        },
    },
})
