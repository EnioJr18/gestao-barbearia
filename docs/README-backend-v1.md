# Barbeiro Financeiro — Backend

API local de controle financeiro para barbeiro.

## Stack

- Java 21 (Gradle Toolchain)
- Spring Boot 4.1.0
- Gradle Kotlin DSL
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- PostgreSQL 17
- Flyway
- Docker Compose

## Princípios

- Flyway é a fonte oficial de evolução do schema.
- Hibernate usa `ddl-auto=validate` e não cria/atualiza o banco.
- Valores monetários usam `BigDecimal`/`NUMERIC(12,2)`.
- Datas financeiras usam `LocalDate`/`DATE`.
- Auditoria usa `Instant`/`TIMESTAMPTZ`.
- UUID é o identificador público das entidades.
- Histórico financeiro não depende do estado atual de serviços/categorias.

## Estrutura inicial

```text
src/main/java/br/com/barbeirofinanceiro/
├── BarbeiroFinanceiroApplication.java
└── domain/
    ├── backup/
    ├── categoria/
    ├── despesarecorrente/
    ├── fechamentodiario/
    ├── movimentacao/
    ├── servico/
    └── usuario/

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__criacao_schema_inicial.sql
```

## Ambiente

Consulte [SETUP-INTELLIJ.md](SETUP-INTELLIJ.md) para configurar Java 21 no IntelliJ IDEA 2025.3.3 sem alterar o Java 25 global.

## Banco local

Subir PostgreSQL:

```bash
docker compose up -d
```

Dados de desenvolvimento:

```text
host: localhost
port: 5432
database: barbeiro_financeiro
user: barbeiro
password: barbeiro
```

## Gradle / Java 21

O projeto fixa Java 21 via toolchain, portanto não é necessário trocar o Java global da máquina. A versão de Gradle escolhida para o projeto deve ser 9.x (o Spring Boot 4.1 exige Gradle 8.14+ ou 9.x).

Validar após o Wrapper estar disponível:

```bash
./gradlew --version
./gradlew test
./gradlew build
```

No Windows PowerShell/CMD:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## Migração

Ao iniciar a aplicação com PostgreSQL disponível, o Flyway executa a V1 automaticamente.

## Domínio V1

Entidades:

- Usuario
- Categoria
- Servico
- Movimentacao
- DespesaRecorrente
- FechamentoDiario
- BackupExecucao

Enums:

- TipoCategoria
- TipoMovimentacao
- OrigemMovimentacao
- FormaPagamento
- Periodicidade
- BackupTipo
- BackupStatus

## Regras importantes

1. Receita usa categoria `RECEITA` e despesa usa categoria `DESPESA`.
2. Movimentação salva snapshot do serviço quando vinculada a um serviço.
3. Despesa recorrente é configuração; cada ocorrência gera uma movimentação independente.
4. O banco impede duas ocorrências da mesma despesa recorrente na mesma data.
5. Fechamento diário é único por data.
6. Valores de dashboard/relatórios são derivados de movimentações e não persistidos como fonte primária.
7. Entidades de configuração usam ativação/desativação em vez de remoção física quando necessário.

## Próximas etapas

1. Completar os mappings JPA.
2. Adicionar repositories.
3. Criar camada de configuração do banco e testes de contexto.
4. Implementar autenticação.
5. Implementar serviços de domínio.
6. Implementar API REST.
7. Integrar com o frontend React.
8. Implementar backup/restauração.
