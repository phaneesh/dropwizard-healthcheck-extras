package io.dyuti.dropwizard.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for Disk Space Health Check
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiskSpaceHealthCheckConfig {
  @NotBlank
  private String name;
  @NotBlank
  private String path;
  @Min(102400)
  private long threshold;
}
