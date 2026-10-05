package io.spring.application;

import io.spring.application.data.PlatformInfoData.BaselineInfo;
import io.spring.application.data.PlatformInfoData.DependencyUpgrade;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {
  private BaselineInfo baseline = new BaselineInfo();
  private List<DependencyUpgrade> dependencies = new ArrayList<>();
}
