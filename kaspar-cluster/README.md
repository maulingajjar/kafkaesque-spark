# kaspar-cluster
This module contains YAML files for Kafka StatefulSet and Spark Deployment on a Kubernetes cluster.

For Kafka, we will use the KRaft mode Kafka image available on [Dockerhub]( https://hub.docker.com/r/doughgle/kafka-kraft/).

For Spark, we will create a Java 11 compatible Docker image based on the one available on [Dockerhub](https://hub.docker.com/r/bitnami/spark).

## Spark Docker Image
Create a Docker image for the Spark using the following command from the location of Dockerfile:
```
docker build -t kaspar-spark:latest .
```