# Atajos del proyecto. "make" sin argumentos muestra la ayuda.
.DEFAULT_GOAL := ayuda
.PHONY: ayuda build test verify run up down e2e test-deploy deploy

ayuda: ## Muestra esta ayuda
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

build: ## Compila y empaqueta sin pruebas
	mvn -B -DskipTests package

test: ## Corre las pruebas unitarias y de integracion
	mvn -B test

verify: ## Pruebas + cobertura minima 90% (JaCoCo). Reporte en target/site/jacoco/index.html
	mvn -B verify

run: ## Arranca el backend en local (perfil local; requiere las variables de .env exportadas)
	mvn -B spring-boot:run -Dspring-boot.run.profiles=local

up: ## Levanta backend + MySQL con Docker (usa .env)
	docker compose up --build -d

down: ## Detiene backend + MySQL (conserva el volumen de datos)
	docker compose down

e2e: ## Pruebas end-to-end contra el backend levantado con "make up"
	./scripts/e2e.sh

test-deploy: ## Prueba el script de despliegue y su rollback con un hosting simulado
	./scripts/test/deploy_test.sh

deploy: ## Despliega (tests, imagen, deploy, health y rollback automatico)
	./deploy.sh
