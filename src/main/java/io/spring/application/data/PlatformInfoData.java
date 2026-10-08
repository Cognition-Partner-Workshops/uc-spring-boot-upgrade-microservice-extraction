package io.spring.application.data;

import java.util.List;

public record PlatformInfoData(
    RuntimeInfoData runtime,
    List<DependencyUpgradeData> dependencies,
    List<EndpointData> endpoints) {}
