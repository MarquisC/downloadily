# Root Makefile

all_test:
	sbt test

standalone_tests:
	sbt "test:testOnly *standalone*"

integration_tests: start_localstack
	(cd ./infra && ./create_s3.sh)
	sbt "test:testOnly *integration*"
	localstack stop

compile:
	sbt compile
run:
	sbt run

post:
	curl -X POST localhost:7070/download \
	   -H 'Content-Type: application/json' \
	   -d "{\"url\":\"https://raw.githubusercontent.com/zio/zio-http/main/zio-http-example/src/main/scala/example/FileStreaming.scala\"}"

install_localstack_mac:
	# Reference: https://docs.localstack.cloud/getting-started/installation/#installation
	pip install localstack
	pip install awscli-local

start_localstack:
	localstack start -d
	localstack wait

stop_localstack:
	localstack stop