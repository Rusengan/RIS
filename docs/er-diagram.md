# Схема базы данных (ER)

Ниже — обзор основных сущностей и связей. Детальная структура задаётся Liquibase в `src/main/resources/db/changelog/`.

```mermaid
erDiagram
  users ||--o{ user_roles : has
  roles ||--o{ user_roles : maps
  users ||--o{ trips : participates
  vehicles ||--o{ trips : assigned
  trips ||--|| routes : has
  routes ||--o{ route_points : contains
  users ||--o{ work_sessions : opens
  users ||--o{ audit_logs : generates
```

Для полной картины откройте файлы `0001_init_users_roles.yaml` … `0006_init_audit_logs.yaml` в каталоге changes.
