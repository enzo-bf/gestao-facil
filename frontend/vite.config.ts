import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.API_PROXY_TARGET ?? "http://localhost:8080",
        changeOrigin: true,
      },
      "/actuator": {
        target: process.env.API_PROXY_TARGET ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) return undefined;
          if (id.includes("/react/") || id.includes("/react-dom/") || id.includes("/scheduler/")) return "react-vendor";
          if (id.includes("/react-router-dom/") || id.includes("/@remix-run/")) return "router-vendor";
          if (id.includes("/@tanstack/") || id.includes("/axios/")) return "data-vendor";
          if (id.includes("/react-hook-form/") || id.includes("/@hookform/") || id.includes("/zod/")) return "forms-vendor";
          if (id.includes("/lucide-react/")) return "icons-vendor";
          return undefined;
        },
      },
    },
  },
});
