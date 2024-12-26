package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.HealthCheckMode;
import io.dyuti.dropwizard.config.HttpHealthCheckConfig;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP Health Check that performs basic CONNECT to check is the application can each the URL
 * endpoint specified in the configuration
 */
@RequiredArgsConstructor
@Slf4j
public class HttpConnectivityHealthCheck extends HealthCheck {

  private final HttpHealthCheckConfig config;
  private final AlertPublisher alertPublisher;

  private URL url;

  @Override
  protected Result check() {
    HttpURLConnection connection = null;
    try {
      if (Objects.isNull(url)) {
        url = new URI(config.getUrl()).toURL();
      }
      connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(config.getConnectTimeout());
      connection.setRequestMethod("GET");
      connection.connect();
      return Result.healthy();
    } catch (Exception e) {
      log.error("Error executing HTTP connectivity healthcheck for {}", config.getUrl(), e);
      alertPublisher.publish(config.getName(), Result.unhealthy(e));
      if (config.getMode() == HealthCheckMode.ALERT) {
        return Result.healthy();
      }
      return Result.unhealthy(e);
    } finally {
      if (Objects.nonNull(connection)) {
        connection.disconnect();
      }
    }
  }
}
