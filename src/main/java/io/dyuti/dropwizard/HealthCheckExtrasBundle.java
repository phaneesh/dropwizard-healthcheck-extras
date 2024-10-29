/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dyuti.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.health.check.tcp.TcpHealthCheck;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.alert.LogAlertPublisher;
import io.dyuti.dropwizard.config.HealthcheckExtrasConfig;
import io.dyuti.dropwizard.healtcheck.DiskSpaceHealthCheck;
import io.dyuti.dropwizard.healtcheck.HttpConnectivityHealthCheck;
import io.dyuti.dropwizard.healtcheck.HttpsConnectivityHealthCheck;
import io.dyuti.dropwizard.healtcheck.MetricHealthCheck;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Bundle that allows initializing TCP and HTTP(s) health checks with easy configuration */
@Slf4j
public abstract class HealthCheckExtrasBundle<T extends Configuration>
    implements ConfiguredBundle<T> {

  private AlertPublisher alertPublisher;

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    alertPublisher = new LogAlertPublisher();
  }

  public abstract HealthcheckExtrasConfig getConfig(T configuration);

  public AlertPublisher getAlertPublisher() {
    return alertPublisher;
  }

  @Override
  public void run(T configuration, Environment environment) {
    var config = getConfig(configuration);
    if (Objects.nonNull(config.getTcp()) && !config.getTcp().isEmpty()) {
      log.info("Registering TCP Health Checks");
      config
          .getTcp()
          .forEach(
              tcpHealthCheckConfig -> {
                log.info("Registering TCP Health Check for: {}", tcpHealthCheckConfig);
                environment
                    .healthChecks()
                    .register(
                        tcpHealthCheckConfig.getName(),
                        new TcpHealthCheck(
                            tcpHealthCheckConfig.getHost(),
                            tcpHealthCheckConfig.getPort(),
                            Duration.of(
                                tcpHealthCheckConfig.getConnectTimeout(), ChronoUnit.MILLIS)));
              });
    }
    if (Objects.nonNull(config.getHttp()) && !config.getHttp().isEmpty()) {
      log.info("Registering HTTP Health Checks");
      config.getHttp().stream()
          .filter(c -> !c.getUrl().startsWith("https"))
          .forEach(
              httpConfig -> {
                log.info("Registering Http Health Check for: {}", httpConfig);
                environment
                    .healthChecks()
                    .register(
                        httpConfig.getName(),
                        new HttpConnectivityHealthCheck(httpConfig, getAlertPublisher()));
              });
      config.getHttp().stream()
          .filter(c -> c.getUrl().startsWith("https"))
          .forEach(
              httpConfig -> {
                log.info("Registering Https Health Check for: {}", httpConfig);
                environment
                    .healthChecks()
                    .register(
                        httpConfig.getName(),
                        new HttpsConnectivityHealthCheck(httpConfig, getAlertPublisher()));
              });
    }
    if (Objects.nonNull(config.getDisk()) && !config.getDisk().isEmpty()) {
      log.info("Registering Disk Space Health Checks");
      config
          .getDisk()
          .forEach(
              diskConfig -> {
                log.info("Registering Disk Space Health Check for: {}", diskConfig);
                environment
                    .healthChecks()
                    .register(
                        diskConfig.getName(),
                        new DiskSpaceHealthCheck(diskConfig, getAlertPublisher()));
              });
    }
    if (Objects.nonNull(config.getMetric()) && !config.getMetric().isEmpty()) {
      log.info("Registering Metric Health Checks");
      config
          .getMetric()
          .forEach(
              metricHealthCheckConfig -> {
                log.info("Registering Metric Health Check for: {}", metricHealthCheckConfig);
                environment
                    .healthChecks()
                    .register(
                        metricHealthCheckConfig.getName(),
                        new MetricHealthCheck(
                            environment, metricHealthCheckConfig, getAlertPublisher()));
              });
    }
  }
}
