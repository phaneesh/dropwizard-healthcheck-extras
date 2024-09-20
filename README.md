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
    <version>2.1.12-1</version>
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
  http:
    - name: "external-http"
      url: "http://www.somewhere.com"
      timeout: 3000 #3 seconds
    - name: "external-https"
      url: "https://www.somewhere.com"
      timeout: 1000 #1 second (default)
      verifyCertificate: true #Verify certificate. If the server certificate is not valid, the healthcheck will fail
  disk:
    - name: "log-volume-space"
      path: "/var/log"
      threshold: 204800 #200 MB
  metric:
    - name: "metric-log-error-rate"
      metric: "ch.qos.logback.core.Appender.error"
      dimension: "m1_rate" #Check 1 minute rate
      type: METER
      threshold: 1000 #If error rate is more than 1000 in 1 minute application will become unhealthy
    - name: "metric-get-latency"
      metric: "io.dropwizard.jetty.MutableServletContextHandler.get-requests"
      dimension: "p75" #Check p75 latency
      type: TIMER
      threshold: 2 #if p75 latency is more than 2 seconds application will become unhealthy
    - name: "critical-errors"
      metric: "custom.critical.error"
      dimension: "m5_rate" #Check 5 minute rate
      type: METER
      threshold: 2000 #if the error rate is more than 2000 in 5 minutes application will become unhealthy
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
