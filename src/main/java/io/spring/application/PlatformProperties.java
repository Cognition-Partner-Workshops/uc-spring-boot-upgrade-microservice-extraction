package io.spring.application;

import io.spring.application.data.DependencyUpgrade;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {
  private List<DependencyUpgrade> dependencies = new ArrayList<>();
}
