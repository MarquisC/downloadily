

unit_tests:
	sbt test
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
	brew install localstack