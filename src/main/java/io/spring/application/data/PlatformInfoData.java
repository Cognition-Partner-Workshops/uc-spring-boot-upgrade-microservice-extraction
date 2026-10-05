package io.spring.application.data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformInfoData {
  private RuntimeInfo runtime;
  private BaselineInfo baseline;
  private List<DependencyUpgrade> dependencies;
  private List<EndpointInfo> endpoints;
  private PlatformSummary summary;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RuntimeInfo {
    private String javaVersion;
    private String javaVendor;
    private String springBootVersion;
    private String springFrameworkVersion;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BaselineInfo {
    private String javaVersion;
    private String springBootVersion;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DependencyUpgrade {
    private String name;
    private String from;
    private String to;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EndpointInfo {
    private String method;
    private String path;
    private String controller;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlatformSummary {
    private int endpointCount;
    private int upgradedDependencyCount;
  }
}
