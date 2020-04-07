/*===========================================================================

  kafka-hello
  App.java

  Copyright (c)2020 Kevin Boone, distributed under the terms of the 
  GNU Public Licence v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.kafka_hello;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;

import java.util.Properties; 
import java.util.Collections; 
import java.time.Duration;

public class App
  {
  // Default Kafka topic
  static final String DEF_TOPIC = "my-topic";

  // How long the consumer will wait for new messages, if none are
  //   avalable
  static final int CONSUMER_WAIT = 10; // Seconds

  // The consumer group ID
  static final String GROUP_ID = "FooConsumer"; 

  public static void main (String args[]) throws Exception
    {
    // Parse command line
    Options options = new Options();
    options.addOption ("p", "password", true, "password");
    options.addOption ("s", "send", false, "send message");
    options.addOption ("r", "receive", false, "receive message");
    options.addOption ("b", "bootstrap", true, "bootstrap server URI");
    options.addOption ("t", "topic", true, "topic");
    options.addOption ("u", "user", true, "user");
    options.addOption (null, "truststore", true, "trust store filename");
    options.addOption (null, "truststore.password", true, 
       "trust store password");

    CommandLineParser parser = new GnuParser();
    CommandLine cl = parser.parse (options, args);

    boolean send = false; boolean receive = false;
    if (cl.hasOption ("receive"))
      receive = true;
    if (cl.hasOption ("send"))
      send = true;

    String user = cl.getOptionValue ("u"); 
    String password = cl.getOptionValue ("p"); 

    if (!send && !receive)
      {
      throw new Exception 
	  ("One of 'send' or 'receive' must be specified");
      }

    String bootstrap = cl.getOptionValue ("b");
    if (bootstrap == null)
      throw new Exception ("No bootstrap server supplied");

    String topic = cl.getOptionValue ("t");
    if (topic == null)
      topic = DEF_TOPIC;

    boolean ssl = false;
    String truststore = cl.getOptionValue ("truststore");
    if (truststore != null) ssl = true;
    String truststorePassword = cl.getOptionValue ("truststore.password");
    if (ssl)
      {
      if (truststorePassword == null)
        throw new Exception 
	  ("In SSL mode, truststore.password must be specified");
      }

    // Call doProduce or doConsume, according to which of --send or
    //   --receive was specified

    if (send)
      doProduce (bootstrap, topic, ssl, truststore, truststorePassword, 
        user, password);

    if (receive)
      doConsume (bootstrap, topic, ssl, truststore, truststorePassword,
        user, password);
    }

/*===========================================================================

  doConsume

  Create a KafkaConsumer and read one message

===========================================================================*/

  static void doConsume (String bootstrap, String topic,
        boolean ssl, String truststore, String truststorePassword,
	String user, String password)
	throws Exception
    {
    Properties consumerProps = new Properties();
    consumerProps.put (ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
      bootstrap);
    consumerProps.put (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
      "org.apache.kafka.common.serialization.IntegerDeserializer");
    consumerProps.put (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
      "org.apache.kafka.common.serialization.StringDeserializer");
    consumerProps.put (ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);

    consumerProps.put ("security.protocol", "SSL");
    
    consumerProps.put ("ssl.truststore.location",
      truststore);
    consumerProps.put("ssl.truststore.password", truststorePassword);

    if (user != null)
      {
      consumerProps.put ("security.protocol", "SASL_SSL");
      consumerProps.put ("sasl.mechanism", "SCRAM-SHA-512");
      consumerProps.put ("sasl.jaas.config", 
         "org.apache.kafka.common.security.scram.ScramLoginModule required username=" 
	  + user + " password=" + password + ";");
      }

    KafkaConsumer<Integer, String> consumer = 
      new KafkaConsumer<> (consumerProps);

    consumer.subscribe (Collections.singletonList (topic));
    ConsumerRecords<Integer, String> records = consumer.poll
      (Duration.ofSeconds (CONSUMER_WAIT)); 
    for (ConsumerRecord<Integer, String> record : records) 
      {
      System.out.println("Received message: (" 
            + record.key() + ", " 
	    + record.value() + ") at offset " 
	    + record.offset());
      }
    consumer.close();
    }

/*===========================================================================

  doProduce

  Create a KafkaProducer and send one message

===========================================================================*/
  static void doProduce (String bootstrap, String topic,
        boolean ssl, String truststore, String truststorePassword,
	String user, String password)
	throws Exception
    {
    Properties producerProps = new Properties();
    producerProps.put (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    producerProps.put (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
	IntegerSerializer.class.getName());
    producerProps.put (ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
	StringSerializer.class.getName());

    if (ssl)
      {
      producerProps.put ("security.protocol", "SSL");
      producerProps.put ("ssl.truststore.location",
        truststore);
      producerProps.put("ssl.truststore.password", truststorePassword);
      }

    if (user != null)
      {
      producerProps.put ("security.protocol", "SASL_SSL");
      producerProps.put ("sasl.mechanism", "SCRAM-SHA-512");
      producerProps.put ("sasl.jaas.config", 
         "org.apache.kafka.common.security.scram.ScramLoginModule required username=" 
	  + user + " password=" + password + ";");
      }

    KafkaProducer<Integer, String> producer = 
	new KafkaProducer<> (producerProps);
    String message = "Hello, World";
    producer.send (new ProducerRecord<> (topic, 42, message)).get();
    producer.close();
    }
  }


