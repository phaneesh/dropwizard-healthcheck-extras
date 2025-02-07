package io.dyuti.dropwizard.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for Log Rate Health Check
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricHealthCheckConfig {
  @NotBlank
  private String name;
  @NotBlank
  private String metric;
  @NotNull
  private MetricType type;
  @NotBlank
  private String dimension = "m1_rate";
  @Min(0)
  private long threshold;

  private HealthCheckMode mode = HealthCheckMode.NORMAL;
}
