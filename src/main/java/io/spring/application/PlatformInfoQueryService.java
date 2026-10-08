package io.spring.application;

import io.spring.application.data.EndpointData;
import io.spring.application.data.PlatformInfoData;
import io.spring.application.data.RuntimeInfoData;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
public class PlatformInfoQueryService {
  private static final String API_PACKAGE = "io.spring.api";
  private static final String ANY_METHOD = "ANY";

  private final Environment environment;
  private final RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final PlatformDependencies platformDependencies;

  public PlatformInfoQueryService(
      Environment environment,
      @Qualifier("requestMappingHandlerMapping")
          RequestMappingHandlerMapping requestMappingHandlerMapping,
      PlatformDependencies platformDependencies) {
    this.environment = environment;
    this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    this.platformDependencies = platformDependencies;
  }

  public PlatformInfoData platformInfo() {
    return new PlatformInfoData(runtime(), platformDependencies.getDependencies(), endpoints());
  }

  private RuntimeInfoData runtime() {
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    Instant startedAt =
        Instant.ofEpochMilli(runtimeMXBean.getStartTime()).truncatedTo(ChronoUnit.SECONDS);
    return new RuntimeInfoData(
        orEmpty(System.getProperty("java.version")),
        orEmpty(System.getProperty("java.vendor")),
        orEmpty(System.getProperty("java.runtime.name")),
        orEmpty(SpringBootVersion.getVersion()),
        orEmpty(SpringVersion.getVersion()),
        orEmpty(System.getProperty("os.name")),
        orEmpty(System.getProperty("os.arch")),
        DateTimeFormatter.ISO_INSTANT.format(startedAt),
        runtimeMXBean.getUptime() / 1000,
        List.of(environment.getActiveProfiles()));
  }

  private List<EndpointData> endpoints() {
    List<EndpointData> endpoints = new ArrayList<>();
    requestMappingHandlerMapping
        .getHandlerMethods()
        .forEach(
            (mappingInfo, handlerMethod) -> {
              if (!isApiHandler(handlerMethod)) {
                return;
              }
              String handler =
                  handlerMethod.getBeanType().getSimpleName()
                      + "#"
                      + handlerMethod.getMethod().getName();
              for (String path : pathsOf(mappingInfo)) {
                for (String method : methodsOf(mappingInfo)) {
                  endpoints.add(new EndpointData(method, path, handler));
                }
              }
            });
    return endpoints.stream()
        .sorted(Comparator.comparing(EndpointData::path).thenComparing(EndpointData::method))
        .collect(Collectors.toList());
  }

  private boolean isApiHandler(HandlerMethod handlerMethod) {
    String packageName = handlerMethod.getBeanType().getPackageName();
    return packageName.equals(API_PACKAGE) || packageName.startsWith(API_PACKAGE + ".");
  }

  private List<String> pathsOf(RequestMappingInfo mappingInfo) {
    Set<String> patterns =
        mappingInfo.getPathPatternsCondition() != null
            ? mappingInfo.getPathPatternsCondition().getPatternValues()
            : mappingInfo.getPatternsCondition().getPatterns();
    return patterns.stream()
        .map(pattern -> pattern.startsWith("/") ? pattern : "/" + pattern)
        .sorted()
        .collect(Collectors.toList());
  }

  private List<String> methodsOf(RequestMappingInfo mappingInfo) {
    Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
    if (methods.isEmpty()) {
      return List.of(ANY_METHOD);
    }
    return methods.stream()
        .map(method -> method.name().toUpperCase())
        .sorted()
        .collect(Collectors.toList());
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
