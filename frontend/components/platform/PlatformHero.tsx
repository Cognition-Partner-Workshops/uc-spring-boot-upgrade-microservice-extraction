import React from "react";

import { PlatformBaseline, PlatformRuntime } from "../../lib/types/platformType";

interface PlatformHeroProps {
  runtime: PlatformRuntime;
  baseline: PlatformBaseline;
}

const PlatformHero = ({ runtime, baseline }: PlatformHeroProps) => (
  <div className="platform-hero">
    <div className="container">
      <div className="hero-heading">
        <h1 className="logo-font">API Platform</h1>
        <span className="live-label">
          <span className="live-dot" />
          LIVE
        </span>
      </div>
      <p className="hero-subtitle">Conduit API — upgraded runtime</p>
      <div className="badges">
        <div className="badge">
          <div className="badge-label">Java</div>
          <div className="badge-values">
            <span className="badge-from">{baseline.javaVersion}</span>
            <span className="badge-arrow">→</span>
            <span className="badge-to">{runtime.javaVersion}</span>
          </div>
        </div>
        <div className="badge">
          <div className="badge-label">Spring Boot</div>
          <div className="badge-values">
            <span className="badge-from">{baseline.springBootVersion}</span>
            <span className="badge-arrow">→</span>
            <span className="badge-to">{runtime.springBootVersion}</span>
          </div>
        </div>
        <div className="badge">
          <div className="badge-label">Spring Framework</div>
          <div className="badge-values">
            <span className="badge-to">{runtime.springFrameworkVersion}</span>
          </div>
        </div>
      </div>
      <p className="hero-vendor">
        Java vendor: <strong>{runtime.javaVendor}</strong> · values reported
        by the running backend
      </p>
    </div>
    <style jsx>
      {`
        .platform-hero {
          background: linear-gradient(135deg, #0f172a, #1e293b);
          color: #e2e8f0;
          padding: 2.5rem 0 2rem;
          margin-bottom: 2rem;
        }
        .hero-heading {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
        }
        .hero-heading h1 {
          color: #fff;
          font-size: 3rem;
          font-weight: 700;
          margin: 0 1rem 0 0;
          text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
        }
        .live-label {
          display: inline-flex;
          align-items: center;
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: #86efac;
          border: 1px solid rgba(134, 239, 172, 0.4);
          border-radius: 999px;
          padding: 0.2rem 0.7rem;
        }
        .live-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #22c55e;
          margin-right: 0.5rem;
          box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
          animation: livePulse 1.6s infinite;
        }
        @keyframes livePulse {
          0% {
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
          }
          70% {
            box-shadow: 0 0 0 8px rgba(34, 197, 94, 0);
          }
          100% {
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
          }
        }
        .hero-subtitle {
          font-size: 1.3rem;
          color: #94a3b8;
          margin: 0.25rem 0 1.5rem;
        }
        .badges {
          display: flex;
          flex-wrap: wrap;
          margin: 0 -0.5rem;
        }
        .badge {
          flex: 1 1 220px;
          margin: 0.5rem;
          padding: 1rem 1.25rem;
          border-radius: 10px;
          background: rgba(255, 255, 255, 0.06);
          border: 1px solid rgba(255, 255, 255, 0.1);
        }
        .badge-label {
          font-size: 0.8rem;
          text-transform: uppercase;
          letter-spacing: 0.1em;
          color: #94a3b8;
          margin-bottom: 0.4rem;
        }
        .badge-values {
          display: flex;
          align-items: baseline;
          flex-wrap: wrap;
        }
        .badge-from {
          font-size: 1.4rem;
          color: #fb7185;
          text-decoration: line-through;
          margin-right: 0.6rem;
        }
        .badge-arrow {
          color: #64748b;
          font-size: 1.2rem;
          margin-right: 0.6rem;
        }
        .badge-to {
          font-size: 2.4rem;
          font-weight: 700;
          color: #4ade80;
          line-height: 1;
        }
        .hero-vendor {
          margin: 1rem 0 0;
          font-size: 0.9rem;
          color: #94a3b8;
        }
        .hero-vendor strong {
          color: #e2e8f0;
        }
        @media screen and (max-width: 800px) {
          .hero-heading h1 {
            font-size: 2.2rem;
          }
          .badge-to {
            font-size: 1.9rem;
          }
        }
      `}
    </style>
  </div>
);

export default PlatformHero;
