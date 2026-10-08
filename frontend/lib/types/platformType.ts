export interface PlatformRuntime {
  javaVersion: string;
  javaVendor: string;
  javaRuntimeName: string;
  springBoot: string;
  springFramework: string;
  osName: string;
  osArch: string;
  startedAt: string;
  uptimeSeconds: number;
  activeProfiles: string[];
}

export interface DependencyUpgrade {
  name: string;
  before: string;
  after: string;
}

export interface EndpointInfo {
  method: string;
  path: string;
  handler: string;
}

export interface PlatformInfo {
  runtime: PlatformRuntime;
  dependencies: DependencyUpgrade[];
  endpoints: EndpointInfo[];
}
