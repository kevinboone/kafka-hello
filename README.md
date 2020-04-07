# kafka-hello


## What is this?

`kafka-hello` is trivial Java Kafka client that uses "SSL" (TLS) transport and
user/password authentication.

This is the simplest self-contained Java example I could come up with, that
sends and/or receives a single message from a Kafka topic, wth (optionally)
SSL (TLS) transport and (optionally) user/password credentials.  

I wrote this sample specifically to demonstrate how to use a Java client
with Strimzi (or Red Hat AMQ Streams), which is Kafka on Kubernetes. It 
assumes that Kafka is accessible via OpenShift routes, which 
require the use of TLS -- the HAProxy router cannot route Kafka
protocol in plaintext. For better or worse, TLS is required even if there
are no specific data security constraints. Strimzi only supports SCRAM-512
over SASL as the user/password protocol, although it supports more 
sophisticated methods than basic user/password. Kafka supports other 
basic user/password mechanisms, but this example does not support them.

This example does not support client certificate authentication. 

## Building

    $ mvn package

This generates a JAR file in `target/' which is completely self-contained
(apart from the JVM, of course).

## Usage

    $ java -jar target/kafka-hello-x.x.x-jar-with-dependencies {options}

SSL mode is indicated by using the `--truststore` command-line option, which
must reference an existing Java keystore, in whatever format the JVM supports.
If `--truststore` is given, the `--truststore.password` argument must also be
given. 

If `--user` is specified, the program uses SCRAM-512 via JAAS. If
`--user` is given, `--password` will almost certainly be required.

If `--topic` is not given, the default is `my-topic`.

To run in receive mode, with authentication and SSL:

    $ java -jar target/kafka-hello-0.0.1-jar-with-dependencies.jar \
        --bootstrap [bootstrap_URI] --truststore [file].jks \
	--truststore.password=changeit \
	--user [user] \
	--password [password] \
	-r

Send mode has the same command line, except `-s` rather that `-r`.

## Logging

Modify the `log4j` configuration in `src/main/resources` to change 
the logging levels.

## Openshift configuration

When running Kafka on OpenShift (Strimzi or Red Hat AMQ Streams), you
can extract the server certificate and import it into a Java keystore
for this program like this:

    $ oc extract secret/[cluster-name]-cluster-ca-cert --keys=ca.crt \ 
       --to=- > ca.crt
    $ keytool -keystore foo.jks -alias foo -import -file ca.crt

To be able to access Kafka on OpenShift from an external Java (or any)
client, Strimzi needs to be set up to expose routes for the brokers.
This is usually done through the `kafka` custom resource:

    metadata:
      name: my-cluster
    spec:
      kafka:
      version: 2.4.0
      replicas: 3
      listeners:
        plain: {}
        tls: {}
        external:
          type: route
        ...

To use user-password authentication on the external listener, you will
also need:

      listeners:
        external:
          type: route
          authentication:
            type: scram-sha-512

To create a user with a password, create a YAML file like this, and the
`oc apply` it:

    apiVersion: kafka.strimzi.io/v1beta1
    kind: KafkaUser
    metadata:
      name: foo
      labels:
	strimzi.io/cluster: my-cluster
    spec:
      authentication:
	type: scram-sha-512
      authorization:
	type: simple
	acls:
	  - resource:
	      type: topic
	      name: my-topic
	      patternType: literal
	    operation: Read
	  - resource:
	      type: topic
	      name: my-topic
	      patternType: literal
	    operation: Describe
	  - resource:
	      type: group
	      name: my-group
	      patternType: literal
	    operation: Read

This process creates a Kubernetes secret, whose value is the plaintext
password, which can be used as the `--password` argument to this sample. To
extract the plaintext password for the user `foo`:

    $ oc get secret foo --template "{{.data.password}}"|base64 -d


