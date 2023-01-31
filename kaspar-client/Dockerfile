FROM python:3.9.7-slim

USER root

RUN apt-get -qq -y update && \
    apt-get -y autoclean && \
    apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir confluent-kafka

RUN useradd -m kaspar && \
    cp /root/.bashrc /home/kaspar/ && \
    mkdir /home/kaspar/data && \
    chown -R --from=root kaspar /home/kaspar

ENV HOME /home/docker
WORKDIR ${HOME}/data
COPY files/* ./

USER kaspar

CMD ["/bin/bash"]