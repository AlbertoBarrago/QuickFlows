# Multi-stage build example for Node.js applications

# Stage 1: Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build the application
COPY . .
RUN npm run build

# Stage 2: Production stage
FROM node:18-alpine AS production

WORKDIR /app

# Set environment to production
ENV NODE_ENV=production

# Copy package files and install only production dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy built application from build stage
COPY --from=build /app/dist /app/dist

# Expose port
EXPOSE 3000

# Start the application
CMD ["node", "dist/index.js"]

# Usage:
# Build: docker build -t my-node-app:latest -f node-app.Dockerfile .
# Run: docker run -p 3000:3000 my-node-app:latest

# Benefits of multi-stage builds:
# 1. Smaller final image size (no dev dependencies or build tools)
# 2. Improved security (fewer packages = smaller attack surface)
# 3. Better organization of build process
# 4. Faster deployment and download times