# Prometheus JFR Exporter

a collector that scrapes JFR events from a JVM target at runtime for Prometheus to use

## Obtaining the Exporter

You can obtain a agent jar by downloading a prebuilt artifact or building from source.

### [Option 1] Using a Prebuilt JAR

Download the prebuilt jar from the [release page](https://github.com/Farit-Biktimirov/prometheus-jfr-exporter/releases).
```sh
$ wget https://github.com/Farit-Biktimirov/prometheus-jfr-exporter/releases/download/untagged-fb500dfa137aed0596da/prometheus-jfr-exporter-0.0.1-SNAPSHOT.jar
```

### [Option 2] Building from Source

#### Requirements

This project references libraries from JDK Misson Control project, which is not published on Maven Central. In order to build from source, gradle expects to find JMC artifacts in Maven Local. Follow [offical instructions](http://hg.openjdk.java.net/jmc/jmc/file/5e0a199762b6/README.md#l177) to build JMC.

After packaging JMC, run the following command in JMC root directory to install artifacts to Maven Local.
```sh
$ mvn install -DskipTests -Dspotbugs.skip=true
```

#### Building Instructions

- Clone this repository to local.
  ```sh
  $ git clone  https://github.com/Farit-Biktimirov/prometheus-jfr-exporter.git
  ```
- Run gradle to build a fat jar.
  ```sh
  $ cd prometheus-jfr-exporter
  $ ./mvn -DskipTests clean install
  ```
- Find the built jar in `./target` directory.
  ```sh
  $ ls ./target/prometheus-jfr-exporter*.jar
  ```

## Running a Exporter

JFR Prometheus Exporter accesses recordings using JMX connections. Before running this program, make sure you have another JVM instance running and listening for JMX connections.

### Usage

```sh
$ java -jar ./prometheus-jfr-exporter-0.0.1-SNAPSHOT.jar  --help
Usage: prometheus-jfr-exporter [options]
  Options:
    --destination
      The recording destination file. Default recording.jfr
      Default: recording.jfr
    --disk
      Write on disk boolean [true/false]
      Default: true
    --dumpOnExit
      Dump to file when exit boolean [true/false]
      Default: false
    --eventConfiguration
      The event Configuration file name.
    -h, --help
  * -l, --local
      The local host for prometheus scraping in [host:port] format
    --maxAge
      Max age of a recording. Default "0"
      Default: 0
    --maxSize
      Max size of a recording. Default "0"
      Default: 0
    --name
      The recording name. Default MyRecording
      Default: MyRecording
    --password
      password for remote connection.
  * -r, --remote
      The remote host in [host:port] format
    --username
      user name for connecting to remote host.
```

### Example
```
$ java -jar ./prometheus-jfr-exporter-0.0.1-SNAPSHOT.jar -r localhost:9091 -l 0.0.0.0:8080
```

By default, the exporter endpoint will be running on `http://0.0.0.0:8080/metrics` with [`default.jfc`](./src/main/resources/com/redhat/rhjmc/prometheus_jfr_exporter/default.jfc) event configuration.

## Testing

``// TODO``

## License

This project is licensed under the [GNU GENERAL PUBLIC LICENSE (Version 3)](./LICENSE).
