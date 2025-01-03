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
  private int connectTimeout = 10000;
  @Min(1000)
  private int readTimeout = 10000;
  private boolean verify;
  private String tlsVersion = "TLSv1.2";
  private HealthCheckMode mode = HealthCheckMode.NORMAL;
}
