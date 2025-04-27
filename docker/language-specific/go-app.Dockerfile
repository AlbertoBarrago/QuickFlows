# Dockerfile for Go applications

# Stage 1: Build stage
FROM golang:1.19-alpine AS build

# Set working directory
WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git ca-certificates tzdata && \
    update-ca-certificates

# Download dependencies first (for better caching)
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build the application with optimizations
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o /go/bin/app ./cmd/main.go

# Stage 2: Production stage
FROM alpine:3.16

# Add necessary runtime packages
RUN apk --no-cache add ca-certificates tzdata && \
    update-ca-certificates

# Create a non-root user to run the application
RUN adduser -D -g '' appuser

# Set working directory
WORKDIR /app

# Copy the binary from the build stage
COPY --from=build /go/bin/app /app/

# Copy configuration files if needed
COPY --from=build /app/configs /app/configs

# Use the non-root user for security
USER appuser

# Expose application port
EXPOSE 8080

# Set environment variables
ENV GIN_MODE=release

# Run the application
CMD ["/app/app"]

# Usage:
# Build: docker build -t go-app:latest -f go-app.Dockerfile .
# Run: docker run -p 8080:8080 go-app:latest

# Benefits:
# 1. Multi-stage build reduces final image size dramatically
# 2. Non-root user improves security
# 3. Alpine base provides minimal attack surface
# 4. Static compilation eliminates runtime dependencies
# 5. Build flags optimize binary size