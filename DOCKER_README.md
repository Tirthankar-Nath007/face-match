# Docker Setup for Face Match Application

This project includes Docker configuration to run the application without needing Java 17 or Maven installed locally.

## Prerequisites

- [Docker](https://www.docker.com/products/docker-desktop) installed
- [Docker Compose](https://docs.docker.com/compose/install/) installed

## Configuration

### 1. Configure Environment Variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

Edit `.env` with your actual configuration:

```env
# Database Configuration (your external Oracle DB)
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@your-db-host:1521/your-service
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=oracle.jdbc.OracleDriver

# Digio Configuration
DIGIO_CLIENT_ID=your_digio_client_id
DIGIO_CLIENT_SECRET=your_digio_client_secret
DIGIO_CALLBACK_URL=http://your-callback-url:8082/api/v1/webhook

# Security Configuration
SECURITY_API_KEY=your_api_key
SECURITY_JWT_SECRET=your_jwt_secret_key_here_min_256_bits_long
SECURITY_JWT_EXPIRATION_MS=86400000
```

## Running the Application

### Build and Run

```bash
# Build and start the container
docker compose up --build -d

# View logs
docker compose logs -f

# Stop the container
docker compose down
```

## Useful Commands

```bash
# Rebuild after code changes
docker compose build --no-cache
docker compose up -d

# Restart the service
docker compose restart fm-app

# View logs
docker compose logs -f face-match-app
```
