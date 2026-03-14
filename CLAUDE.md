# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Campus Medical Management System - a full-stack medical management system for campuses using:
- **Frontend**: React 18 + TypeScript + Vite
- **Backend**: Python 3.11 + FastAPI
- **Databases**: MySQL 8.0 (relational) + MongoDB (document/NoSQL)
- **Containerization**: Docker + Docker Compose

All services (frontend, backend, MySQL, MongoDB) run in Docker containers for development and production.

---

## Development Commands

### Docker (Recommended - All Services)

```bash
# Build and start all services (first time or after changes)
docker-compose up --build

# Start services (if already built)
docker-compose up -d

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
docker-compose logs -f mongo

# Stop all services
docker-compose down

# Stop and remove volumes (deletes database data)
docker-compose down -v

# Restart a specific service
docker-compose restart backend
```

### Local Development (Without Docker)

#### Backend
```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Testing & Linting

#### Backend
```bash
cd backend
# Run tests
pytest

# Run specific test file
pytest tests/test_api/test_auth.py

# Run with coverage
pytest --cov=app tests/

# Lint (flake8, black, mypy configured)
flake8 app/
black --check app/
mypy app/
```

#### Frontend
```bash
cd frontend
# Type check
npm run type-check

# Lint
npm run lint

# Build for production
npm run build

# Preview production build
npm run preview
```

---

## Architecture

### Backend Structure

```
backend/app/
├── main.py                 # FastAPI app entry point, middleware registration
├── api/v1/                 # API routes (auth, health, etc.)
├── core/                   # Core configuration and utilities
│   ├── config.py          # Pydantic Settings for environment variables
│   ├── deps.py            # Dependency injection (auth, pagination)
│   ├── security.py        # JWT tokens, password hashing
│   ├── logger.py          # Structured logging configuration
│   └── exceptions.py      # Custom exceptions and error handlers
├── db/                     # Database connections
│   ├── mysql.py           # SQLAlchemy ORM (connection pool configured)
│   └── mongo.py           # Motor async MongoDB client (connection pool configured)
├── models/mysql/           # SQLAlchemy ORM models
├── schemas/                # Pydantic schemas for request/response validation
├── services/               # Business logic layer
├── middleware/             # Custom middleware (logging, etc.)
└── utils/                  # Utility functions
```

**Key patterns:**
- `app/core/config.py` - All environment variables managed via `Settings` class (pydantic-settings)
- `app/core/deps.py` - FastAPI dependencies for `get_current_user_id`, `require_roles()`, `PaginationParams`
- `app/core/exceptions.py` - Custom exceptions (`AppException`, `AuthenticationException`, etc.) with unified error response format
- `app/core/security.py` - JWT token creation/verification, password hashing with bcrypt
- `app/db/mysql.py` - Connection pool: `pool_size=10`, `max_overflow=20`, `pool_recycle=3600`
- `app/db/mongo.py` - Async Motor client: `maxPoolSize=50`, `minPoolSize=10`
- Global exception handlers registered in `main.py` for consistent error responses

### Frontend Structure

```
frontend/src/
├── main.tsx               # Entry point
├── App.tsx                # Root component
├── router/                # React Router configuration
├── pages/                 # Page components
├── components/            # Reusable components
│   ├── common/           # Generic components
│   └── layout/           # Layout components
├── services/              # API services
│   └── request.ts        # Axios instance with interceptors, token refresh
├── store/                 # State management
├── types/                 # TypeScript type definitions
└── utils/                 # Utility functions
```

**Key patterns:**
- `services/request.ts` - Axios wrapper with:
  - Automatic JWT token injection from `localStorage`
  - Token refresh on 401 responses
  - Unified error handling
  - Request ID generation
- Environment variables prefixed with `VITE_` (e.g., `VITE_API_BASE_URL`)

### Docker Services

```yaml
mysql:      port 3306, init script: docker/mysql/init.sql
mongo:      port 27017, init script: docker/mongo/init.js
backend:    port 8000, depends_on mysql+mongo (healthcheck)
frontend:   port 3000, depends_on backend
```

**Important:** Backend uses service names (`mysql`, `mongo`) as hostnames when running in Docker, not `localhost`.

---

## Database Initialization

### MySQL
- Initialization script: `docker/mysql/init.sql`
- Creates tables: `users`, `roles`, `departments`, `doctors`, `appointments`, `medicines`, `prescriptions`, `prescription_items`
- Default admin: `admin` / `admin123` (bcrypt hash in init.sql)

### MongoDB
- Initialization script: `docker/mongo/init.js`
- Creates collections: `medical_records`, `lab_results`, `medical_images`, `system_logs`, `audit_logs`, `notifications`, `health_profiles`, `health_metrics`, `cache`, `sessions`
- Creates app user: `medical_app`

---

## API Routes

- Health check: `GET /api/v1/health` (checks MySQL + MongoDB connectivity)
- API docs: `GET /api/v1/docs` (Swagger UI)
- Root: `GET /`

---

## Environment Variables

Copy `.env.example` to `.env` before running. Key variables:

```bash
# MySQL
MYSQL_HOST=mysql          # Use service name in Docker
MYSQL_PORT=3306
MYSQL_DATABASE=campus_medical
MYSQL_USER=medical_user
MYSQL_PASSWORD=medical_password

# MongoDB
MONGO_HOST=mongo          # Use service name in Docker
MONGO_PORT=27017
MONGO_INITDB_ROOT_USERNAME=mongo_user
MONGO_INITDB_ROOT_PASSWORD=mongo_password
MONGO_DATABASE=campus_medical

# Backend
BACKEND_PORT=8000
SECRET_KEY=your-secret-key
ACCESS_TOKEN_EXPIRE_MINUTES=30
ENVIRONMENT=development

# Frontend
FRONTEND_PORT=3000
VITE_API_BASE_URL=http://localhost:8000/api/v1
```

---

## Default Credentials

- **Admin account**: username `admin`, password `admin123`

---

## CORS Configuration

Configured in `app/main.py` with origins from `settings.BACKEND_CORS_ORIGINS`:
- `http://localhost:3000` (local development)
- `http://frontend:3000` (Docker internal)
