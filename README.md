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

- Java 21 (necessário para executar e testar o backend fora do Docker).
- Docker Desktop com Docker Compose v2.
- Node.js e npm para executar o frontend Vite.
- Cliente PostgreSQL (`pg_dump`/`pg_restore`) somente para usar backup/restauração quando a API for executada fora do Docker; a imagem do backend já inclui essas ferramentas.

## Configuração local

Copie o modelo de variáveis e preencha todos os valores obrigatórios:

```powershell
Copy-Item .env.example .env
```

`.env.example` contém valores de exemplo e placeholders; ajuste-os para seu ambiente local. Nunca versione `.env`, senhas ou `JWT_SECRET`.

Para apontar o frontend para outro endereço da API, copie o modelo específico do Vite:

```powershell
Copy-Item front/.env.example front/.env.local
```

Configure `VITE_API_URL` com a URL completa da API, incluindo `/api/v1`. Quando a
variável não é definida, o frontend usa `http://localhost:8080/api/v1`, mantendo o
comportamento padrão de desenvolvimento. Variáveis `VITE_*` são incorporadas ao
bundle do navegador e não devem conter segredos.

Instale as dependências usando o lockfile e inicie o Vite separadamente:

```powershell
Set-Location front
npm ci
npm run dev
```

Por padrão, o Vite fica disponível em `http://localhost:5173`.

`JWT_SECRET` é obrigatório e precisa ter pelo menos 32 bytes UTF-8. Gere e armazene esse segredo em um gerenciador de senhas ou mecanismo seguro de configuração; não o coloque no frontend nem em arquivos versionados.

## Executar com Docker Compose

Com `.env` preenchido, o Compose inicia somente PostgreSQL e backend. As portas publicadas são vinculadas a `127.0.0.1` e destinam-se ao acesso local:

```powershell
docker compose up --build -d
docker compose ps
```

O backend aguarda o healthcheck do PostgreSQL e também possui healthcheck próprio. O estado `healthy` aparece em `docker compose ps`. A API fica disponível em `http://127.0.0.1:8080` e o PostgreSQL em `127.0.0.1:5432` (ou nas portas definidas por `BACKEND_PORT` e `POSTGRES_PORT`). O Compose não inicia o frontend.

Para acompanhar os logs e interromper o ambiente sem remover o volume persistente:

```powershell
docker compose logs -f back postgres
docker compose down
```

O pgAdmin é opcional, restrito a `127.0.0.1` e associado ao perfil `dev-tools`. Defina `PGADMIN_EMAIL` e `PGADMIN_PASSWORD` no `.env` e inicie o perfil:

```powershell
docker compose --profile dev-tools up --build
```

Ele fica disponível em `http://127.0.0.1:5050` (ou na porta `PGADMIN_PORT`). Sem `--profile dev-tools`, o serviço não é iniciado.

### CORS

O ambiente local autoriza `http://localhost:5173` por padrão. `CORS_ALLOWED_ORIGINS` permite adicionar outras origens, separadas **somente por vírgula**, por exemplo `https://app.exemplo.com` ou `http://localhost:5173,https://app.exemplo.com`. Cada entrada deve ser uma origem HTTP/HTTPS explícita, sem caminho e sem wildcard (`*`).

Em produção, ative o perfil Spring `prod` (ou `production`) e configure explicitamente `CORS_ALLOWED_ORIGINS` com as origens reais do frontend; a aplicação falha na inicialização se a lista estiver ausente, inválida ou contiver localhost/loopback. CORS controla quais navegadores podem ler respostas e não substitui o JWT ou as regras de autorização.

### Healthchecks

- PostgreSQL: `pg_isready` verifica disponibilidade usando usuário e banco definidos para o container, sem senha no comando.
- Backend: `curl` consulta `GET /actuator/health/readiness` dentro do container. O Actuator expõe somente health, e a resposta não inclui componentes ou detalhes internos. Esse endpoint GET específico é público para permitir a verificação; as rotas operacionais continuam protegidas por JWT.
- O backend e o pgAdmin aguardam o PostgreSQL ficar saudável antes de iniciar.

## Backend fora do Docker

Defina as variáveis de conexão e JWT antes de iniciar:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

Para execução fora do Docker, configure esses valores no ambiente de execução (ou na configuração protegida da IDE); para o banco local, use `localhost` e a porta publicada do PostgreSQL. O CORS local mantém `http://localhost:5173` por padrão. Então:

```powershell
# Execute na raiz do repositório para iniciar somente o banco:
docker compose up -d postgres

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

O sistema possui dois perfis: `ADMIN` e `OPERADOR`. Ambos acessam os dados
operacionais compartilhados da barbearia. O `ADMIN` pode consultar e administrar
contas, criar usuários e definir perfis. O `OPERADOR` pode acessar a operação e
autogerenciar seus próprios dados, mas não pode administrar outras contas.
O cadastro público foi removido: usuários novos são criados por um administrador
em `POST /api/v1/usuarios` e recebem perfil `OPERADOR` por padrão.
As rotas administrativas são `GET /api/v1/usuarios`, `GET` e `PUT`
`/api/v1/usuarios/{id}`, além de `PATCH /api/v1/usuarios/{id}/ativar` ou
`PATCH /api/v1/usuarios/{id}/desativar`. O campo `perfil` só é aceito nos DTOs
administrativos. A conta própria continua em `GET/PUT /api/v1/usuarios/me`,
`PUT /api/v1/usuarios/me/senha` e `PATCH /api/v1/usuarios/me/desativar`.

### Provisionar o primeiro administrador

O provisionamento é um comando separado, executado sem servidor HTTP. Não é
executado durante a inicialização normal. Primeiro configure a conexão local do
PostgreSQL e `JWT_SECRET`, como no `.env`, e depois defina temporariamente no
ambiente do PowerShell:

```powershell
$env:ADMIN_PROVISIONING_CONFIRM = 'true'
$env:ADMIN_PROVISIONING_ACTION = 'create'
$env:ADMIN_PROVISIONING_NAME = 'Nome do administrador'
$env:ADMIN_PROVISIONING_EMAIL = 'admin@exemplo.invalid'
$secure = Read-Host 'Senha temporária (mínimo 12 caracteres)' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
  $env:ADMIN_PROVISIONING_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
  Set-Location back
  .\gradlew.bat provisionAdmin --no-daemon
} finally {
  Remove-Item Env:ADMIN_PROVISIONING_CONFIRM, Env:ADMIN_PROVISIONING_ACTION,
    Env:ADMIN_PROVISIONING_NAME, Env:ADMIN_PROVISIONING_EMAIL,
    Env:ADMIN_PROVISIONING_PASSWORD -ErrorAction SilentlyContinue
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
  $secure.Dispose()
}
```

Para uma instalação existente sem administrador ativo, é possível promover uma
conta explicitamente escolhida: defina `ADMIN_PROVISIONING_CONFIRM=true`,
`ADMIN_PROVISIONING_ACTION=promote` e `ADMIN_PROVISIONING_USER_ID=<UUID>` e execute
o mesmo comando. Obtenha o UUID com uma consulta somente de leitura à tabela
`usuarios` e confira cuidadosamente a conta selecionada antes da execução;
contas inexistentes ou inativas são recusadas. Em ambos os modos, a operação
falha se já houver um `ADMIN` ativo. O perfil de provisionamento e a ausência de
servidor HTTP são fixados pela tarefa `provisionAdmin`. Após sucesso, as variáveis
temporárias devem ser removidas; confirme o perfil pela listagem administrativa
após iniciar a API normalmente.

## Backup e restauração

Backups são arquivos locais controlados pelo backend, registrados no banco e armazenados em `backups/`. A restauração aceita somente backups registrados com sucesso pelo sistema. Depois de uma restauração bem-sucedida, a API permanece em manutenção e requer reinício do ambiente antes de voltar a atender requisições.

## Estado atual

O backend possui caixa, itens, clientes, vendas, despesas, relatórios, autenticação, usuários, backup/restauração e dashboard. O Compose é voltado ao desenvolvimento local; o frontend é executado separadamente pelo Vite. Não use valores de produção ou credenciais reais em arquivos `VITE_*`, pois eles são incorporados ao bundle público.

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.


## 👨‍💻 Autor

Desenvolvido por **Enio Jr.** e **David Gabriel**.
