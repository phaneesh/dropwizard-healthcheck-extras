package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.HealthCheckMode;
import io.dyuti.dropwizard.config.HttpHealthCheckConfig;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import lombok.RequiredArgsConstructor;

/**
 * HTTPS Health Check that performs basic CONNECT to check is the application can each the URL endpoint
 * specified in the configuration. Can also perform additional certificate validation if required.
 */
@RequiredArgsConstructor
public class HttpsConnectivityHealthCheck extends HealthCheck {

  private final HttpHealthCheckConfig config;
  private final AlertPublisher alertPublisher;

  private URL url;

  @Override
  protected Result check() {
    HttpsURLConnection connection = null;
    try {
      if (Objects.isNull(url)) {
        url = new URI(config.getUrl()).toURL();
      }
      connection = (HttpsURLConnection) url.openConnection();
      connection.setConnectTimeout(config.getConnectTimeout());
      connection.setRequestMethod("GET");
      connection.connect();
      if (config.isVerify()) {
        Certificate[] certs = connection.getServerCertificates();
        for (Certificate cert : certs) {
          ((X509Certificate) cert).checkValidity();
        }
        Result.healthy();
      }
      return Result.healthy();
    } catch (Exception e) {
      alertPublisher.publish(config.getName(), Result.unhealthy(e));
      if(config.getMode() == HealthCheckMode.ALERT) {
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
