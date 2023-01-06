

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