import Head from "next/head";
import React from "react";
import axios from "axios";
import useSWR from "swr";

import ErrorMessage from "../components/common/ErrorMessage";
import LoadingSpinner from "../components/common/LoadingSpinner";
import PlatformAPI from "../lib/api/platform";
import {
  DependencyUpgrade,
  EndpointInfo,
  PlatformInfo,
} from "../lib/types/platformType";
import { SERVER_BASE_URL } from "../lib/utils/constant";

interface PingResult {
  status: number | null;
  latencyMs: number;
}

type PingMap = Record<string, PingResult | undefined>;

const isPingable = (endpoint: EndpointInfo) =>
  endpoint.method === "GET" && endpoint.path.indexOf("{") === -1;

const pingTone = (result: PingResult) => {
  if (result.status === null) return "red";
  if (result.status >= 200 && result.status < 300) return "green";
  if (result.status === 401 || result.status === 403) return "amber";
  if (result.status >= 500) return "red";
  return "grey";
};

const methodTone = (method: string) => {
  switch (method) {
    case "GET":
      return "get";
    case "POST":
      return "post";
    case "PUT":
      return "put";
    case "DELETE":
      return "delete";
    default:
      return "other";
  }
};

const groupByController = (endpoints: EndpointInfo[]) => {
  const groups: { controller: string; endpoints: EndpointInfo[] }[] = [];
  endpoints.forEach((endpoint) => {
    const group = groups.find((g) => g.controller === endpoint.controller);
    if (group) {
      group.endpoints.push(endpoint);
    } else {
      groups.push({ controller: endpoint.controller, endpoints: [endpoint] });
    }
  });
  return groups;
};

const pingEndpoint = async (path: string): Promise<PingResult> => {
  const start = performance.now();
  try {
    const response = await axios.get(`${SERVER_BASE_URL}${path}`, {
      validateStatus: () => true,
    });
    return {
      status: response.status,
      latencyMs: Math.round(performance.now() - start),
    };
  } catch (error) {
    return { status: null, latencyMs: Math.round(performance.now() - start) };
  }
};

interface VersionBadgeProps {
  label: string;
  before?: string;
  after: string;
  note?: string;
}

const VersionBadge = ({ label, before, after, note }: VersionBadgeProps) => (
  <div className="version-badge">
    <div className="version-label">{label}</div>
    <div className="version-values">
      {before && (
        <>
          <span className="version-before">{before}</span>
          <span className="version-arrow">→</span>
        </>
      )}
      <span className="version-after">{after}</span>
    </div>
    {note && <div className="version-note">{note}</div>}
    <style jsx>{`
      .version-badge {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.16);
        border-radius: 10px;
        padding: 18px 22px;
        min-width: 220px;
        text-align: left;
      }
      .version-label {
        font-size: 0.75rem;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: rgba(255, 255, 255, 0.65);
        margin-bottom: 8px;
        font-weight: 600;
      }
      .version-values {
        display: flex;
        align-items: baseline;
        flex-wrap: wrap;
      }
      .version-before {
        color: #ff6b6b;
        text-decoration: line-through;
        font-size: 1.4rem;
        font-weight: 600;
        margin-right: 10px;
      }
      .version-arrow {
        color: rgba(255, 255, 255, 0.6);
        font-size: 1.3rem;
        margin-right: 10px;
      }
      .version-after {
        color: #5cd65c;
        font-size: 2.2rem;
        font-weight: 700;
        line-height: 1;
      }
      .version-note {
        margin-top: 8px;
        font-size: 0.8rem;
        color: rgba(255, 255, 255, 0.6);
      }
    `}</style>
  </div>
);

interface SummaryTileProps {
  value: number;
  label: string;
}

const SummaryTile = ({ value, label }: SummaryTileProps) => (
  <div className="summary-tile">
    <div className="summary-value">{value}</div>
    <div className="summary-label">{label}</div>
    <style jsx>{`
      .summary-tile {
        background: #fff;
        border: 1px solid #e5e5e5;
        border-radius: 8px;
        padding: 22px 24px;
        text-align: center;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
      }
      .summary-value {
        font-size: 2.6rem;
        font-weight: 700;
        color: #5cb85c;
        line-height: 1.1;
      }
      .summary-label {
        margin-top: 4px;
        color: #777;
        font-size: 0.9rem;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }
    `}</style>
  </div>
);

const DependencyTable = ({
  dependencies,
}: {
  dependencies: DependencyUpgrade[];
}) => (
  <table className="dependency-table">
    <thead>
      <tr>
        <th>Dependency</th>
        <th className="col-before">Before</th>
        <th className="col-arrow" />
        <th className="col-after">After</th>
      </tr>
    </thead>
    <tbody>
      {dependencies.map((dependency) => (
        <tr key={dependency.name}>
          <td className="dep-name">{dependency.name}</td>
          <td className="dep-before">{dependency.from}</td>
          <td className="dep-arrow">→</td>
          <td className="dep-after">{dependency.to}</td>
        </tr>
      ))}
    </tbody>
    <style jsx>{`
      .dependency-table {
        width: 100%;
        border-collapse: collapse;
        background: #fff;
        border: 1px solid #e5e5e5;
        border-radius: 8px;
        overflow: hidden;
      }
      th,
      td {
        padding: 10px 16px;
        border-bottom: 1px solid #eee;
        text-align: left;
      }
      th {
        background: #f8f8f8;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: #777;
      }
      tbody tr:last-child td {
        border-bottom: none;
      }
      .col-before,
      .col-after {
        width: 22%;
      }
      .col-arrow {
        width: 40px;
      }
      .dep-name {
        font-weight: 600;
        color: #373a3c;
      }
      .dep-before,
      .dep-after,
      .dep-arrow {
        font-family: "SFMono-Regular", Menlo, Consolas, monospace;
        font-size: 0.9rem;
      }
      .dep-before {
        color: #d9534f;
        text-decoration: line-through;
      }
      .dep-arrow {
        color: #5cb85c;
        font-weight: 700;
        text-align: center;
      }
      .dep-after {
        color: #3c9a3c;
        font-weight: 700;
      }
    `}</style>
  </table>
);

interface EndpointGroupProps {
  controller: string;
  endpoints: EndpointInfo[];
  pings: PingMap;
}

const EndpointGroup = ({ controller, endpoints, pings }: EndpointGroupProps) => (
  <div className="endpoint-group">
    <div className="group-heading">
      <span className="group-name">{controller}</span>
      <span className="group-count">
        {endpoints.length} endpoint{endpoints.length === 1 ? "" : "s"}
      </span>
    </div>
    <table className="endpoint-table">
      <tbody>
        {endpoints.map((endpoint) => {
          const key = `${endpoint.method} ${endpoint.path}`;
          const ping = pings[key];
          const pingable = isPingable(endpoint);
          return (
            <tr key={key}>
              <td className="col-method">
                <span className={`method-pill method-${methodTone(endpoint.method)}`}>
                  {endpoint.method}
                </span>
              </td>
              <td className="col-path">{endpoint.path}</td>
              <td className="col-ping">
                {!pingable ? (
                  <span className="ping-na">—</span>
                ) : !ping ? (
                  <span className="ping-pending">pinging…</span>
                ) : (
                  <span className={`ping-result ping-${pingTone(ping)}`}>
                    <span className="ping-status">
                      {ping.status === null ? "ERR" : ping.status}
                    </span>
                    <span className="ping-latency">{ping.latencyMs} ms</span>
                  </span>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
    <style jsx>{`
      .endpoint-group {
        background: #fff;
        border: 1px solid #e5e5e5;
        border-radius: 8px;
        margin-bottom: 16px;
        overflow: hidden;
      }
      .group-heading {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 16px;
        background: #f8f8f8;
        border-bottom: 1px solid #eee;
      }
      .group-name {
        font-weight: 700;
        color: #373a3c;
        font-family: "SFMono-Regular", Menlo, Consolas, monospace;
      }
      .group-count {
        font-size: 0.8rem;
        color: #999;
      }
      .endpoint-table {
        width: 100%;
        border-collapse: collapse;
      }
      td {
        padding: 9px 16px;
        border-bottom: 1px solid #f0f0f0;
        vertical-align: middle;
      }
      tr:last-child td {
        border-bottom: none;
      }
      .col-method {
        width: 90px;
      }
      .col-path {
        font-family: "SFMono-Regular", Menlo, Consolas, monospace;
        font-size: 0.92rem;
        color: #373a3c;
      }
      .col-ping {
        width: 150px;
        text-align: right;
        font-size: 0.85rem;
      }
      .method-pill {
        display: inline-block;
        min-width: 64px;
        text-align: center;
        padding: 3px 8px;
        border-radius: 999px;
        font-size: 0.72rem;
        font-weight: 700;
        letter-spacing: 0.06em;
        color: #fff;
      }
      .method-get {
        background: #5cb85c;
      }
      .method-post {
        background: #337ab7;
      }
      .method-put {
        background: #f0ad4e;
      }
      .method-delete {
        background: #d9534f;
      }
      .method-other {
        background: #999;
      }
      .ping-na,
      .ping-pending {
        color: #bbb;
      }
      .ping-result {
        display: inline-flex;
        align-items: center;
        gap: 8px;
      }
      .ping-status {
        display: inline-block;
        min-width: 38px;
        text-align: center;
        padding: 2px 7px;
        border-radius: 4px;
        font-weight: 700;
        font-size: 0.75rem;
        color: #fff;
      }
      .ping-latency {
        color: #777;
        font-family: "SFMono-Regular", Menlo, Consolas, monospace;
      }
      .ping-green .ping-status {
        background: #5cb85c;
      }
      .ping-amber .ping-status {
        background: #f0ad4e;
      }
      .ping-red .ping-status {
        background: #d9534f;
      }
      .ping-grey .ping-status {
        background: #999;
      }
    `}</style>
  </div>
);

const PlatformContent = ({ platform }: { platform: PlatformInfo }) => {
  const [pings, setPings] = React.useState<PingMap>({});
  const { runtime, baseline, dependencies, endpoints, summary } = platform;

  React.useEffect(() => {
    let cancelled = false;
    endpoints.filter(isPingable).forEach((endpoint) => {
      const key = `${endpoint.method} ${endpoint.path}`;
      pingEndpoint(endpoint.path).then((result) => {
        if (!cancelled) {
          setPings((previous) => ({ ...previous, [key]: result }));
        }
      });
    });
    return () => {
      cancelled = true;
    };
  }, [endpoints]);

  const groups = groupByController(endpoints);

  return (
    <>
      <div className="platform-hero">
        <div className="container">
          <h1 className="hero-title">API Platform</h1>
          <p className="hero-subtitle">
            Runtime baseline for the Conduit REST API — live values from the
            running service
          </p>
          <div className="hero-badges">
            <VersionBadge
              label="Java"
              before={baseline.javaVersion}
              after={runtime.javaVersion}
              note={runtime.javaVendor}
            />
            <VersionBadge
              label="Spring Boot"
              before={baseline.springBootVersion}
              after={runtime.springBootVersion}
            />
            <VersionBadge
              label="Spring Framework"
              after={runtime.springFrameworkVersion}
            />
          </div>
        </div>
      </div>

      <div className="container page platform-body">
        <div className="row">
          <div className="col-md-6">
            <SummaryTile value={summary.endpointCount} label="REST endpoints" />
          </div>
          <div className="col-md-6">
            <SummaryTile
              value={summary.upgradedDependencyCount}
              label="Dependencies upgraded"
            />
          </div>
        </div>

        <h2 className="section-title">Dependency upgrades</h2>
        <DependencyTable dependencies={dependencies} />

        <h2 className="section-title">
          Endpoint inventory
          <span className="section-meta">
            {groups.length} controllers · {endpoints.length} endpoints
          </span>
        </h2>
        {groups.map((group) => (
          <EndpointGroup
            key={group.controller}
            controller={group.controller}
            endpoints={group.endpoints}
            pings={pings}
          />
        ))}
      </div>

      <style jsx>{`
        .platform-hero {
          background: linear-gradient(135deg, #1f2a44 0%, #2d3e66 50%, #0f172a 100%);
          color: #fff;
          padding: 2.5rem 0 2.75rem;
          margin-bottom: 2rem;
          box-shadow: inset 0 8px 8px -8px rgba(0, 0, 0, 0.3),
            inset 0 -8px 8px -8px rgba(0, 0, 0, 0.3);
        }
        .hero-title {
          font-family: "Titillium Web", sans-serif;
          font-weight: 700;
          font-size: 3rem;
          margin: 0 0 0.25rem;
          text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
        }
        .hero-subtitle {
          color: rgba(255, 255, 255, 0.7);
          margin: 0 0 1.75rem;
          font-size: 1.05rem;
        }
        .hero-badges {
          display: flex;
          flex-wrap: wrap;
          gap: 16px;
        }
        .platform-body {
          padding-bottom: 3rem;
        }
        .section-title {
          font-size: 1.35rem;
          font-weight: 700;
          color: #373a3c;
          margin: 2.25rem 0 1rem;
          display: flex;
          align-items: baseline;
          justify-content: space-between;
        }
        .section-meta {
          font-size: 0.85rem;
          font-weight: 400;
          color: #999;
        }
      `}</style>
    </>
  );
};

const Platform = () => {
  const { data, error } = useSWR("platform-info", PlatformAPI.getInfo);

  return (
    <>
      <Head>
        <title>API PLATFORM | NEXT REALWORLD</title>
        <meta
          name="description"
          content="Runtime versions, dependency upgrades and REST endpoint inventory of the Conduit API platform."
        />
      </Head>
      <div className="platform-page">
        {error ? (
          <ErrorMessage message="Unable to load platform information." />
        ) : !data ? (
          <LoadingSpinner />
        ) : (
          <PlatformContent platform={data.platform} />
        )}
      </div>
    </>
  );
};

export default Platform;
