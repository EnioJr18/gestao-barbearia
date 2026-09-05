# Gestão Barbeiro — Monorepo

Aplicação local de gestão financeira para barbearia. O monorepo reúne a API Spring Boot, o frontend React/Vite e o PostgreSQL executado com Docker Compose.

## Stack

- Backend: Java 21, Spring Boot 4.1, Gradle, Spring Data JPA, Flyway, Spring Security e JWT.
- Banco: PostgreSQL 17.
- Frontend: React, TypeScript e Vite.
- Testes de integração: Testcontainers com PostgreSQL 17 real.

## Estrutura

```text
back/                  API e Gradle Wrapper
front/                 interface React/Vite
backups/               backups locais gerados em execução (ignorado pelo Git)
docs/                  documentação técnica existente
docker-compose.yml     ambiente local integrado
.env.example           modelo de configuração local
```

## Requisitos

- Docker Desktop com Docker Compose.
- Java 21 para executar a API fora do Docker.

## Configuração local

Copie o modelo de variáveis e preencha todos os valores obrigatórios:

```powershell
Copy-Item .env.example .env
```

`.env.example` contém apenas placeholders. Nunca versione `.env`, senhas ou `JWT_SECRET`.

`JWT_SECRET` é obrigatório e precisa ter pelo menos 32 bytes UTF-8. No PowerShell, um valor aleatório pode ser gerado assim:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## Executar com Docker Compose

Com `.env` preenchido:

```powershell
docker compose up --build
```

Serviços principais:

- API: `http://localhost:8080`
- Frontend Vite: `http://localhost:5173`
- PostgreSQL: `127.0.0.1:5432`

O pgAdmin é opcional e exclusivo para desenvolvimento/manutenção. Para iniciá-lo, defina `PGADMIN_EMAIL` e `PGADMIN_PASSWORD` no `.env` e use:

```powershell
docker compose --profile dev-tools up --build
```

Ele fica disponível somente em `http://127.0.0.1:5050`.

## Backend fora do Docker

Defina as variáveis de conexão e JWT antes de iniciar:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

Então:

```powershell
Set-Location back
.\gradlew.bat bootRun
```

Flyway executa as migrations versionadas e Hibernate mantém `ddl-auto=validate`; migrations V1, V2 e V3 não devem ser alteradas.

## Testes

Os testes usam PostgreSQL 17 via Testcontainers e exigem Docker disponível:

```powershell
Set-Location back
.\gradlew.bat clean test --no-daemon
.\gradlew.bat clean build --no-daemon
```

## Autenticação

`POST /api/v1/auth/login` é público. As demais rotas exigem JWT Bearer. Tokens são invalidados quando a senha muda e deixam de funcionar quando o usuário é desativado.

## Backup e restauração

Backups são arquivos locais controlados pelo backend, registrados no banco e armazenados em `backups/`. A restauração aceita somente backups registrados com sucesso pelo sistema. Depois de uma restauração bem-sucedida, a API permanece em manutenção e requer reinício do ambiente antes de voltar a atender requisições.

## Estado atual

O backend possui caixa, itens, clientes, vendas, despesas, relatórios, autenticação, usuários, backup/restauração e dashboard. O Compose atual é voltado ao desenvolvimento local; empacotamento definitivo do frontend, reverse proxy, healthchecks e observabilidade ficam para a próxima etapa.

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.


## 👨‍💻 Autor

Desenvolvido por **Enio Jr.** e **David Gabriel**.