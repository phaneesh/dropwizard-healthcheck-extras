package io.dyuti.dropwizard.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bundle configuration for configuring healthchecks for TCP and HTTP(s)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthcheckExtrasConfig {
  private List<TcpHealthCheckConfig> tcp = new ArrayList<>();
  private List<HttpHealthCheckConfig> http = new ArrayList<>();
  private List<DiskSpaceHealthCheckConfig> disk = new ArrayList<>();
  private List<MetricHealthCheckConfig> metric = new ArrayList<>();
}
