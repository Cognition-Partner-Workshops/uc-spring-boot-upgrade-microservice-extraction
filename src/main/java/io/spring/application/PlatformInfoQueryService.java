package io.spring.application;

import io.spring.application.data.DependencyUpgrade;
import io.spring.application.data.EndpointInfo;
import io.spring.application.data.PlatformInfoData;
import io.spring.application.data.RuntimeInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
public class PlatformInfoQueryService {
  private static final String API_PACKAGE = "io.spring.api";

  private final RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final PlatformProperties platformProperties;

  public PlatformInfoQueryService(
      @Qualifier("requestMappingHandlerMapping")
          RequestMappingHandlerMapping requestMappingHandlerMapping,
      PlatformProperties platformProperties) {
    this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    this.platformProperties = platformProperties;
  }

  public PlatformInfoData platformInfo() {
    return new PlatformInfoData(runtime(), dependencies(), endpoints());
  }

  private RuntimeInfo runtime() {
    return new RuntimeInfo(System.getProperty("java.version"), SpringBootVersion.getVersion());
  }

  private List<DependencyUpgrade> dependencies() {
    return new ArrayList<>(platformProperties.getDependencies());
  }

  private List<EndpointInfo> endpoints() {
    List<EndpointInfo> endpoints = new ArrayList<>();
    requestMappingHandlerMapping
        .getHandlerMethods()
        .forEach(
            (mapping, handler) -> {
              if (isApiController(handler)) {
                endpoints.addAll(expand(mapping));
              }
            });
    endpoints.sort(
        Comparator.comparing(EndpointInfo::getPath).thenComparing(EndpointInfo::getMethod));
    return endpoints;
  }

  private boolean isApiController(HandlerMethod handler) {
    return handler.getBeanType().getPackageName().startsWith(API_PACKAGE);
  }

  private List<EndpointInfo> expand(RequestMappingInfo mapping) {
    List<EndpointInfo> expanded = new ArrayList<>();
    Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
    for (String path : mapping.getPatternValues()) {
      String normalizedPath = path.startsWith("/") ? path : "/" + path;
      for (RequestMethod method : methods) {
        expanded.add(new EndpointInfo(method.name(), normalizedPath));
      }
    }
    return expanded;
  }
}
