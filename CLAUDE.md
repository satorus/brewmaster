# BrewMaster — Claude Code Instructions

## Project
Full-stack homebrewing app. Spec is in `BrewMaster_App_Specification.md`.
Always read the spec before making architectural decisions.

## Structure
- `/backend` — Spring Boot 3 / Java 21 / Maven
- `/frontend` — Angular 17+ standalone components
- `/docker-compose.yml` — PostgreSQL + backend

## Key Rules
- Java: constructor injection, records for DTOs, @Transactional on service writes
- Angular: standalone components only, signals for state, reactive forms
- DB: ALL schema changes via Flyway migrations in `backend/src/main/resources/db/migration/`
- Never hardcode secrets — use environment variables from `.env`
- Never use `spring.jpa.hibernate.ddl-auto=create` or `update`

## Commands
- Start DB: `docker compose up -d postgres`
- Start backend: `cd backend && mvn spring-boot:run`
- Start frontend: `cd frontend && ng serve`