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

  private final RequestMappingHandlerMapping handlerMapping;
  private final PlatformProperties platformProperties;

  public PlatformInfoQueryService(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      PlatformProperties platformProperties) {
    this.handlerMapping = handlerMapping;
    this.platformProperties = platformProperties;
  }

  public PlatformInfoData platformInfo() {
    List<DependencyUpgrade> dependencies = new ArrayList<>(platformProperties.getDependencies());
    List<EndpointInfo> endpoints = endpoints();
    BaselineInfo baseline = platformProperties.getBaseline();
    return new PlatformInfoData(
        new RuntimeInfo(
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            SpringBootVersion.getVersion(),
            SpringVersion.getVersion()),
        new BaselineInfo(baseline.getJavaVersion(), baseline.getSpringBootVersion()),
        dependencies,
        endpoints,
        new PlatformSummary(endpoints.size(), dependencies.size()));
  }

  private List<EndpointInfo> endpoints() {
    List<EndpointInfo> result = new ArrayList<>();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
        handlerMapping.getHandlerMethods().entrySet()) {
      Class<?> controller = entry.getValue().getBeanType();
      if (!controller.getPackageName().startsWith(API_PACKAGE)) {
        continue;
      }
      RequestMappingInfo info = entry.getKey();
      for (String path : pathsOf(info)) {
        for (RequestMethod method : info.getMethodsCondition().getMethods()) {
          result.add(new EndpointInfo(method.name(), path, controller.getSimpleName()));
        }
      }
    }
    return result.stream()
        .sorted(Comparator.comparing(EndpointInfo::getPath).thenComparing(EndpointInfo::getMethod))
        .collect(Collectors.toList());
  }

  private Set<String> pathsOf(RequestMappingInfo info) {
    if (info.getPathPatternsCondition() != null) {
      return info.getPathPatternsCondition().getPatternValues();
    }
    return info.getPatternsCondition() != null
        ? info.getPatternsCondition().getPatterns()
        : Set.of();
  }
}
