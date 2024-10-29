package io.dyuti.dropwizard.alert;

import com.codahale.metrics.health.HealthCheck.Result;

public interface AlertPublisher {
  void publish(String name, Result result);
}
