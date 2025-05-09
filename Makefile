.PHONY: build run stop clean test lint docker-build docker-up docker-down docker-logs help

APP_NAME=user-auth-service
DOCKER_COMPOSE=docker-compose

# Основные команды
help:
	@echo "Доступные команды:"
	@echo "  make build        - Сборка приложения"
	@echo "  make run         - Запуск приложения локально"
	@echo "  make stop        - Остановка приложения"
	@echo "  make clean       - Очистка скомпилированных файлов"
	@echo "  make test        - Запуск тестов"
	@echo "  make lint        - Проверка кода линтером"
	@echo "  make docker-build - Сборка Docker-образа"
	@echo "  make docker-up   - Запуск всех сервисов в Docker"
	@echo "  make docker-down - Остановка всех сервисов в Docker"
	@echo "  make docker-logs - Просмотр логов Docker-контейнеров"

# Локальная разработка
build:
	mvn clean package -DskipTests

run:
	mvn spring-boot:run

stop:
	@echo "Остановка приложения..."
	@kill $$(lsof -t -i:8080) 2>/dev/null || true

clean:
	mvn clean

test:
	mvn test

lint:
	mvn checkstyle:check

# Docker команды
docker-build:
	$(DOCKER_COMPOSE) build

docker-up:
	$(DOCKER_COMPOSE) up -d

docker-down:
	$(DOCKER_COMPOSE) down

docker-logs:
	$(DOCKER_COMPOSE) logs -f

# Дополнительные команды
docker-restart:
	$(DOCKER_COMPOSE) restart

docker-ps:
	$(DOCKER_COMPOSE) ps

docker-clean:
	$(DOCKER_COMPOSE) down -v
	docker system prune -f

# Команды для Keycloak
keycloak-setup:
	@echo "Настройка Keycloak..."
	@echo "1. Откройте http://localhost:8180"
	@echo "2. Войдите с учетными данными admin/admin"
	@echo "3. Создайте новый realm 'user-auth'"
	@echo "4. Создайте клиент 'user-auth-client'"
	@echo "5. Настройте роли 'user' и 'admin'"

# Команды для базы данных
db-migrate:
	mvn flyway:migrate

db-clean:
	mvn flyway:clean

