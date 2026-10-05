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

export type HttpMethod =
  | "GET"
  | "POST"
  | "PUT"
  | "DELETE"
  | "PATCH"
  | "HEAD"
  | "OPTIONS";

export interface EndpointInfo {
  method: HttpMethod;
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
