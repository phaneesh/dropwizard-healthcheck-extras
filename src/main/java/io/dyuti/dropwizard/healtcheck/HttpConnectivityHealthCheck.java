package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.config.HttpHealthCheckConfig;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
/**
 * HTTP Health Check that performs basic CONNECT to check is the application can each the URL endpoint
 * specified in the configuration
 */
public class HttpConnectivityHealthCheck extends HealthCheck {

  private final HttpHealthCheckConfig config;

  public HttpConnectivityHealthCheck(HttpHealthCheckConfig config) {
    this.config = config;
  }

  private URL url;

  @Override
  protected Result check() {
    HttpURLConnection connection = null;
    try {
      if (Objects.isNull(url)) {
        url = new URI(config.getUrl()).toURL();
      }
      connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(config.getConnectTimeout()));
      connection.setRequestMethod("CONNECT");
      connection.connect();
      return Result.healthy();
    } catch( Exception e) {
      return Result.unhealthy(e);
    } finally {
      if( Objects.nonNull(connection)) {
        connection.disconnect();
      }
    }
  }
}
