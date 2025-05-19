package io.dyuti.dropwizard.healtcheck;

import static java.util.Objects.nonNull;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.HealthCheckMode;
import io.dyuti.dropwizard.config.HttpHealthCheckConfig;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTPS Health Check that performs basic CONNECT to check is the application can each the URL
 * endpoint specified in the configuration. Can also perform additional certificate validation if
 * required.
 */
@RequiredArgsConstructor
@Slf4j
public class HttpsConnectivityHealthCheck extends HealthCheck {

  private final HttpHealthCheckConfig config;
  private final AlertPublisher alertPublisher;

  private final TrustManager[] trustAllCerts = new TrustManager[]{
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
          log.info("Client trusted: {}", authType);
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
          log.info("Server trusted: {}", authType);
        }
      }
  };

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
      connection.setReadTimeout(config.getReadTimeout());
      if (nonNull(config.getTlsVersion())) {
        var sslContext = SSLContext.getInstance(config.getTlsVersion());
        sslContext.init(null, trustAllCerts, new SecureRandom());
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
      }
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
      log.error("Error executing HTTPS connectivity healthcheck for {}", config.getUrl(), e);
      alertPublisher.publish(config.getName(), Result.unhealthy(e));
      if (config.getMode() == HealthCheckMode.ALERT) {
        return Result.healthy();
      }
      return Result.unhealthy(e);
    } finally {
      if (nonNull(connection)) {
        connection.disconnect();
      }
    }
  }
}
