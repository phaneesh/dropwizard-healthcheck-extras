package io.dyuti.dropwizard.healtcheck;

import static java.util.Objects.isNull;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.ClusterReachabilityHealthCheckConfig;
import io.dyuti.dropwizard.config.ClusterReachabilityHealthCheckConfig.HostListSource;
import io.dyuti.dropwizard.config.ClusterReachabilityHealthCheckConfig.HostNameMode;
import io.dyuti.dropwizard.config.ClusterReachabilityHealthCheckConfig.SelectionMode;
import io.dyuti.dropwizard.config.HealthCheckMode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class ClusterReachabilityHealthCheck extends HealthCheck {

  private final ClusterReachabilityHealthCheckConfig config;
  private final AlertPublisher alertPublisher;
  private final Supplier<List<InetSocketAddress>> hostSource;
  private long lastUpdatedTime;
  private Result lastUpdatedResult;

  private final Pattern hostPattern = Pattern.compile(
      "(\\[a-zA-z\\-\\]+)\\[(\\d+)-(\\d+)\\]\\.(\\[a-zA-z\\-\\]+)\\.(\\[a-zA-z\\-\\]+)");

  public ClusterReachabilityHealthCheck(ClusterReachabilityHealthCheckConfig config,
      AlertPublisher alertPublisher) {
    this(config, alertPublisher, null);
  }

  @Override
  protected Result check() {
    if (lastUpdatedTime == 0) {
      lastUpdatedTime = System.currentTimeMillis();
    }
    if (System.currentTimeMillis() - lastUpdatedTime > config.getCheckInterval() || isNull(
        lastUpdatedResult)) {
      lastUpdatedTime = System.currentTimeMillis();
      lastUpdatedResult = checkClusterReachability();
    }
    return lastUpdatedResult;
  }

  @SuppressWarnings("java:S3776")
  private Result checkClusterReachability() {
    var portRange = config.getPortRange().split("-");
    int startPort = Integer.parseInt(portRange[0]);
    int endPort = Integer.parseInt(portRange[1]);
    if(config.getHostListSource() == HostListSource.DYNAMIC) {
      if(config.getHostNameMode() == HostNameMode.LIST && config.getSelectionMode() == SelectionMode.RANDOM) {
        return checkHostListRandom(hostSource.get());
      }
      if(config.getHostNameMode() == HostNameMode.LIST && config.getSelectionMode() == SelectionMode.SEQUENTIAL) {
        return checkHostListSequential(hostSource.get());
      }
    } else {
      if(config.getHostNameMode() == HostNameMode.LIST && config.getSelectionMode() == SelectionMode.RANDOM) {
        return checkHostListRandom(startPort, endPort);
      }
      if(config.getHostNameMode() == HostNameMode.LIST && config.getSelectionMode() == SelectionMode.SEQUENTIAL) {
        return checkHostListSequential(startPort, endPort);
      }
      if(config.getHostNameMode() == HostNameMode.PATTERN) {
        var hostMatcher = hostPattern.matcher(config.getHostNamePattern());
        int startHost = Integer.parseInt(hostMatcher.group(2));
        int endHost = Integer.parseInt(hostMatcher.group(3));
        if(config.getSelectionMode() == SelectionMode.RANDOM) {
          return checkHostPatternRandom(hostMatcher, startHost, endHost, startPort, endPort);
        } else if (config.getSelectionMode() == SelectionMode.SEQUENTIAL) {
          return checkHostPatternSequential(hostMatcher, startHost, endHost, startPort, endPort);
        }
      }
    }
    return Result.healthy();
  }

  private Result checkHostListSequential(int startPort, int endPort) {
    for (var host : config.getHosts()) {
      int selectedPort = ThreadLocalRandom.current().nextInt(startPort, endPort);
      var result = executeHealthCheck(host, selectedPort);
      if (!result.isHealthy()) {
        return result;
      }
    }
    return Result.healthy();
  }

  private Result checkHostListSequential(List<InetSocketAddress> hosts) {
    for (var host : hosts) {
      var result = executeHealthCheck(host.getAddress().getHostAddress(), host.getPort());
      if (!result.isHealthy()) {
        return result;
      }
    }
    return Result.healthy();
  }

  private Result checkHostListRandom(int startPort, int endPort) {
    var host = config.getHosts().get(ThreadLocalRandom.current().nextInt(config.getHosts().size()));
    int selectedPort = ThreadLocalRandom.current().nextInt(startPort, endPort);
    var result = executeHealthCheck(host, selectedPort);
    if (!result.isHealthy()) {
      return result;
    }
    return Result.healthy();
  }

  private Result checkHostListRandom(List<InetSocketAddress> sourceHostList) {
    var host = sourceHostList.get(ThreadLocalRandom.current().nextInt(sourceHostList.size()));
    var result = executeHealthCheck(host.getAddress().getHostAddress(), host.getPort());
    if (!result.isHealthy()) {
      return result;
    }
    return Result.healthy();
  }

  private Result checkHostPatternRandom(Matcher hostMatcher, int startHost, int endHost,
      int startPort, int endPort) {
    int selectedPort = ThreadLocalRandom.current().nextInt(startPort, endPort);
    String selectedHost = StringUtils.leftPad(
        String.valueOf(ThreadLocalRandom.current().nextInt(startHost, endHost)),
        hostMatcher.group(2).length() - String.valueOf(startHost).length(), '0');
    var host =
        hostMatcher.group(1) + selectedHost + "." + hostMatcher.group(4) + "."
            + hostMatcher.group(
            5);
    return executeHealthCheck(host, selectedPort);
  }

  private Result checkHostPatternSequential(Matcher hostMatcher, int startHost, int endHost,
      int startPort, int endPort) {
    for (int i = startHost; i <= endHost; i++) {
      int selectedPort = ThreadLocalRandom.current().nextInt(startPort, endPort);
      var selectedHost = StringUtils.leftPad(
          String.valueOf(i), hostMatcher.group(2).length() - String.valueOf(startHost).length(),
          '0');
      var host =
          hostMatcher.group(1) + selectedHost + "." + hostMatcher.group(4) + "."
              + hostMatcher.group(5);
      var result = executeHealthCheck(host, selectedPort);
      if (!result.isHealthy()) {
        return result;
      }
    }
    return Result.healthy();
  }

  private Result executeHealthCheck(String host, int selectedPort) {
    log.info("Cluster healthcheck {}:{}", host, selectedPort);
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, selectedPort), config.getConnectTimeout());
      if (socket.isConnected()) {
        return Result.healthy();
      }
      alert(host, selectedPort);
      if (config.getMode() == HealthCheckMode.ALERT) {
        return Result.healthy();
      }
      return Result.unhealthy(
          "Cluster host %s is not reachable on port range: %s".formatted(host, selectedPort));
    } catch (IOException e) {
      log.error("Error executing cluster reachability healthcheck for {}:{}", host, selectedPort, e);
      alert(host, selectedPort);
      if (config.getMode() == HealthCheckMode.ALERT) {
        return Result.healthy();
      }
      return Result.unhealthy(e);
    }
  }

  private void alert(String host, int port) {
    alertPublisher.publish(
        config.getName(),
        Result.unhealthy(
            "Cluster host %s is not reachable on port range: %s".formatted(host, port)));
  }
}
