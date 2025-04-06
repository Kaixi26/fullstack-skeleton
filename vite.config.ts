import {defineConfig} from 'vite'
import {svelte} from '@sveltejs/vite-plugin-svelte'
import path from "path";

// https://vite.dev/config/
export default defineConfig({
    root: "web",
    build: {
        outDir: "../backend/src/main/resources/static",
        emptyOutDir: true,
    },
    plugins: [
        svelte(),
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
    resolve: {
        alias: {
            $shadcn: path.resolve("./web/src/shadcn"),
        },
    },
})
