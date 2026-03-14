# Repository Guidelines

## Project Structure & Module Organization
- `frontend/` houses the React + TypeScript UI built with Vite. Key areas: `frontend/src/pages`, `frontend/src/components`, and `frontend/src/layouts`. Static assets live in `frontend/src/assets` and public files in `frontend/public/`.
- `backend/` contains the FastAPI service. Core code is under `backend/app/` with submodules such as `api/`, `models/`, `schemas/`, and `services/`.
- `docs/` stores project documentation. `docker/` and `docker-compose.yml` manage containerized services.
- Tests: frontend tests are in `frontend/tests/` and backend tests are in `backend/tests/`.

## Build, Test, and Development Commands
- Frontend dev server: `cd frontend && npm install && npm run dev`
- Frontend build: `cd frontend && npm run build` (TypeScript compile + Vite build)
- Frontend lint/type-check: `cd frontend && npm run lint` or `npm run type-check`
- Frontend tests: `cd frontend && npm run test` (interactive) or `npm run test:run`
- Backend dev server: `cd backend && pip install -r requirements.txt && uvicorn app.main:app --reload`
- Backend tests: `cd backend && python -m pytest`
- Backend tooling (optional): `black .`, `isort .`, `flake8`, `mypy app`
- Docker: `docker-compose up -d` to start all services

## Coding Style & Naming Conventions
- Frontend uses ESLint; follow existing React + TypeScript patterns (PascalCase components, `useX` hooks, `camelCase` variables).
- Backend follows PEP 8 with `black`/`isort` defaults (line length 100) and strict typing per `mypy` settings in `backend/pyproject.toml`.
- Keep filenames aligned to feature intent (e.g., `UserProfile.tsx`, `student_dashboard.html`).

## Testing Guidelines
- Frontend: Vitest + Testing Library. Place tests under `frontend/tests/` or alongside services; use `*.test.tsx` or `*.test.ts`.
- Backend: Pytest with async support; tests live in `backend/tests/` (see `test_api/` and `test_services/`).
- Aim to add tests for new API endpoints and UI flows touched by changes.

## Commit & Pull Request Guidelines
- This workspace does not include git history, so no project-specific commit convention was found. If you initialize git, prefer concise messages like `feat: add student dashboard cards` or `fix: correct vitest setup`.
- PRs should include: a clear description, linked issue (if any), and screenshots for UI changes. Note migrations or env var changes explicitly.

## Security & Configuration Tips
- Configure environment variables via `.env` (see `.env.example` if present). Keep secrets out of version control.
- When running locally, ensure MySQL and MongoDB are available (or use Docker Compose).
