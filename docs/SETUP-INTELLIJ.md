# Setup local — IntelliJ IDEA 2025.3.3

## 1. JDK global

Não é necessário remover Java 25.

O projeto foi configurado para usar Java 21 com Gradle Toolchain:

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
```

## 2. IntelliJ

Abra a pasta raiz do projeto (`barbeiro-financeiro-backend`) como projeto Gradle.

Em `Settings > Build, Execution, Deployment > Build Tools > Gradle`:

- Build and run using: `Gradle`
- Run tests using: `Gradle`
- Gradle JVM: Java 21

Em `File > Project Structure`:

- Project SDK: Java 21

Caso Java 21 não esteja instalado, adicione um JDK 21 em `File > Project Structure > SDKs` ou use a opção de download do próprio IntelliJ.

## 3. PostgreSQL

Docker deve estar instalado e em execução.

Na raiz do projeto:

```powershell
docker compose up -d
```

Validar:

```powershell
docker ps
```

## 4. Banco

```text
Host: localhost
Port: 5432
Database: barbeiro_financeiro
Username: barbeiro
Password: barbeiro
```

## 5. Executar

Quando o Gradle Wrapper estiver disponível:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

A aplicação usa:

```text
http://localhost:8080
```

Ao subir o Spring Boot, o Flyway deve executar `V1__criacao_schema_inicial.sql` e o Hibernate deve validar o schema por meio de `ddl-auto=validate`.

## 6. Importante sobre Java 25

Não altere `JAVA_HOME` global só por causa deste projeto. O objetivo é permitir que:

```text
Projeto A → Java 21
Projeto B → Java 25
```

coexistam na mesma máquina.
