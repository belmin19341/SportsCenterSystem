# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
RUN npm install -g pnpm
WORKDIR /app

# Install dependencies (cached layer — only re-runs when lockfile changes)
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Build — VITE_API_BASE_URL is baked in at compile time
COPY . .
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN pnpm build

# ── Stage 2: Serve ────────────────────────────────────────────────────────────
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=20s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1
