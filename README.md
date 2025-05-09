# Keycloak User Service

Сервис управления пользователями на базе Keycloak с расширенной функциональностью и интеграцией с PostgreSQL.

## Содержание

- [Архитектура](#архитектура)
- [Требования](#требования)
- [Установка и запуск](#установка-и-запуск)
- [Конфигурация](#конфигурация)
- [API Endpoints](#api-endpoints)
- [Безопасность](#безопасность)
- [Мониторинг](#мониторинг)
- [Разработка](#разработка)

## Архитектура

Система состоит из следующих компонентов:
- User Service (Spring Boot приложение)
- Keycloak (сервер аутентификации и авторизации)
- PostgreSQL (база данных)

![Архитектура системы](docs/architecture.drawio.png)

## Требования

- Java 17
- Docker и Docker Compose
- Maven 3.9+
- PostgreSQL 15+
- Keycloak 24.0.1

## Установка и запуск

### Локальная разработка

1. Клонируйте репозиторий:
```bash
git clone https://github.com/your-username/keycloak-user-service.git
cd keycloak-user-service
```

2. Соберите проект:
```bash
make build
```

3. Запустите приложение:
```bash
make run
```

### Docker

1. Соберите и запустите все сервисы:
```bash
make docker-build
make docker-up
```

2. Проверьте статус контейнеров:
```bash
make docker-ps
```

3. Просмотр логов:
```bash
make docker-logs
```

## Конфигурация

### Переменные окружения

Основные настройки приложения:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_auth_db
    username: postgres
    password: postgres

keycloak:
  auth-server-url: http://localhost:8180
  realm: user-auth
  resource: user-auth-client
  credentials:
    secret: your-client-secret
```

### Настройка Keycloak

1. Запустите настройку Keycloak:
```bash
make keycloak-setup
```

2. Следуйте инструкциям в консоли для создания:
   - Realm
   - Client
   - Roles
   - Users

## API Endpoints

### Пользовательские эндпоинты

- `GET /api/v1/me` - Получение профиля текущего пользователя
- `PUT /api/v1/me` - Обновление профиля
- `DELETE /api/v1/me` - Деактивация профиля

### Административные эндпоинты

- `GET /api/v1/admin/users` - Получение списка всех пользователей
- `POST /api/v1/admin/users` - Создание нового пользователя
- `PUT /api/v1/admin/users/{userId}` - Обновление пользователя
- `DELETE /api/v1/admin/users/{userId}` - Удаление пользователя

## Безопасность

- JWT аутентификация через Keycloak
- Ролевая модель доступа (ROLE_USER, ROLE_ADMIN)
- Rate limiting для API endpoints
- Валидация входных данных
- Безопасное хранение учетных данных

## Мониторинг

Доступные эндпоинты мониторинга:

- Метрики: `http://localhost:8080/actuator/metrics`
- Health check: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Разработка

### Структура проекта

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── keycloak/
│   │           └── userservice/
│   │               ├── config/
│   │               ├── controller/
│   │               ├── dto/
│   │               ├── service/
│   │               └── util/
│   └── resources/
│       └── application.yml
└── test/
```

### Полезные команды

```bash
# Запуск тестов
make test

# Проверка кода
make lint

# Очистка проекта
make clean

# Миграции базы данных
make db-migrate
```

### Логирование

Настроено логирование через SLF4J с выводом в консоль и файл. Уровни логирования настраиваются в `application.yml`.

## Лицензия

MIT License