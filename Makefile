APP_NAME=app
DB_NAME=db
TESTDB_NAME=testdb

run: db testdb build-app
	docker-compose up ${APP_NAME}

daemon: db testdb build-app
	docker-compose up -d ${APP_NAME}

test: testdb
	mvn test

db:
	docker-compose up -d ${DB_NAME}

testdb:
	docker-compose up -d ${TESTDB_NAME}

build-app:
	mvn spring-boot:build-image

stop: 
	docker-compose down

.PHONY: run daemon test db testdb build-app stop
