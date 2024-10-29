package io.dyuti.dropwizard.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for HTTP Health Check
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpHealthCheckConfig {
  @NotBlank
  private String name;
  @NotBlank
  private String url;
  @Min(1000)
  private int connectTimeout;
  private boolean verify;

  private HealthCheckMode mode = HealthCheckMode.NORMAL;
}
