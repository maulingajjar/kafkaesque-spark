# kaspar-client
This module contains a simple Kafka client that can be used in a standalone mode or can be packaged up into a Docker image which can run on a Kubernetes cluster. The Kafka client can be used in Producer mode or Consumer mode.

In the producer mode, the client will iterate over the contents of `kaspar-client/files/kaspar.csv` and produce one message per row per second.

## Usage
```
usage: kafka-client.py [-h] [-s BOOTSTRAP_SERVER] [-t TOPIC] mode

Client for Kafka

positional arguments:
  mode                  Mode of Kafka Client (producer, consumer)

optional arguments:
  -h, --help            show this help message and exit
  -s BOOTSTRAP_SERVER, --bootstrap-server BOOTSTRAP_SERVER
                        Kafka bootstrap server. For example: localhost:9092
  -t TOPIC, --topic TOPIC
                        Kafka topic
```

## Docker Image
Create a Docker image for the client using the following command from the location of Dockerfile:
```
docker build -t kaspar-client:latest .
```