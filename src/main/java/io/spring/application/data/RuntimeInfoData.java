package io.spring.application.data;

import java.util.List;

public record RuntimeInfoData(
    String javaVersion,
    String javaVendor,
    String javaRuntimeName,
    String springBoot,
    String springFramework,
    String osName,
    String osArch,
    String startedAt,
    long uptimeSeconds,
    List<String> activeProfiles) {}
