# Java API (Spring Boot)

Esta pasta contem a migracao do backend NestJS para Java com Spring Boot.

## Modulos migrados
- users CRUD basico (create, list, get by id)
- auth login com JWT
- products CRUD com paginacao/filtro e atualizacao de estoque
- orders criacao de pedidos com baixa de estoque e listagem paginada
- dashboard metricas
- seeder para dados iniciais

## Requisitos
- Java 17+
- Maven 3.9+
- MySQL

## Rodar
```bash
cd java-api
mvn spring-boot:run
```

A API sobe por padrao na porta 3000 para manter compatibilidade com o frontend atual.

## Variaveis de ambiente
- PORT (default: 3000)
- DB_HOST (default: localhost)
- DB_PORT (default: 3306)
- DB_USER (default: root)
- DB_PASS (default: vazio)
- DB_NAME (default: inventory)
- JWT_SECRET (default: dev_secret_dev_secret_dev_secret_32)
