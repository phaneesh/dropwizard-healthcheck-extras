package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dyuti.dropwizard.config.DiskSpaceHealthCheckConfig;
import java.io.File;
import lombok.RequiredArgsConstructor;

/**
 * Health Check for Disk Space
 */
@RequiredArgsConstructor
public class DiskSpaceHealthCheck extends HealthCheck {

  private final DiskSpaceHealthCheckConfig config;

  @Override
  protected Result check() {
    try {
      File diskPartition = new File(config.getPath());
      long freeSpace = diskPartition.getFreeSpace() / 1024;
      if (freeSpace < config.getThreshold()) {
        return Result.unhealthy("Disk Space is below threshold. Free Space: " + freeSpace);
      }
    } catch (Exception e) {
      return Result.unhealthy(e);
    }
    return Result.healthy();
  }
}
