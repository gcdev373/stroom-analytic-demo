# SimpleKafkaProducer
Stroom v7.0 introduced a new pipeline element `SimpleKafkaProducer`, which is used within this demonstrator, as described below. 

## Design
### KafkaConfig
The `SimpleKafkaProducer` requires a `KafkaConfig` document, which is a Java `properties` format document, 
that contains all the connection details that are expected/required for a Kafka Producer.

```properties
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=org.apache.kafka.common.serialization.StringSerializer
client.id=stroom
bootstrap.servers=localhost:9092
acks=all
buffer.memory=100000
compression.type=none
retries=0
```

It is likely that this might be simplified in the future, e.g. by providing sensible defaults for
`key.serializer`,`value.serializer`, and `client.id`.

### KafkaRecords XML
This repo contains the schema for `kafka-records`, an XML format that defines a structure suitable for representing
a Kafka message (including topic, key, body, partition, headers, etc.). 

For further information, refer to documentation within the schema (XSD) itself, e.g. by exploring within Stroom UI.

### SimpleKafkaProducer
This is a prototype, but it is likely that similar functionality will emerge within standard Stroom on or after `v7.0`.

A pipeline element, it expects to receive XML documents that conform to `kafka-records` schema.  It is a Kafka Producer
and it creates a single message for each `kafkaRecord` that it processes.

An example pipeline that uses `SimpleKafkaProducer` is  `Sample Topic`, which includes an XSLT that converts `events-logging` XML into
`kafka-records` XML, the message body being a JSON representation of the `Event` itself.  This is a powerful, flexible approach
that avoids creating Kafka topics with differing formats of data within message bodies, that can lead to more tightly coupled, brittle, systems design.