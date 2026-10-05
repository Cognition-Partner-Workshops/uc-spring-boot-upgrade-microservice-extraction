import React from "react";

import { DependencyUpgrade } from "../../lib/types/platformType";

interface DependencyTableProps {
  dependencies: DependencyUpgrade[];
}

const DependencyTable = ({ dependencies }: DependencyTableProps) => (
  <section className="dependency-section">
    <h2>Dependency upgrades</h2>
    <div className="table-wrap">
      <table className="dependency-table">
        <thead>
          <tr>
            <th>Dependency</th>
            <th>Before</th>
            <th className="arrow-col" />
            <th>After</th>
          </tr>
        </thead>
        <tbody>
          {dependencies.map((dependency) => (
            <tr key={dependency.name}>
              <td className="dep-name">{dependency.name}</td>
              <td>
                <span className="version from">{dependency.from}</span>
              </td>
              <td className="arrow-col">→</td>
              <td>
                <span className="version to">{dependency.to}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
    <style jsx>
      {`
        .dependency-section {
          margin-bottom: 2.5rem;
        }
        h2 {
          font-size: 1.4rem;
          font-weight: 700;
          color: #0f172a;
          margin-bottom: 1rem;
        }
        .table-wrap {
          overflow-x: auto;
          border-radius: 10px;
          border: 1px solid #e5e7eb;
          background: #fff;
        }
        .dependency-table {
          width: 100%;
          border-collapse: collapse;
          margin: 0;
        }
        th,
        td {
          padding: 0.7rem 1rem;
          text-align: left;
          border-bottom: 1px solid #f1f5f9;
          white-space: nowrap;
        }
        th {
          font-size: 0.75rem;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          color: #64748b;
          background: #f8fafc;
        }
        tbody tr:last-child td {
          border-bottom: none;
        }
        .dep-name {
          width: 50%;
          font-weight: 600;
          color: #1e293b;
        }
        .arrow-col {
          width: 2rem;
          text-align: center;
          color: #22c55e;
          font-weight: 700;
        }
        .version {
          display: inline-block;
          padding: 0.2rem 0.6rem;
          border-radius: 6px;
          font-family: Menlo, Consolas, monospace;
          font-size: 0.9rem;
        }
        .version.from {
          background: #fee2e2;
          color: #b91c1c;
        }
        .version.to {
          background: #dcfce7;
          color: #15803d;
          font-weight: 600;
        }
      `}
    </style>
  </section>
);

export default DependencyTable;
