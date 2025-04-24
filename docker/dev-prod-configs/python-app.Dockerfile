# Development and Production Docker configurations for Python applications

# ==========================================
# Development configuration
# ==========================================
# Usage: docker build -t python-app:dev --target development .

FROM python:3.9-slim AS development

WORKDIR /app

# Install development dependencies
COPY requirements.txt requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt

# Copy source code
COPY . .

# Set environment variables
ENV FLASK_APP=app.py
ENV FLASK_ENV=development
ENV FLASK_DEBUG=1

# Expose port
EXPOSE 5000

# Start development server with hot-reload
CMD ["flask", "run", "--host=0.0.0.0", "--reload"]

# ==========================================
# Production configuration
# ==========================================
# Usage: docker build -t python-app:prod --target production .

FROM python:3.9-slim AS production

WORKDIR /app

# Create non-root user for security
RUN adduser --disabled-password --gecos "" appuser

# Install production dependencies only
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt gunicorn

# Copy source code
COPY . .

# Set environment variables
ENV FLASK_APP=app.py
ENV FLASK_ENV=production
ENV FLASK_DEBUG=0

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 5000

# Start production server with gunicorn
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app", "--workers", "4"]

# Key differences between development and production:
# 1. Development includes additional dependencies for testing and debugging
# 2. Development uses Flask's built-in server with hot-reload
# 3. Production uses a non-root user for security
# 4. Production uses gunicorn as a production-ready WSGI server
# 5. Different environment variables for each environment