FROM bitnami/spark:3.3.1

USER 0

RUN apt update && \
    apt install -y openjdk-11-jre-headless && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/apt/archives/* && \
    rm -rf /opt/bitnami/java

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/ 
ENV PATH=/opt/bitnami/python/bin:/usr/lib/jvm/java-11-openjdk-amd64:/opt/bitnami/spark/bin:/opt/bitnami/spark/sbin:/opt/bitnami/common/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

USER 1001
