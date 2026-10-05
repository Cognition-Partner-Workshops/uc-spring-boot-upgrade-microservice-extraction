export interface PlatformRuntime {
  javaVersion: string;
  javaVendor: string;
  springBootVersion: string;
  springFrameworkVersion: string;
}

export interface PlatformBaseline {
  javaVersion: string;
  springBootVersion: string;
}

export interface DependencyUpgrade {
  name: string;
  from: string;
  to: string;
}

export interface EndpointInfo {
  method: string;
  path: string;
  controller: string;
}

export interface PlatformSummary {
  endpointCount: number;
  upgradedDependencyCount: number;
}

export interface PlatformInfo {
  runtime: PlatformRuntime;
  baseline: PlatformBaseline;
  dependencies: DependencyUpgrade[];
  endpoints: EndpointInfo[];
  summary: PlatformSummary;
}

export interface PlatformInfoResponse {
  platform: PlatformInfo;
}

export interface PingResult {
  status: number;
  latencyMs: number;
}
