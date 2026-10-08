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
  private List<DependencyUpgrade> dependencies;
  private List<EndpointInfo> endpoints;
}
