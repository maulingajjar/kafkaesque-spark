#!/usr/bin/env python

from confluent_kafka import Producer, Consumer, OFFSET_BEGINNING
from datetime import datetime
import time, argparse, os

def delivery_callback(err, msg):
    if err:
        print('ERROR: Message failed delivery: {}'.format(err))
    else:
        print("Produced event to topic {topic}: key = {key} value = {value}".format(
            topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))

def producer(server, topic):
    conf = {'bootstrap.servers': server,
        'client.id': 'kaspar-producer'}
    producer = Producer(conf)

    dataFile = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'kaspar.csv')
    if(not os.path.isfile(dataFile)):
        print('The file ' + dataFile + ' does not exist')
        return
    with open(dataFile, 'r') as file:
        while True:
            line = file.readline()
  
            if not line:
                break
            producer.poll(0)
            producer.produce(topic, key=datetime.now().strftime("%d-%m-%Y %H:%M:%S"), value=line.strip(), callback=delivery_callback)
            time.sleep(1)

        producer.flush()

def consumer(server, topic):
    conf = {'bootstrap.servers': server,
        'group.id': 'kasper-consumer',
        'auto.offset.reset': 'earliest'}
    consumer = Consumer(conf)
    consumer.subscribe([topic])

    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            elif msg.error():
                print("ERROR: {}".format(msg.error()))
            else:
                print("Consumed event from topic {topic}: key = {key} value = {value}".format(
                    topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()

if __name__ == '__main__':
    CLIENT_TYPE = {'producer': producer, 'consumer': consumer}

    parser = argparse.ArgumentParser(description='Client for Kafka')
    parser.add_argument('type', metavar='type', help='Type of Kafka Client (producer, consumer)', choices=CLIENT_TYPE.keys())
    parser.add_argument('-s', '--bootstrap-server', help='Kafka bootstrap server. For example: localhost:9092')
    parser.add_argument('-t', '--topic', help='Kafka topic')

    args = parser.parse_args()
    CLIENT_TYPE[args.type](args.bootstrap_server, args.topic)