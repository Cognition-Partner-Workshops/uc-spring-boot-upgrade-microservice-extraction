import React from "react";

import PlatformAPI from "../../lib/api/platform";
import { EndpointInfo, PingResult } from "../../lib/types/platformType";

interface EndpointInventoryProps {
  endpoints: EndpointInfo[];
}

type PingState = { [key: string]: PingResult | "pending" };

const methodColors: { [method: string]: string } = {
  GET: "#2ecc71",
  POST: "#3498db",
  PUT: "#f39c12",
  DELETE: "#e74c3c",
};

const isPingable = (endpoint: EndpointInfo) =>
  endpoint.method === "GET" && endpoint.path.indexOf("{") === -1;

const endpointKey = (endpoint: EndpointInfo) =>
  `${endpoint.method} ${endpoint.path}`;

const statusClass = (status: number) => {
  if (status === 0) return "offline";
  if (status >= 200 && status < 300) return "ok";
  if (status === 401 || status === 403) return "auth";
  return "error";
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
  return groups.sort((a, b) => a.controller.localeCompare(b.controller));
};

interface PingCellProps {
  endpoint: EndpointInfo;
  ping?: PingResult | "pending";
}

const PingCell = ({ endpoint, ping }: PingCellProps) => {
  const pingable = isPingable(endpoint);
  const resolved = ping && ping !== "pending" ? ping : null;
  return (
    <span className="ping-cell">
      {!pingable && <span className="ping-na">—</span>}
      {pingable && !resolved && (
        <span className="ping-pending">pinging…</span>
      )}
      {pingable && resolved && (
        <>
          <span className={`ping-status ${statusClass(resolved.status)}`}>
            {resolved.status === 0 ? "offline" : resolved.status}
          </span>
          <span className="ping-latency">{resolved.latencyMs} ms</span>
        </>
      )}
      <style jsx>
        {`
          .ping-cell {
            flex: 0 0 auto;
            margin-left: 0.75rem;
            text-align: right;
            white-space: nowrap;
          }
          .ping-na,
          .ping-pending {
            color: #94a3b8;
            font-size: 0.8rem;
          }
          .ping-status {
            display: inline-block;
            font-size: 0.75rem;
            font-weight: 700;
            border-radius: 4px;
            padding: 0.1rem 0.45rem;
            margin-right: 0.4rem;
          }
          .ping-status.ok {
            background: #dcfce7;
            color: #15803d;
          }
          .ping-status.auth {
            background: #fef3c7;
            color: #b45309;
          }
          .ping-status.error {
            background: #fee2e2;
            color: #b91c1c;
          }
          .ping-status.offline {
            background: #e2e8f0;
            color: #475569;
          }
          .ping-latency {
            font-size: 0.75rem;
            color: #64748b;
            font-family: Menlo, Consolas, monospace;
          }
        `}
      </style>
    </span>
  );
};

const EndpointInventory = ({ endpoints }: EndpointInventoryProps) => {
  const [pings, setPings] = React.useState<PingState>({});
  const [isPinging, setIsPinging] = React.useState(false);

  const runPings = React.useCallback(async () => {
    const pingable = endpoints.filter(isPingable);
    if (pingable.length === 0) return;
    setIsPinging(true);
    const pending: PingState = {};
    pingable.forEach((endpoint) => {
      pending[endpointKey(endpoint)] = "pending";
    });
    setPings(pending);
    const results = await Promise.all(
      pingable.map((endpoint) => PlatformAPI.ping(endpoint.path))
    );
    const resolved: PingState = {};
    pingable.forEach((endpoint, index) => {
      resolved[endpointKey(endpoint)] = results[index];
    });
    setPings(resolved);
    setIsPinging(false);
  }, [endpoints]);

  React.useEffect(() => {
    runPings();
  }, [runPings]);

  const groups = groupByController(endpoints);

  return (
    <section className="endpoint-section">
      <div className="section-heading">
        <h2>Endpoint inventory</h2>
        <button
          type="button"
          className="btn btn-sm btn-outline-primary"
          onClick={runPings}
          disabled={isPinging}
        >
          {isPinging ? "Pinging…" : "Re-ping"}
        </button>
      </div>
      <div className="controller-grid">
        {groups.map((group) => (
          <div className="controller-card" key={group.controller}>
            <div className="controller-header">
              <span className="controller-name">{group.controller}</span>
              <span className="controller-count">
                {group.endpoints.length}
              </span>
            </div>
            <ul className="endpoint-list">
              {group.endpoints.map((endpoint) => (
                <li className="endpoint-row" key={endpointKey(endpoint)}>
                  <span
                    className="method-pill"
                    style={{
                      background: methodColors[endpoint.method] || "#95a5a6",
                    }}
                  >
                    {endpoint.method}
                  </span>
                  <code className="endpoint-path">{endpoint.path}</code>
                  <PingCell
                    endpoint={endpoint}
                    ping={pings[endpointKey(endpoint)]}
                  />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <style jsx>
        {`
          .endpoint-section {
            margin-bottom: 3rem;
          }
          .section-heading {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1rem;
          }
          h2 {
            font-size: 1.4rem;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
          }
          .controller-grid {
            display: flex;
            flex-wrap: wrap;
            margin: 0 -0.5rem;
          }
          .controller-card {
            flex: 1 1 calc(50% - 1rem);
            min-width: 280px;
            margin: 0.5rem;
            border-radius: 10px;
            border: 1px solid #e5e7eb;
            background: #fff;
            overflow: hidden;
          }
          .controller-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.6rem 1rem;
            background: #f8fafc;
            border-bottom: 1px solid #e5e7eb;
          }
          .controller-name {
            font-weight: 700;
            color: #1e293b;
            font-family: Menlo, Consolas, monospace;
          }
          .controller-count {
            font-size: 0.75rem;
            font-weight: 700;
            color: #475569;
            background: #e2e8f0;
            border-radius: 999px;
            padding: 0.1rem 0.6rem;
          }
          .endpoint-list {
            list-style: none;
            margin: 0;
            padding: 0;
          }
          .endpoint-row {
            display: flex;
            align-items: center;
            padding: 0.5rem 1rem;
            border-bottom: 1px solid #f1f5f9;
            font-size: 0.9rem;
          }
          .endpoint-row:last-child {
            border-bottom: none;
          }
          .method-pill {
            flex: 0 0 64px;
            text-align: center;
            color: #fff;
            font-size: 0.7rem;
            font-weight: 700;
            letter-spacing: 0.05em;
            border-radius: 4px;
            padding: 0.2rem 0;
            margin-right: 0.75rem;
          }
          .endpoint-path {
            flex: 1 1 auto;
            font-family: Menlo, Consolas, monospace;
            font-size: 0.85rem;
            color: #0f172a;
            background: none;
            padding: 0;
            word-break: break-all;
          }
          @media screen and (max-width: 800px) {
            .controller-card {
              flex-basis: 100%;
            }
          }
        `}
      </style>
    </section>
  );
};

export default EndpointInventory;
