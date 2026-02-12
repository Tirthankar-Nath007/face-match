# Docker Setup for Face Match Application

This project includes Docker configuration to run the application without needing Java 17 or Maven installed locally.

## Prerequisites

- [Docker](https://www.docker.com/products/docker-desktop) installed
- [Docker Compose](https://docs.docker.com/compose/install/) installed

## Quick Start

### 1. Configure Environment Variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

Edit `.env` with your actual configuration values.

### 2. Build and Run with Docker Compose

```bash
# Build and start all services (FM app + Oracle DB)
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### 3. Access the Application

- **Face Match API**: http://localhost:8080
- **Oracle Database**: localhost:1521 (FREE PDB)

## Useful Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Just the FM app
docker-compose logs -f fm-app

# Just the Oracle DB
docker-compose logs -f oracle-db
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: destroys database data)
docker-compose down -v
```

### Rebuild After Code Changes

```bash
docker-compose build --no-cache
docker-compose up
```

## Development Workflow

1. Make code changes in your IDE
2. Rebuild the Docker image: `docker-compose build`
3. Restart the service: `docker-compose restart fm-app`

## Connecting to Oracle Database

You can connect to the Oracle database using:

- **Host**: localhost (or oracle-db from docker network)
- **Port**: 1521
- **Service Name**: FREE
- **Username**: system
- **Password**: oracle

Example SQL connection string:
```
jdbc:oracle:thin:@localhost:1521/FREE
```

## Production Deployment

For production, remove the `oracle-db` service and configure your external database connection via environment variables.
