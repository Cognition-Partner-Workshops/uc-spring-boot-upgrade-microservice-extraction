import Head from "next/head";
import React from "react";
import useSWR from "swr";

import ErrorMessage from "../components/common/ErrorMessage";
import LoadingSpinner from "../components/common/LoadingSpinner";
import PlatformAPI from "../lib/api/platform";
import {
  DependencyUpgrade,
  EndpointInfo,
  PlatformInfo,
  PlatformRuntime,
} from "../lib/types/platformType";

const methodColors: { [method: string]: string } = {
  GET: "#5cb85c",
  POST: "#337ab7",
  PUT: "#f0ad4e",
  DELETE: "#d9534f",
};

const methodColor = (method: string) =>
  methodColors[method.toUpperCase()] || "#818a91";

const formatUptime = (totalSeconds: number) => {
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (days > 0 || hours > 0) parts.push(`${hours}h`);
  if (days > 0 || hours > 0 || minutes > 0) parts.push(`${minutes}m`);
  parts.push(`${seconds}s`);
  return parts.join(" ");
};

interface VersionCardProps {
  label: string;
  value: string;
  detail?: string;
}

const VersionCard = ({ label, value, detail }: VersionCardProps) => (
  <div className="col-md-3 col-sm-6">
    <div className="card platform-card">
      <div className="card-block text-xs-center">
        <p className="text-muted platform-card-label">{label}</p>
        <h3 className="platform-card-value">{value}</h3>
        {detail && <p className="text-muted platform-card-detail">{detail}</p>}
      </div>
    </div>
  </div>
);

const RuntimeCards = ({ runtime }: { runtime: PlatformRuntime }) => (
  <div className="row">
    <VersionCard
      label="Java"
      value={runtime.javaVersion}
      detail={`${runtime.javaVendor} · ${runtime.javaRuntimeName}`}
    />
    <VersionCard label="Spring Boot" value={runtime.springBoot} />
    <VersionCard label="Spring Framework" value={runtime.springFramework} />
    <div className="col-md-3 col-sm-6">
      <div className="card platform-card">
        <div className="card-block">
          <p className="text-muted platform-card-label">Runtime</p>
          <dl className="platform-runtime-list">
            <dt>OS</dt>
            <dd>
              {runtime.osName} ({runtime.osArch})
            </dd>
            <dt>Started at</dt>
            <dd>{runtime.startedAt}</dd>
            <dt>Uptime</dt>
            <dd>{formatUptime(runtime.uptimeSeconds)}</dd>
            <dt>Active profiles</dt>
            <dd>
              {runtime.activeProfiles.length > 0
                ? runtime.activeProfiles.join(", ")
                : "default"}
            </dd>
          </dl>
        </div>
      </div>
    </div>
  </div>
);

const DependenciesTable = ({
  dependencies,
}: {
  dependencies: DependencyUpgrade[];
}) => (
  <table className="table platform-table">
    <thead>
      <tr>
        <th>Dependency</th>
        <th>Before</th>
        <th>After</th>
      </tr>
    </thead>
    <tbody>
      {dependencies.map((dependency) => (
        <tr key={dependency.name}>
          <td>{dependency.name}</td>
          <td>
            <span className="text-muted platform-before">
              {dependency.before}
            </span>
          </td>
          <td>
            <span className="platform-after">{dependency.after}</span>
          </td>
        </tr>
      ))}
    </tbody>
  </table>
);

const EndpointsTable = ({ endpoints }: { endpoints: EndpointInfo[] }) => (
  <table className="table platform-table">
    <thead>
      <tr>
        <th>Method</th>
        <th>Path</th>
        <th>Handler</th>
      </tr>
    </thead>
    <tbody>
      {endpoints.map((endpoint) => (
        <tr key={`${endpoint.method} ${endpoint.path}`}>
          <td>
            <span
              className="platform-method"
              style={{ backgroundColor: methodColor(endpoint.method) }}
            >
              {endpoint.method}
            </span>
          </td>
          <td>
            <code>{endpoint.path}</code>
          </td>
          <td>{endpoint.handler}</td>
        </tr>
      ))}
    </tbody>
  </table>
);

const PlatformContent = () => {
  const { data, error } = useSWR<PlatformInfo>(
    "platform/info",
    PlatformAPI.getPlatformInfo
  );

  if (error) {
    return (
      <ErrorMessage message="Cannot load platform info. Is the backend running and does it expose GET /platform/info?" />
    );
  }

  if (!data) return <LoadingSpinner />;

  return (
    <>
      <RuntimeCards runtime={data.runtime} />

      <div className="row">
        <div className="col-md-12">
          <h2 className="platform-section-title">Dependency upgrades</h2>
          <DependenciesTable dependencies={data.dependencies} />
        </div>
      </div>

      <div className="row">
        <div className="col-md-12">
          <h2 className="platform-section-title">
            REST endpoints{" "}
            <small className="text-muted">
              {data.endpoints.length} endpoints
            </small>
          </h2>
          <EndpointsTable endpoints={data.endpoints} />
        </div>
      </div>
    </>
  );
};

const Platform = () => (
  <>
    <Head>
      <title>API PLATFORM | NEXT REALWORLD</title>
      <meta
        name="description"
        content="Live runtime versions, dependency upgrades and REST endpoints of the Conduit backend"
      />
    </Head>
    <div className="platform-page">
      <div className="container page">
        <div className="row">
          <div className="col-md-12">
            <h1>API Platform</h1>
            <p className="text-muted">
              Live runtime versions, dependency upgrades and REST endpoints
              reported by the Spring Boot backend.
            </p>
          </div>
        </div>
        <PlatformContent />
      </div>
    </div>
    <style jsx global>
      {`
        .platform-card {
          margin-bottom: 1.5rem;
          min-height: 150px;
        }
        .platform-card-label {
          margin-bottom: 0.25rem;
          text-transform: uppercase;
          font-size: 0.8rem;
          letter-spacing: 0.05em;
        }
        .platform-card-value {
          margin: 0.25rem 0;
          font-weight: 700;
          color: #5cb85c;
        }
        .platform-card-detail {
          margin-bottom: 0;
          font-size: 0.85rem;
        }
        .platform-runtime-list {
          margin-bottom: 0;
          font-size: 0.85rem;
        }
        .platform-runtime-list dt {
          color: #818a91;
          font-weight: 600;
          margin-top: 0.25rem;
        }
        .platform-runtime-list dd {
          margin-left: 0;
          margin-bottom: 0;
        }
        .platform-section-title {
          margin-top: 1rem;
          margin-bottom: 1rem;
          font-size: 1.5rem;
        }
        .platform-section-title small {
          font-size: 1rem;
        }
        .platform-table th {
          border-top: 0;
        }
        .platform-before {
          text-decoration: line-through;
        }
        .platform-after {
          color: #3d8b3d;
          font-weight: 700;
        }
        .platform-method {
          display: inline-block;
          min-width: 64px;
          padding: 0.2rem 0.5rem;
          border-radius: 3px;
          color: #fff;
          font-size: 0.75rem;
          font-weight: 700;
          text-align: center;
          letter-spacing: 0.05em;
        }
      `}
    </style>
  </>
);

export default Platform;
