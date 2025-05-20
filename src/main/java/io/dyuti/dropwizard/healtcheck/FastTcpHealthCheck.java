package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.HealthCheckMode;
import io.dyuti.dropwizard.config.TcpHealthCheckConfig;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FastTcpHealthCheck extends HealthCheck {

  private final TcpHealthCheckConfig tcpHealthCheckConfig;
  private final AlertPublisher alertPublisher;

  @Override
  protected Result check() throws Exception {
    Instant startTime = Instant.now();
    try (SocketChannel socketChannel = SocketChannel.open();
        Selector selector = Selector.open()) {
      // Configure socket for non-blocking mode
      socketChannel.configureBlocking(false);
      socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
      // Register with selector for connection events
      socketChannel.register(selector, SelectionKey.OP_CONNECT);
      // Attempt to connect
      InetSocketAddress address = new InetSocketAddress(tcpHealthCheckConfig.getHost(),
          tcpHealthCheckConfig.getPort());
      boolean connected = socketChannel.connect(address);
      if (!connected) {
        // Wait for connection to complete
        if (selector.select(tcpHealthCheckConfig.getConnectTimeout()) == 0) {
          log.error("[{}] Health check timed out for {}:{} at {} ms",
              tcpHealthCheckConfig.getName(),
              tcpHealthCheckConfig.getHost(), tcpHealthCheckConfig.getPort(),
              tcpHealthCheckConfig.getConnectTimeout());
          return handleError(new Exception(
              "Connection timed out for " + tcpHealthCheckConfig.getHost() + ":"
                  + tcpHealthCheckConfig.getPort()));
        }
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey selectedKey = iterator.next();
          iterator.remove();
          if (selectedKey.isConnectable()) {
            try {
              connected = socketChannel.finishConnect();
            } catch (Exception e) {
              log.error("[{}] Health check error for {}:{}", tcpHealthCheckConfig.getName(),
                  tcpHealthCheckConfig.getHost(), tcpHealthCheckConfig.getPort(), e);
              return handleError(e);
            }
          }
        }
      }
      Duration responseTime = Duration.between(startTime, Instant.now());
      if (connected && socketChannel.isConnected()) {
        log.info("[{}] Healthcheck for {}:{} succeeded with response time of {} ms",
            tcpHealthCheckConfig.getName(),
            tcpHealthCheckConfig.getHost(), tcpHealthCheckConfig.getPort(),
            responseTime.toMillis());
        return Result.healthy("Response time: " + responseTime.toMillis() + " ms");
      } else {
        log.error("[{}] Health check failed for {}:{}", tcpHealthCheckConfig.getName(),
            tcpHealthCheckConfig.getHost(), tcpHealthCheckConfig.getPort());
        //Handle error
        return handleError(null);
      }
    } catch (Exception e) {
      log.error("[{}] Health check error for {}:{}", tcpHealthCheckConfig.getName(),
          tcpHealthCheckConfig.getHost(), tcpHealthCheckConfig.getPort(), e);
      return handleError(e);
    }
  }

  private Result handleError(Exception e) {
    alertPublisher.publish(tcpHealthCheckConfig.getName(), Result.unhealthy(e));
    if (tcpHealthCheckConfig.getMode() == HealthCheckMode.ALERT) {
      return Result.healthy();
    }
    return Result.unhealthy(e);
  }

  @Override
  public String toString() {
    return String.format("FastTcpHealthCheck{name=%s, host='%s', port=%d, timeout=%s, mode=%s}",
        tcpHealthCheckConfig.getName(), tcpHealthCheckConfig.getHost(),
        tcpHealthCheckConfig.getPort(), tcpHealthCheckConfig.getConnectTimeout(),
        tcpHealthCheckConfig.getMode());
  }
}
