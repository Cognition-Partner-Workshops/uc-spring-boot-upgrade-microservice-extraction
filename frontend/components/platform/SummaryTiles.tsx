import React from "react";

import { PlatformSummary } from "../../lib/types/platformType";

interface SummaryTilesProps {
  summary: PlatformSummary;
  controllerCount: number;
}

const SummaryTiles = ({ summary, controllerCount }: SummaryTilesProps) => (
  <div className="summary-tiles">
    <div className="tile">
      <div className="tile-value">{summary.endpointCount}</div>
      <div className="tile-label">REST endpoints</div>
    </div>
    <div className="tile">
      <div className="tile-value">{summary.upgradedDependencyCount}</div>
      <div className="tile-label">Dependencies upgraded</div>
    </div>
    <div className="tile">
      <div className="tile-value">{controllerCount}</div>
      <div className="tile-label">Controllers</div>
    </div>
    <style jsx>
      {`
        .summary-tiles {
          display: flex;
          flex-wrap: wrap;
          margin: 0 -0.5rem 2rem;
        }
        .tile {
          flex: 1 1 180px;
          margin: 0.5rem;
          padding: 1.25rem;
          text-align: center;
          border-radius: 10px;
          background: #fff;
          border: 1px solid #e5e7eb;
          box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
        }
        .tile-value {
          font-size: 2.4rem;
          font-weight: 700;
          color: #0f172a;
          line-height: 1.1;
        }
        .tile-label {
          margin-top: 0.3rem;
          font-size: 0.85rem;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          color: #64748b;
        }
      `}
    </style>
  </div>
);

export default SummaryTiles;
