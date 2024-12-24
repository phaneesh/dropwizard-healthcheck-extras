package io.dyuti.dropwizard.alert;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck.Result;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsAlertPublisher implements AlertPublisher {

  private final MetricRegistry metricRegistry;

  private final Map<String, Counter> alertCounters;

  public MetricsAlertPublisher(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
    alertCounters = new ConcurrentHashMap<>();
  }

  public void publish(String name, Result result) {
    alertCounters
        .computeIfAbsent(name, k -> metricRegistry.counter(k + ".healthcheck.alerts"))
        .inc();
  }
}
