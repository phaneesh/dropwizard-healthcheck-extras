package io.dyuti.dropwizard.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterReachabilityHealthCheckConfig {

  public enum SelectionMode {
    RANDOM, SEQUENTIAL
  }

  public enum HostNameMode {
    PATTERN, LIST
  }

  public enum HostListSource {
    CONFIG, DYNAMIC
  }

  @NotBlank
  private String name;
  private String hostNamePattern;
  private List<String> hosts = Collections.emptyList();
  @NotBlank
  private String portRange;
  @Min(1000)
  private int connectTimeout = 1000;
  @Min(60000)
  private int checkInterval = 43200000;
  @NotNull
  private HostNameMode hostNameMode = HostNameMode.PATTERN;
  @NotNull
  private HealthCheckMode mode = HealthCheckMode.NORMAL;
  @NotNull
  private SelectionMode selectionMode = SelectionMode.RANDOM;
  @NotNull
  private HostListSource hostListSource = HostListSource.CONFIG;
}
