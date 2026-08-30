# Gestão Barbeiro — Monorepo

Monorepo local com backend Spring Boot, frontend React/Vite e PostgreSQL executado via Docker.

## Estrutura

- `back/`: API Java 21 / Spring Boot 4.1.0, Gradle 9, JPA e Flyway.
- `front/`: aplicação React com Vite.
- `docs/`: documentação técnica existente do backend.
- `docker-compose.yml`: configuração única do ambiente local.

## Backend local

Com PostgreSQL local disponível em `localhost:5432`, execute:

```powershell
Set-Location back
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

O `application.yml` usa o banco local `barbeiro_financeiro` e mantém `ddl-auto: validate`.

## Ambiente completo com Docker

Copie `.env.example` para `.env` e ajuste os valores locais, se necessário:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Serviços padrão:

- Backend: http://localhost:8080
- Frontend Vite: http://localhost:5173
- PostgreSQL: localhost:5432
- pgAdmin: http://localhost:5050

O backend usa `postgres:5432` somente dentro da rede Docker. Fora do Docker, mantém `localhost:5432`.
