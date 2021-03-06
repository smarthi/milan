## Milan

Milan is a data-oriented programming language and runtime infrastructure.

The Milan language is a DSL embedded in Scala. The output is an intermediate language that can be compiled to run on different target platforms. Currently there exists a single compiler that produces Flink applications.

The Milan runtime infrastructure compiles and runs Milan applications on a Flink cluster.

### Milan Language
The Milan language is similar in look and feel to other JVM-based streaming frameworks like Spark Streaming, Flink, or Kafka Streams. The main differences are that Milan uses higher-level constructs to build streaming applications, and that Milan applications are not tied to a specific runtime infrastructure.

Examples of the Milan language are available in [the samples](milan/milan-samples).

Some of the language features are described in [the docs](doc).

### Runtime
Currently Milan has a single compiler and runtime, based on [Apache Flink](https://flink.apache.org).

There are three options for executing a Milan program:
1. In-process using Flink's mini-cluster. It is not necessary to have a Flink cluster running to use this mode, only that Flink libraries are present.
1. On a Flink cluster on the local machine.
1. Remotely on a Flink cluster that has the Milan Flink control plane running.

Examples of executing using these different modes are available in [the samples](milan/milan-samples).

Instructions for using the Flink runtime are available in [the docs](doc/Milan%20Flink%20Controller.md).

### Environment Setup
Milan depends on the Flink Kinesis connector, which is not currently available on Maven central. You will need to build it yourself and install it into your local maven repo. The latest documentation on using the connector is available at https://ci.apache.org/projects/flink/flink-docs-stable/dev/connectors/kinesis.html .

1. Clone the Flink repository from https://github.com/apache/flink.
1. Checkout the version of Flink used by Milan: 'git checkout release-1.7'
1. Build and install the snapshots for this release: ' mvn clean install -Punsafe-mapr-repo -Pinclude-kinesis -DskipTests'
1. Any time the Flink version that Milan uses is changed you will need to repeat this process using the appropriate Flink release branch.

### Building
Please complete the steps in Environment Setup above before building for the first time.

`mvn clean package` will compile, run tests, and create jars of the Milan packages.

`mvn clean install` will compile, run tests, package, and install the snapshot version into your local maven repository. You can then use these from other projects using the following in your pom.xml:
```
<dependency>
    <groupId>com.amazon.milan</groupId>
    <artifactId>milan-lang</artifactId>
    <version>0.8-SNAPSHOT</version>
</dependency>

```


## License

This project is licensed under the Apache-2.0 License.
