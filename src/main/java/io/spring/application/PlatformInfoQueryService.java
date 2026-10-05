package io.spring.application;

import io.spring.application.data.PlatformInfoData;
import io.spring.application.data.PlatformInfoData.BaselineInfo;
import io.spring.application.data.PlatformInfoData.DependencyUpgrade;
import io.spring.application.data.PlatformInfoData.EndpointInfo;
import io.spring.application.data.PlatformInfoData.PlatformSummary;
import io.spring.application.data.PlatformInfoData.RuntimeInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
public class PlatformInfoQueryService {
  private static final String API_PACKAGE = "io.spring.api";

  private final RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final String baselineJavaVersion;
  private final String baselineSpringBootVersion;

  public PlatformInfoQueryService(
      @Qualifier("requestMappingHandlerMapping")
          RequestMappingHandlerMapping requestMappingHandlerMapping,
      @Value("${platform.baseline.java-version}") String baselineJavaVersion,
      @Value("${platform.baseline.spring-boot-version}") String baselineSpringBootVersion) {
    this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    this.baselineJavaVersion = baselineJavaVersion;
    this.baselineSpringBootVersion = baselineSpringBootVersion;
  }

  public PlatformInfoData platformInfo() {
    RuntimeInfo runtime =
        new RuntimeInfo(
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            SpringBootVersion.getVersion(),
            SpringVersion.getVersion());
    BaselineInfo baseline = new BaselineInfo(baselineJavaVersion, baselineSpringBootVersion);
    List<DependencyUpgrade> dependencies = upgradedDependencies();
    List<EndpointInfo> endpoints = endpoints();
    return new PlatformInfoData(
        runtime,
        baseline,
        dependencies,
        endpoints,
        new PlatformSummary(endpoints.size(), dependencies.size()));
  }

  /** Before/after table of the upgrade. The "to" values mirror what is declared in build.gradle. */
  private List<DependencyUpgrade> upgradedDependencies() {
    List<DependencyUpgrade> dependencies = new ArrayList<>();
    dependencies.add(
        new DependencyUpgrade(
            "Spring Boot", baselineSpringBootVersion, SpringBootVersion.getVersion()));
    dependencies.add(
        new DependencyUpgrade("Spring Framework", "5.3.15", SpringVersion.getVersion()));
    dependencies.add(new DependencyUpgrade("Java", baselineJavaVersion, "17"));
    dependencies.add(new DependencyUpgrade("Gradle", "7.4", "8.8"));
    dependencies.add(new DependencyUpgrade("Netflix DGS", "4.9.21", "9.0.4"));
    dependencies.add(new DependencyUpgrade("MyBatis Spring Boot", "2.2.2", "3.0.4"));
    dependencies.add(new DependencyUpgrade("jjwt", "0.11.2", "0.12.6"));
    dependencies.add(new DependencyUpgrade("sqlite-jdbc", "3.36.0.3", "3.49.1.0"));
    dependencies.add(new DependencyUpgrade("REST Assured", "4.5.1", "5.5.6"));
    dependencies.add(new DependencyUpgrade("Servlet / Validation API", "javax", "jakarta"));
    dependencies.add(
        new DependencyUpgrade(
            "Spring Security config", "WebSecurityConfigurerAdapter", "SecurityFilterChain"));
    return dependencies;
  }

  private List<EndpointInfo> endpoints() {
    List<EndpointInfo> endpoints = new ArrayList<>();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
        requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
      HandlerMethod handlerMethod = entry.getValue();
      if (!API_PACKAGE.equals(handlerMethod.getBeanType().getPackageName())) {
        continue;
      }
      String controller = handlerMethod.getBeanType().getSimpleName();
      for (String path : pathsOf(entry.getKey())) {
        for (String method : methodsOf(entry.getKey())) {
          endpoints.add(new EndpointInfo(method, path, controller));
        }
      }
    }
    return endpoints.stream()
        .sorted(Comparator.comparing(EndpointInfo::getPath).thenComparing(EndpointInfo::getMethod))
        .collect(Collectors.toList());
  }

  private Set<String> pathsOf(RequestMappingInfo mappingInfo) {
    Set<String> patterns;
    if (mappingInfo.getPathPatternsCondition() != null) {
      patterns = mappingInfo.getPathPatternsCondition().getPatternValues();
    } else if (mappingInfo.getPatternsCondition() != null) {
      patterns = mappingInfo.getPatternsCondition().getPatterns();
    } else {
      patterns = Set.of();
    }
    return patterns.stream()
        .map(pattern -> pattern.startsWith("/") ? pattern : "/" + pattern)
        .collect(Collectors.toSet());
  }

  private Set<String> methodsOf(RequestMappingInfo mappingInfo) {
    Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
    if (methods.isEmpty()) {
      return Set.of("GET");
    }
    return methods.stream().map(RequestMethod::name).collect(Collectors.toSet());
  }
}
