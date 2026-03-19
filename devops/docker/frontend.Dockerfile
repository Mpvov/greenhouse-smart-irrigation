# =============================================================================
# frontend.Dockerfile — Development server for React + Vite
#
# Strategy for Prototype: Run Vite's hot-reload dev server directly.
# This is ideal for prototype/development because:
#   - Hot Module Replacement (HMR) works out of the box
#   - No need to rebuild image on every code change (mount src as volume)
#   - Fast iteration cycle
#
# NOTE: For Production, swap this for a 2-stage build:
#   Stage 1 (node): npm run build → /dist
#   Stage 2 (nginx:alpine): serve /dist as static files
# =============================================================================

FROM node:20-alpine

WORKDIR /app

# Copy dependency manifests first (cached layer)
COPY package.json package-lock.json* ./

# Install all dependencies
RUN npm install

# Copy the rest of the source code
# (In docker-compose, src/ is mounted as a volume for live reload)
COPY . .

EXPOSE 5173

# Vite dev server — host 0.0.0.0 is set in vite.config.js to bind all interfaces
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
