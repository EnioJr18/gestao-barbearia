# Barbeiro Financeiro — Backend

API Java 21/Spring Boot do monorepo Gestão Barbeiro.

## Stack atual

- Spring Boot 4.1, Gradle Kotlin DSL e Gradle Wrapper.
- PostgreSQL 17, Flyway V1–V3 e JPA/Hibernate com `ddl-auto=validate`.
- Spring Security com JWT e BCrypt.
- Testcontainers com PostgreSQL 17 para testes de integração.

## Execução

A configuração principal não possui credenciais fixas. Defina `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET` antes de executar localmente:

```powershell
.\gradlew.bat bootRun
```

Para testes e build:

```powershell
.\gradlew.bat clean test --no-daemon
.\gradlew.bat clean build --no-daemon
```

Consulte o [README da raiz](../README.md) para Compose, backup/restauração e configuração completa do monorepo.
