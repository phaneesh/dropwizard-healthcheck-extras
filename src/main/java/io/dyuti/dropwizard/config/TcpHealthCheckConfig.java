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
public class TcpHealthCheckConfig {

  @NotBlank private String name;
  @NotBlank private String host;
  @NotBlank private int port;

  @Min(1000)
  private int connectTimeout = 1000;
}
