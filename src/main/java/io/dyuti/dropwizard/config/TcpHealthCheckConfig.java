package io.dyuti.dropwizard.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Configuration for HTTP Health Check
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TcpHealthCheckConfig {

  @NotBlank
  private String name;
  @NotBlank private String host;
  @NotBlank private int port;

  @Min(1000)
  private int connectTimeout = 1000;

  private HealthCheckMode mode = HealthCheckMode.NORMAL;
}
