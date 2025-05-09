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
- [Надежность и отказоустойчивость](#надежность-и-отказоустойчивость)

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

Приложение автоматически валидирует наличие всех необходимых переменных окружения при запуске и выдает подробные сообщения об ошибках в случае их отсутствия.

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

### Аутентификация и регистрация

- `POST /api/v1/auth/register` - Регистрация нового пользователя
  ```json
  {
    "username": "newuser",
    "email": "user@example.com",
    "password": "password123",
    "firstName": "Имя",
    "lastName": "Фамилия",
    "phoneNumber": "+79001234567"
  }
  ```

- `POST /api/v1/auth/login` - Авторизация пользователя
  ```json
  {
    "username": "username",
    "password": "password"
  }
  ```

- `POST /api/v1/auth/refresh` - Обновление токена
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

Все эндпоинты аутентификации возвращают следующий формат ответа:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "bearer",
  "expiresIn": 300,
  "refreshExpiresIn": 1800,
  "userId": "f1234567-89ab-cdef-0123-456789abcdef",
  "roles": ["user"]
}
```

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

## Надежность и отказоустойчивость

### Graceful Shutdown

Приложение поддерживает graceful shutdown для корректного завершения работы:

- При получении сигнала завершения приложение перестает принимать новые запросы
- Обрабатывает все текущие запросы до завершения (с таймаутом 30 секунд)
- Корректно закрывает все соединения и ресурсы

Настройки в `application.yml`:
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Валидация переменных окружения

При запуске приложение проверяет наличие всех необходимых переменных окружения:
- Автоматическая проверка конфигурации при запуске
- Детальные сообщения об ошибках при отсутствии обязательных переменных
- Быстрая диагностика проблем конфигурации

## Лицензия

MIT License