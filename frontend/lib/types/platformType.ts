export interface PlatformRuntime {
  java: string;
  springBoot: string;
}

export interface DependencyUpgrade {
  name: string;
  before: string;
  after: string;
}

export interface EndpointInfo {
  method: string;
  path: string;
}

export interface PlatformInfo {
  runtime: PlatformRuntime;
  dependencies: DependencyUpgrade[];
  endpoints: EndpointInfo[];
}
