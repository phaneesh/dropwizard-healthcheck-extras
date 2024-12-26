# Dropwizard Healthcheck Extras Bundle

This bundle adds additional healthchecks which can used to ensure that the application and perform 
healthchecks with endpoint resources like proxies, external endpoints from a connectivity standpoint.
It can also be used to monitor application metrics and disk space based health.

## Supported healthchecks
- TCP
- HTTP
- HTTPS with certificate verification support
- Disk space healthcheck
- Metric healthcheck (based on any metric that is emitted)
- Cluster Reachability healthcheck
 
## Usage
This bundle makes it simple to add healthchecks to monitor external endpoint resources that your application depends on. 
 
### Build instructions
  - Clone the source:

        git clone github.com/phaneesh/dropwizard-healthcheck-extras

  - Build

        mvn install

### Maven Dependency
* Use the following maven dependency:
```
<dependency>
    <groupId>io.dyuti</groupId>
    <artifactId>dropwizard-healthcheck-extras</artifactId>
    <version>3.0.11-1</version>
</dependency>
```

### Using Health Check Extras bundle

#### Bootstrap
```java
    @Override
    public void initialize(final Bootstrap bootstrap) {
        bootstrap.addBundle(new HealthCheckExtrasBundle<Configuration>() {

          public HealthcheckExtrasConfig getConfig(Configuration appConfig) {
            return appConfig.getHealthcheckExtrasConfig();
          }
        });
    }
```

#### Configuration
```yaml
extraHealthChecks:
  tcp:
    - name: "remote-endpoint"
      host: "192.167.172.76"
      port: 3500
      timeout: 2000 #2 seconds
      mode: NORMAL
  http:
    - name: "external-http"
      url: "http://www.somewhere.com"
      timeout: 3000 #3 seconds
    - name: "external-https"
      url: "https://www.somewhere.com"
      timeout: 1000 #1 second (default)
      verifyCertificate: true #Verify the certificate. If the server certificate is not valid, the healthcheck will fail
      mode: NORMAL
  disk:
    - name: "log-volume-space"
      path: "/var/log"
      threshold: 204800 #200 MB
  metric:
    - name: "metric-log-error-rate"
      metric: "ch.qos.logback.core.Appender.error"
      dimension: "m1_rate" #Check 1-minute rate
      type: METER
      threshold: 1000 #If the error rate is more than 1000 in 1-minute application will become unhealthy
      mode: ALERT
    - name: "metric-get-latency"
      metric: "io.dropwizard.jetty.MutableServletContextHandler.get-requests"
      dimension: "p75" #Check p75 latency
      type: TIMER
      threshold: 2 #if p. 75 latency is more than 2-second application will become unhealthy
      mode: ALERT
    - name: "critical-errors"
      metric: "custom.critical.error"
      dimension: "m5_rate" #Check 5-minute rate
      type: METER
      threshold: 2000 #if the error rate is higher than 2000 in 5-minute application will become unhealthy
  cluster:
    - name: "my-remote-cluster"
      hostNameMode: PATTERN #The Host name is specified as a pattern which conforms to prefix[range].suffix format. HOSTS mode can be used to specify a list of hosts. Default is PATTERN
      hostNamePattern: my-host-prefix[001-100].mydomain.com #Check if the cluster host is reachable. In RANDOM mode, a random host will be selected from the pattern.
      portRange: 32000-50000  #Check if the cluster host is reachable on the given port range (A random port will be selected from the range)
      selectionMode: RANDOM #Select a random host from the pattern. SEQUENTIAL will check each host in the pattern sequentially. Port selection will still be random
      connectTimeout: 500
      checkInterval: 86400000 #Run the check every 12 hours. Default is 24 hours and the minimum supported interval is 1 minute. Avoid giving aggressive intervals in SEQUENTIAL mode for large clusters as it may impact network traffic and might stall helthchecks.
      mode: ALERT
    - name: "my-remote-cluster-list"
      hostNameMode: LIST #Host name is specified as a list of hosts. Default is PATTERN
      hosts: 
        - "my-host-001.mydomain.com"
        - "my-host-002.mydomain.com"
        - "my-host-003.mydomain.com"
      portRange: 32000-50000  #Check if the cluster host is reachable on the given port range (A random port will be selected from the range)
      selectionMode: SEQUENTIAL #SEQUENTIAL will check each host in the pattern sequentially. Port selection will still be random
      connectTimeout: 500
      checkInterval: 86400000 #Run the check every 12 hours. Default is 24 hours and the minimum supported interval is 1 minute. Avoid giving aggressive intervals in SEQUENTIAL mode for large clusters as it may impact network traffic and might stall helthchecks.
      mode: NORMAL
```

### Metric Types Supported
- COUNTER
- GAUGE
- METER
- TIMER
- HISTOGRAM

### Dimension Supported (Please use relevant dimension based on the metric type) 
| Dimension | COUNTER | GUAGE | METER | TIMER | HISTOGRAM |
|-----------|---------|-------|-------|-------|-----------|
| count     | No      | No    | Yes   | No    | No        |
| max       | No      | No    | No    | Yes   | Yes       |
| min       | No      | No    | No    | Yes   | Yes       |
| mean      | No      | No    | No    | Yes   | Yes       |
| stddev    | No      | No    | No    | Yes   | Yes       |
| median    | No      | No    | No    | Yes   | Yes       |
| p75       | No      | No    | No    | Yes   | Yes       |
| p95       | No      | No    | No    | Yes   | Yes       |
| p98       | No      | No    | No    | Yes   | Yes       |
| p99       | No      | No    | No    | Yes   | Yes       |
| p999      | No      | No    | No    | Yes   | Yes       |
| m1_rate   | No      | No    | Yes   | No    | No        |
| m5_rate   | No      | No    | Yes   | No    | No        |
| m15_rate  | No      | No    | Yes   | No    | No        |
| mean_rate | No      | No    | Yes   | No    | No        |

### Alert Publisher
The bundle also supports alerting the healthcheck status to a publisher. 
The publisher can be any class that implements the `AlertPublisher` interface. 
The publisher will be called whenever the healthcheck result is unhealthy and healthcheck mode is set to ALERT in config. 
The publisher can be configured when initializing the bundle as shown below:

* Custom AlertPublisher
```java
@Override
public void initialize(final Bootstrap bootstrap) {
    bootstrap.addBundle(new HealthCheckExtrasBundle<Configuration>() {

      public HealthcheckExtrasConfig getConfig(Configuration appConfig) {
        return appConfig.getHealthcheckExtrasConfig();
      }
      
      //Custom AlertPublisher
      public AlertPublisher getAlertPublisher() {
        return new AlertPublisher() {
            @Override
            public void publish(String name, HealthCheck.Result result) {
                //Publish the alert to a monitoring system
            }
        };
      }
    });
}
```
* MetricsAlertPublisher
```java
@Override
public void initialize(final Bootstrap bootstrap) {
  bootstrap.addBundle(new HealthCheckExtrasBundle<Configuration>() {

    public HealthcheckExtrasConfig getConfig(Configuration appConfig) {
      return appConfig.getHealthcheckExtrasConfig();
    }

    //MetricsAlertPublisher
    public AlertPublisher getAlertPublisher() {
      return new MetricsAlertPublisher(metricRegistry);
    }
  });
}
```
* LogAlertPublisher

```java
import io.dyuti.dropwizard.alert.LogAlertPublisher;

@Override
public void initialize(final Bootstrap bootstrap) {
  bootstrap.addBundle(new HealthCheckExtrasBundle<Configuration>() {

    public HealthcheckExtrasConfig getConfig(Configuration appConfig) {
      return appConfig.getHealthcheckExtrasConfig();
    }

    //LogAlertPublisher
    public AlertPublisher getAlertPublisher() {
      return new LogAlertPublisher();
    }
  });
}
```
Default AlertPublisher is a `LogAlertPublisher` which logs the alert to the application logs. 
A `MetricAlertPublisher` is also available which can be used to publish the alert to a counter with 
the name in the following format:
```
<prefix>.<healthcheck-name>.healthcheck.alerts
```

### Cluster Healthcheck Dynamic Source

```java
import java.net.InetSocketAddress;

@Override
public void initialize(final Bootstrap bootstrap) {
  bootstrap.addBundle(new HealthCheckExtrasBundle<Configuration>() {

    public HealthcheckExtrasConfig getConfig(Configuration appConfig) {
      return appConfig.getHealthcheckExtrasConfig();
    }

    //MetricsAlertPublisher
    public AlertPublisher getAlertPublisher() {
      return new MetricsAlertPublisher(metricRegistry);
    }

    public Map<String, Supplier<List<InetSocketAddress>>> getHostSource() {
      Map<String, Supplier<List<InetSocketAddress>>> hostSource = new HashMap<>();
      hostSource.put("my-remote-cluster", () -> {
        List<InetSocketAddress> hosts = new ArrayList<>();
        //Add the hosts to the list
        return hosts;
      });
      return hostSource;
    }
  });
}
```