package io.spring.application;

import io.spring.application.data.DependencyUpgradeData;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Static before/after list of the build and runtime dependencies bumped by the upgrade. */
@Component
@ConfigurationProperties(prefix = "platform")
@Getter
@Setter
public class PlatformDependencies {
  private List<DependencyUpgradeData> dependencies = new ArrayList<>();
}
