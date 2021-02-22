Quarkus Kafka Quickstart
========================

Demo Kafka producer application.

Uses MicroProfile Reactive Messaging library as the client.

## Kafka cluster

First you need a Kafka cluster. You can follow the instructions from the [Apache Kafka web site](https://kafka.apache.org/quickstart) or run `docker-compose up` if you have docker installed on your machine.

## Start the application

The application can be started using: 

```bash
mvn quarkus:dev
```

## Endpoints

`GET /records`: To see the amount of successful and failed messages processed since the last request made to this endpoint. Can be called every second to get a rough idea on performance.

`POST /records -d "num=$number" -d "payload=$data"`: Trigger sending messages in bulk to kafka where `$number` is the amount of messages you would like to send, and the `$data` is the payload you would to include in these messages. `$data` is optional. If left empty, some other random data of a relatively big size (i.e. ~18kb) will be sent.

### Example

Wathcing the processed data every second:
```bash
while sleep 1; do curl http://localhost:8080/records; done
```

Send 100,000 messages:
```bash
curl http://localhost:8080/records -X POST -d "num=100000"
```

Send 100,000 messages with user-provided data:
```bash
export MYVAR=<YOUR_DATA_HERE>
curl http://localhost:8080/records -X POST -d "num=100000" -d "payload=$MYVAR"
```