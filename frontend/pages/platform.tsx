import axios from "axios";
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
} from "../lib/types/platformType";
import { SERVER_BASE_URL } from "../lib/utils/constant";

interface PingResult {
  status: number | null;
  latencyMs: number;
}

type PingState = Record<string, PingResult | "pending">;

const METHOD_COLORS: Record<string, string> = {
  GET: "#5cb85c",
  POST: "#3b82f6",
  PUT: "#f59e0b",
  DELETE: "#ef4444",
};

const methodColor = (method: string) =>
  METHOD_COLORS[method.toUpperCase()] || "#6b7280";

const isPingable = (endpoint: EndpointInfo) =>
  endpoint.method.toUpperCase() === "GET" && endpoint.path.indexOf("{") === -1;

const pingKey = (endpoint: EndpointInfo) =>
  `${endpoint.method} ${endpoint.path}`;

const pingClass = (status: number | null) => {
  if (status === null) return "ping-error";
  if (status >= 200 && status < 300) return "ping-ok";
  if (status === 401 || status === 403) return "ping-auth";
  if (status >= 500) return "ping-error";
  return "ping-other";
};

const findDependency = (
  dependencies: DependencyUpgrade[],
  name: string
): DependencyUpgrade | undefined =>
  dependencies.find((d) => d.name.toLowerCase() === name.toLowerCase());

const VersionBadge = ({
  label,
  before,
  after,
}: {
  label: string;
  before?: string;
  after: string;
}) => (
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
    <style jsx>{`
      .version-badge {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.15);
        border-radius: 10px;
        padding: 18px 28px;
        min-width: 220px;
        text-align: center;
      }
      .version-label {
        font-size: 0.85rem;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: rgba(255, 255, 255, 0.7);
        margin-bottom: 8px;
      }
      .version-values {
        display: flex;
        align-items: baseline;
        justify-content: center;
        gap: 12px;
        flex-wrap: wrap;
        font-family: "Titillium Web", sans-serif;
        font-weight: 700;
      }
      .version-before {
        font-size: 1.6rem;
        color: #ff6b6b;
        text-decoration: line-through;
        opacity: 0.85;
      }
      .version-arrow {
        font-size: 1.4rem;
        color: rgba(255, 255, 255, 0.6);
      }
      .version-after {
        font-size: 2.4rem;
        color: #5cb85c;
      }
    `}</style>
  </div>
);

const RuntimeSection = ({ info }: { info: PlatformInfo }) => {
  const javaDep = findDependency(info.dependencies, "Java");
  const bootDep = findDependency(info.dependencies, "Spring Boot");
  return (
    <div className="platform-hero">
      <div className="container">
        <h1 className="hero-title">API Platform</h1>
        <p className="hero-subtitle">
          Live runtime versions reported by the conduit backend
        </p>
        <div className="badges">
          <VersionBadge
            label="Java"
            before={javaDep && javaDep.before}
            after={info.runtime.java}
          />
          <VersionBadge
            label="Spring Boot"
            before={bootDep && bootDep.before}
            after={info.runtime.springBoot}
          />
        </div>
      </div>
      <style jsx>{`
        .platform-hero {
          background: linear-gradient(135deg, #1f2d3d 0%, #0f1a26 100%);
          color: #fff;
          padding: 2.5rem 0 3rem;
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
          color: rgba(255, 255, 255, 0.75);
          font-size: 1.1rem;
          margin-bottom: 1.75rem;
        }
        .badges {
          display: flex;
          gap: 20px;
          flex-wrap: wrap;
        }
      `}</style>
    </div>
  );
};

const CountBadge = ({ children }: { children: React.ReactNode }) => (
  <span className="count-badge">
    {children}
    <style jsx>{`
      .count-badge {
        display: inline-block;
        margin-left: 12px;
        padding: 2px 10px;
        border-radius: 999px;
        background: #5cb85c;
        color: #fff;
        font-size: 0.8rem;
        font-weight: 600;
        vertical-align: middle;
      }
    `}</style>
  </span>
);

const DependencySection = ({
  dependencies,
}: {
  dependencies: DependencyUpgrade[];
}) => (
  <section className="platform-section">
    <h2>
      Dependency upgrades
      <CountBadge>{dependencies.length} upgraded</CountBadge>
    </h2>
    <div className="table-wrap">
      <table className="platform-table">
        <thead>
          <tr>
            <th>Dependency</th>
            <th>Before</th>
            <th className="arrow-col">→</th>
            <th>After</th>
          </tr>
        </thead>
        <tbody>
          {dependencies.map((dep) => (
            <tr key={dep.name}>
              <td className="dep-name">{dep.name}</td>
              <td className="dep-before">{dep.before}</td>
              <td className="arrow-col dep-arrow">→</td>
              <td className="dep-after">{dep.after}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
    <style jsx>{`
      .dep-name {
        font-weight: 600;
      }
      .dep-before {
        color: #999;
        text-decoration: line-through;
        font-family: monospace;
      }
      .dep-arrow {
        color: #5cb85c;
        font-weight: 700;
      }
      .dep-after {
        color: #3d8b3d;
        font-weight: 700;
        font-family: monospace;
      }
      .arrow-col {
        text-align: center;
        width: 40px;
      }
    `}</style>
  </section>
);

const EndpointSection = ({
  endpoints,
  pings,
}: {
  endpoints: EndpointInfo[];
  pings: PingState;
}) => (
  <section className="platform-section">
    <h2>
      Endpoint inventory
      <CountBadge>{endpoints.length} endpoints</CountBadge>
    </h2>
    <div className="table-wrap">
      <table className="platform-table">
        <thead>
          <tr>
            <th className="method-col">Method</th>
            <th>Path</th>
            <th className="ping-col">Live ping</th>
          </tr>
        </thead>
        <tbody>
          {endpoints.map((endpoint) => {
            const key = pingKey(endpoint);
            const ping = pings[key];
            return (
              <tr key={key}>
                <td className="method-col">
                  <span
                    className="method-pill"
                    style={{ background: methodColor(endpoint.method) }}
                  >
                    {endpoint.method}
                  </span>
                </td>
                <td className="endpoint-path">{endpoint.path}</td>
                <td className="ping-col">
                  {!isPingable(endpoint) || ping === undefined ? (
                    <span className="ping-na">—</span>
                  ) : ping === "pending" ? (
                    <span className="ping-pending">pinging…</span>
                  ) : (
                    <span className={`ping-result ${pingClass(ping.status)}`}>
                      {ping.status === null ? "ERR" : ping.status}
                      <span className="ping-latency">
                        {Math.round(ping.latencyMs)} ms
                      </span>
                    </span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
    <style jsx>{`
      .method-col {
        width: 100px;
      }
      .ping-col {
        width: 160px;
        white-space: nowrap;
      }
      .method-pill {
        display: inline-block;
        min-width: 64px;
        text-align: center;
        padding: 2px 8px;
        border-radius: 4px;
        color: #fff;
        font-size: 0.75rem;
        font-weight: 700;
        letter-spacing: 0.05em;
      }
      .endpoint-path {
        font-family: monospace;
        font-size: 0.95rem;
      }
      .ping-na {
        color: #bbb;
      }
      .ping-pending {
        color: #999;
        font-style: italic;
      }
      .ping-result {
        font-weight: 700;
        padding: 2px 8px;
        border-radius: 4px;
      }
      .ping-latency {
        margin-left: 8px;
        font-weight: 400;
        color: #777;
        font-family: monospace;
      }
      .ping-ok {
        color: #3d8b3d;
        background: rgba(92, 184, 92, 0.12);
      }
      .ping-auth {
        color: #b7791f;
        background: rgba(245, 158, 11, 0.15);
      }
      .ping-error {
        color: #c0392b;
        background: rgba(239, 68, 68, 0.12);
      }
      .ping-other {
        color: #555;
        background: rgba(0, 0, 0, 0.05);
      }
    `}</style>
  </section>
);

const usePings = (endpoints: EndpointInfo[] | undefined): PingState => {
  const [pings, setPings] = React.useState<PingState>({});

  React.useEffect(() => {
    if (!endpoints) return;
    const targets = endpoints.filter(isPingable);
    if (targets.length === 0) return;
    let cancelled = false;

    setPings(
      targets.reduce<PingState>((acc, endpoint) => {
        acc[pingKey(endpoint)] = "pending";
        return acc;
      }, {})
    );

    targets.forEach(async (endpoint) => {
      const start = performance.now();
      let status: number | null = null;
      try {
        const response = await axios.get(`${SERVER_BASE_URL}${endpoint.path}`, {
          validateStatus: () => true,
        });
        status = response.status;
      } catch (error) {
        status = null;
      }
      const latencyMs = performance.now() - start;
      if (cancelled) return;
      setPings((prev) => ({
        ...prev,
        [pingKey(endpoint)]: { status, latencyMs },
      }));
    });

    return () => {
      cancelled = true;
    };
  }, [endpoints]);

  return pings;
};

const Platform = () => {
  const { data, error } = useSWR<PlatformInfo>(
    "platform-info",
    PlatformAPI.getInfo
  );
  const pings = usePings(data && data.endpoints);

  return (
    <>
      <Head>
        <title>API PLATFORM | NEXT REALWORLD</title>
        <meta
          name="description"
          content="Runtime versions, dependency upgrades and endpoint inventory of the conduit backend"
        />
      </Head>
      <div className="platform-page">
        {error ? (
          <div className="container page">
            <ErrorMessage message="Cannot load platform info from the backend." />
          </div>
        ) : !data ? (
          <LoadingSpinner />
        ) : (
          <>
            <RuntimeSection info={data} />
            <div className="container page">
              <DependencySection dependencies={data.dependencies} />
              <EndpointSection endpoints={data.endpoints} pings={pings} />
            </div>
          </>
        )}
      </div>
      <style jsx global>{`
        .platform-page .platform-section {
          margin-bottom: 2.5rem;
        }
        .platform-page .platform-section h2 {
          font-family: "Titillium Web", sans-serif;
          font-weight: 700;
          font-size: 1.6rem;
          margin-bottom: 1rem;
          color: #373a3c;
        }
        .platform-page .table-wrap {
          overflow-x: auto;
          border: 1px solid #e5e5e5;
          border-radius: 6px;
        }
        .platform-page .platform-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.95rem;
        }
        .platform-page .platform-table th {
          text-align: left;
          padding: 10px 14px;
          background: #f3f3f3;
          color: #555;
          font-size: 0.8rem;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          border-bottom: 1px solid #e5e5e5;
        }
        .platform-page .platform-table td {
          padding: 10px 14px;
          border-bottom: 1px solid #f0f0f0;
          vertical-align: middle;
        }
        .platform-page .platform-table tbody tr:last-child td {
          border-bottom: none;
        }
        .platform-page .platform-table tbody tr:hover td {
          background: #fafafa;
        }
        @media (max-width: 576px) {
          .platform-page .platform-table th,
          .platform-page .platform-table td {
            padding: 8px 10px;
          }
        }
      `}</style>
    </>
  );
};

export default Platform;
