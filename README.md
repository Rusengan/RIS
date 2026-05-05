# Driver Service

## Описание проекта

Веб-приложение для диспетчеризации рейсов водителей: OAuth2 (Google), роли DRIVER / DISPATCHER / ADMIN, смены и перерывы, маршруты и точки, расчёт маршрута через внешний провайдер (Google Maps / заглушка), журнал аудита и REST API с OpenAPI.

## Стек

- **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Liquibase, PostgreSQL, Redis (кэш), MapStruct, jjwt, Google API client, Testcontainers.
- **Frontend:** React, TypeScript, Vite, TanStack Query, React Router, Tailwind (если используется в проекте).

## Архитектура

Приложение следует **гексагональной архитектуре**: домен и порты отделены от адаптеров (JPA, HTTP, внешние API). Команды и запросы разделены по стилю **CQRS**: изменение состояния через command handlers, чтение — через query-сервисы и репозитории со спецификациями.

## Схема БД

См. [docs/er-diagram.md](docs/er-diagram.md) и Liquibase changes в `src/main/resources/db/changelog/changes/`.

## Запуск

1. Скопируйте переменные окружения: `cp .env.example .env` и заполните как минимум `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_MAPS_API_KEY`, `JWT_SECRET` (длинная случайная строка).
2. Поднимите инфраструктуру: `docker-compose up -d` (PostgreSQL и Redis; опционально собранный backend, см. ниже).
3. Frontend: `cd frontend && npm install && npm run dev`.
   // mvn -DskipTests package
Backend локально из корня репозитория (при запущенных Postgres и Redis из compose):

```bash
mvn spring-boot:run // mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Образ backend в `docker-compose` собирается из корня репозитория (`Dockerfile`). Перед первым запуском контейнера выполните `mvn -DskipTests package`, чтобы появился JAR в `target/`.

## Тесты

```bash
mvn verify
```

Генерируется отчёт JaCoCo (`target/site/jacoco/index.html`). На этапе `verify` выполняется проверка покрытия строк не ниже **70%** для пакетов `com.coursework.driverservice.application*` и `com.coursework.driverservice.domain*` (исключения см. в `pom.xml`).

## Swagger / OpenAPI

После запуска backend: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Структура папок (кратко)

- `src/main/java/com/coursework/driverservice/` — приложение: `application` (handlers, queries), `domain`, `infrastructure` (web, persistence, config).
- `src/main/resources/db/changelog/` — миграции Liquibase.
- `frontend/src/` — SPA: страницы, компоненты, API-клиент.

## Реализованные паттерны

- CQRS (команды / запросы), hexagonal ports & adapters.
- Спецификации JPA для фильтрации.
- Доменные события приложения + асинхронная запись аудита после коммита транзакции.
- Кэширование профиля пользователя в Redis (`userProfile`, TTL 15 минут).
- Интеграционные тесты с Testcontainers (PostgreSQL, Redis).
