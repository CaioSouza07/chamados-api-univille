# Sistema de Chamados - API Univille

API REST + interface web para gerenciamento de chamados de suporte (clientes, técnicos, categorias, chamados e comentários), desenvolvida com Spring Boot.

## Tecnologias

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Thymeleaf)
- Banco de dados H2 (em memória)
- Springdoc OpenAPI (Swagger UI)
- Maven (com Maven Wrapper incluso)

## Pré-requisitos

- **JDK 21** instalado e configurado no `JAVA_HOME`
  - Para verificar: `java -version`
- **Maven** (opcional): o projeto já traz o Maven Wrapper (`mvnw` / `mvnw.cmd`), então não é obrigatório ter o Maven instalado.
  - Para verificar, caso tenha instalado: `mvn -v`

## Como executar o projeto

### 1. Clonar o repositório

```bash
git clone https://github.com/CaioSouza07/chamados-api-univille.git
```

### 2. Ir até a pasta raiz do projeto

A pasta raiz é a que contém o arquivo `pom.xml`:

```bash
cd chamados-api-univille
```

### 3. Executar a aplicação

#### Windows (CMD)

Com o Maven instalado:

```cmd
mvn spring-boot:run
```

Ou, sem o Maven instalado, usando o Maven Wrapper:

```cmd
mvnw.cmd spring-boot:run
```

#### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux / macOS

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Ou, com o Maven instalado:

```bash
mvn spring-boot:run
```

Quando aparecer no console uma mensagem como `Started ChamadosApplication in X seconds`, a aplicação está no ar.

Para parar a aplicação, pressione `Ctrl + C` no terminal.

## Acessando a aplicação

| Recurso | URL |
|---|---|
| Página inicial (dashboard) | http://localhost:8080/ |
| Tela de chamados | http://localhost:8080/telas/chamados |
| Tela de clientes | http://localhost:8080/telas/clientes |
| Tela de técnicos | http://localhost:8080/telas/tecnicos |
| Tela de categorias | http://localhost:8080/telas/categorias |
| Documentação da API (Swagger) | http://localhost:8080/swagger-ui.html |
| Console do banco H2 | http://localhost:8080/h2-console |

### Acesso ao console H2

- **JDBC URL:** `jdbc:h2:mem:chamadosdb`
- **Usuário:** `sa`
- **Senha:** *(deixar em branco)*

> ⚠️ O banco é **em memória**: todos os dados são apagados sempre que a aplicação é reiniciada.

## Endpoints principais da API

| Recurso | Caminho base |
|---|---|
| Categorias | `/categorias` |
| Clientes | `/clientes` |
| Técnicos | `/tecnicos` |
| Chamados | `/chamados` |
| Comentários de um chamado | `/chamados/{chamadoId}/comentarios` |

A lista completa de endpoints, com exemplos de requisição, está disponível no Swagger.

## Executando os testes

```cmd
mvn test
```

ou, com o wrapper:

```cmd
mvnw.cmd test        (Windows)
./mvnw test          (Linux/macOS)
```

## Gerando o .jar

```cmd
mvn clean package
java -jar target/chamados-0.0.1-SNAPSHOT.jar
```

## Problemas comuns

- **`'mvn' não é reconhecido como um comando interno`**: o Maven não está instalado ou não está no `PATH`. Use `mvnw.cmd spring-boot:run`.
- **Erro de versão do Java (`release version 21 not supported`)**: confira se o `JAVA_HOME` aponta para um JDK 21.
- **`Port 8080 was already in use`**: outra aplicação está usando a porta 8080. Feche essa aplicação ou rode em outra porta:
  ```cmd
  mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
  ```
