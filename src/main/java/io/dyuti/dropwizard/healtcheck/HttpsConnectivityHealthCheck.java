package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.config.HttpHealthCheckConfig;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;

/**
 * HTTPS Health Check that performs basic CONNECT to check is the application can each the URL endpoint
 * specified in the configuration. Can also perform additional certificate validation if required.
 */
public class HttpsConnectivityHealthCheck extends HealthCheck {

  private final HttpHealthCheckConfig config;

  public HttpsConnectivityHealthCheck(HttpHealthCheckConfig config) {
    this.config = config;
  }

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
      return Result.unhealthy(e);
    } finally {
      if (Objects.nonNull(connection)) {
        connection.disconnect();
      }
    }
  }
}
