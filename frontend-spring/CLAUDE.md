# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Campus Medical Frontend (Spring Stack) - a Vue 3 + TypeScript + Vite frontend for the campus medical management system.

Connects to:
- **backend-spring**: Spring Boot API (port 8080)
- **langgraph-app**: Python AI Agent service (port 8001)

---

## Development Commands

### Docker (Recommended)

```bash
# Start with the full stack (from repo root)
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml up -d --build backend-spring langgraph-app frontend-spring

# Build and test in Docker
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml run --rm frontend-spring npm run build
docker compose -f docker-compose.yml -f docker-compose.spring.yml -f docker-compose.frontend-spring.yml run --rm frontend-spring npm run test:run

# Smoke test (checks routes and proxy health)
docker exec campus-medical-frontend-spring node scripts/smoke-frontend-spring.mjs

# Regression test
docker exec campus-medical-frontend-spring node scripts/regression-frontend-spring.mjs
```

### Local Development

```bash
# Install dependencies
npm install

# Start dev server (requires backend services running)
npm run dev

# Type check
npm run type-check

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests (watch mode)
npm run test

# Run tests (single run)
npm run test:run
```

---

## Architecture

### Frontend Structure

```
src/
├── main.ts                    # Entry point, creates Vue app
├── App.vue                    # Root component (renders RouterView)
├── router/
│   ├── index.ts              # Vue Router config with role-based guards
│   └── navigation.ts         # Navigation items for each role
├── layouts/
│   ├── StudentLayout.vue     # Student portal layout with AI assistant
│   ├── DoctorLayout.vue      # Doctor workspace layout
│   └── AdminLayout.vue       # Admin dashboard layout
├── views/
│   ├── student/              # Student role pages
│   ├── doctor/               # Doctor role pages
│   ├── admin/                # Admin role pages
│   ├── HomeView.vue
│   ├── LoginView.vue
│   └── NotFoundView.vue
├── services/
│   ├── http/                 # REST API clients (Axios)
│   │   ├── apiClient.ts     # Axios instance with JWT interceptors
│   │   ├── auth.ts          # Auth endpoints
│   │   ├── session.ts       # Token/user localStorage helpers
│   │   └── *.ts             # Domain-specific services
│   ├── agent/               # AI Agent API clients (fetch)
│   │   ├── agentAssistant.ts # SSE streaming chat with langgraph-app
│   │   └── pharmacy.ts      # Agent pharmacy endpoints
│   └── ws/
│       └── alertSocketClient.ts # SockJS/STOMP WebSocket client
├── stores/
│   ├── auth.ts              # Pinia auth store (user, tokens, login/logout)
│   ├── studentAssistant.ts  # AI assistant panel state
│   └── socket.ts            # WebSocket connection state
├── components/              # Vue components
└── types/                   # TypeScript type definitions
```

### Key Patterns

**API Client (`services/http/apiClient.ts`)**
- Axios instance with automatic JWT token injection
- Token refresh on 401 responses
- Request ID generation for tracing
- Error messages extracted from `response.data.error.message`

**Agent Services (`services/agent/*.ts`)**
- Uses native `fetch()` instead of Axios
- SSE streaming for AI chat (token-by-token response handling)
- FastAPI-style error extraction (`detail` field)

**Authentication Flow**
- JWT tokens stored in `localStorage` (keys: `access_token`, `refresh_token`)
- User info stored under `user_info`
- Role-based routing guards in `router/index.ts`
- Three roles: `student`, `doctor`, `admin`

**WebSocket Alerts (`services/ws/alertSocketClient.ts`)**
- SockJS + STOMP protocol
- Connects to `/ws/alerts` endpoint
- Subscribes to `/topic/alerts`

### Vite Proxy Configuration

Development server proxies requests to backend services:
- `/api/*` → `backend-spring:8080`
- `/agent-api/*` → `langgraph-app:8001` (rewrites path)
- `/ws/*` → `backend-spring:8080` (WebSocket)

Environment variables (`.env.development`):
```
VITE_API_BASE_URL=/api/v1
VITE_API_PROXY_TARGET=http://backend-spring:8080
VITE_AGENT_API_BASE_URL=/agent-api/api/v1
VITE_AGENT_PROXY_TARGET=http://langgraph-app:8001
VITE_WS_BASE_URL=/ws/alerts
VITE_WS_PROXY_TARGET=http://backend-spring:8080
```

---

## Testing

- **Framework**: Vitest + jsdom + @vue/test-utils
- **Setup**: `tests/setup.ts` clears localStorage after each test
- **Run single test**: `npm run test -- src/path/to/file.spec.ts`

---

## Role-Based Routes

| Role | Home Route | Available Pages |
|------|-----------|-----------------|
| student | `/student/dashboard` | dashboard, consultation, pharmacy, appointments, medicine-records |
| doctor | `/doctor/dashboard` | dashboard, schedule |
| admin | `/admin/dashboard` | dashboard, schedules, doctors, students, medicines, settings |

---

## AI Assistant Integration

The student dashboard includes a `CampusAIAssistant` component that:
- Streams chat responses via SSE from `langgraph-app`
- Supports action execution (confirm_appointment, create_medicine_order)
- Maintains chat session history
- Panel visibility controlled by `useStudentAssistantStore`

---

## Default Credentials

Same as backend-spring:
- **Admin**: `admin` / `admin123`
- **Student/Doctor accounts**: Created via admin panel
