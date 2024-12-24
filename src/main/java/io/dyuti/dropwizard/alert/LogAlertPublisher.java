package io.dyuti.dropwizard.alert;

import com.codahale.metrics.health.HealthCheck.Result;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAlertPublisher implements AlertPublisher {
  @Override
  public void publish(String name, Result result) {
    log.error("Health check {} result: {}", name, result);
  }
}
