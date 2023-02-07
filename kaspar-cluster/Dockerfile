FROM bitnami/spark:3.3.1

USER 0

RUN apt-get update && \
    apt-get install -y openjdk-11-jre-headless curl && \
    rm -rf /opt/bitnami/java && \
    rm -rf /opt/bitnami/spark

ARG JAVA_EXTRA_SECURITY_DIR="/bitnami/java/extra-security"
ARG SPARK=spark-3.3.1-bin-hadoop3-scala2.13

RUN curl -SsLf "https://dlcdn.apache.org/spark/spark-3.3.1/${SPARK}.tgz" -O && \
    tar -zxf "${SPARK}.tgz" -C /opt/bitnami/spark --strip-components=1 --no-same-owner

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/ 
ENV PATH=/opt/bitnami/python/bin:/usr/lib/jvm/java-11-openjdk-amd64:/opt/bitnami/spark/bin:/opt/bitnami/spark/sbin:/opt/bitnami/common/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

RUN apt-get autoremove --purge -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists && \
    rm -rf /var/cache/apt/archives

RUN /opt/bitnami/scripts/spark/postunpack.sh
RUN /opt/bitnami/scripts/java/postunpack.sh

USER 1001
