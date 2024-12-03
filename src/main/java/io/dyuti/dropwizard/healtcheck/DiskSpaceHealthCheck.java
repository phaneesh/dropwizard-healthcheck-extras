package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.alert.AlertPublisher;
import io.dyuti.dropwizard.config.DiskSpaceHealthCheckConfig;
import io.dyuti.dropwizard.config.HealthCheckMode;
import java.io.File;
import lombok.RequiredArgsConstructor;

/** Health Check for Disk Space */
@RequiredArgsConstructor
public class DiskSpaceHealthCheck extends HealthCheck {

  private final DiskSpaceHealthCheckConfig config;
  private final AlertPublisher alertPublisher;

  @Override
  protected Result check() {
    try {
      File diskPartition = new File(config.getPath());
      long freeSpace = diskPartition.getFreeSpace() / 1024;
      if (freeSpace < config.getThreshold()) {
        alert(freeSpace);
        if (config.getMode() == HealthCheckMode.ALERT) {
          return Result.healthy();
        } else {
          return Result.unhealthy("Disk Space is below threshold. Free Space: " + freeSpace);
        }
      }
    } catch (Exception e) {
      alert(0);
      if (config.getMode() == HealthCheckMode.ALERT) {
        return Result.healthy();
      }
      return Result.unhealthy(e);
    }
    return Result.healthy();
  }

  private void alert(long freeSpace) {
    alertPublisher.publish(
        config.getName(),
        Result.unhealthy("Disk Space is below threshold. Free Space: " + freeSpace));
  }
}
