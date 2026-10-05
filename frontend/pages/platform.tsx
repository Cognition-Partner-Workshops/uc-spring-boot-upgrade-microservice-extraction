import Head from "next/head";
import React from "react";
import useSWR from "swr";

import DependencyTable from "../components/platform/DependencyTable";
import EndpointInventory from "../components/platform/EndpointInventory";
import PlatformHero from "../components/platform/PlatformHero";
import SummaryTiles from "../components/platform/SummaryTiles";
import ErrorMessage from "../components/common/ErrorMessage";
import LoadingSpinner from "../components/common/LoadingSpinner";
import PlatformAPI from "../lib/api/platform";
import { PlatformInfoResponse } from "../lib/types/platformType";
import { SERVER_BASE_URL } from "../lib/utils/constant";

const Platform = () => {
  const { data, error, mutate } = useSWR<PlatformInfoResponse>(
    "platform-info",
    PlatformAPI.getInfo
  );

  const handleRetry = React.useCallback(() => mutate(), [mutate]);

  const renderBody = () => {
    if (error) {
      return (
        <div className="container platform-state">
          <ErrorMessage
            message={`Could not reach the Conduit API at ${SERVER_BASE_URL} — is the backend running?`}
          />
          <div className="retry-wrap">
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={handleRetry}
            >
              Retry
            </button>
          </div>
        </div>
      );
    }
    if (!data) {
      return (
        <div className="container platform-state">
          <LoadingSpinner />
        </div>
      );
    }

    const { runtime, baseline, dependencies, endpoints, summary } =
      data.platform;
    const controllerCount = endpoints
      .map((endpoint) => endpoint.controller)
      .filter((controller, index, all) => all.indexOf(controller) === index)
      .length;

    return (
      <>
        <PlatformHero runtime={runtime} baseline={baseline} />
        <div className="container">
          <SummaryTiles summary={summary} controllerCount={controllerCount} />
          <DependencyTable dependencies={dependencies} />
          <EndpointInventory endpoints={endpoints} />
        </div>
      </>
    );
  };

  return (
    <>
      <Head>
        <title>API Platform — conduit</title>
        <meta
          name="description"
          content="Runtime, dependency and endpoint overview of the upgraded Conduit API"
        />
      </Head>
      <div className="platform-page">{renderBody()}</div>
      <style jsx>
        {`
          .platform-page {
            background: #f8fafc;
            min-height: 60vh;
            padding-bottom: 2rem;
          }
          .platform-state {
            padding-top: 2rem;
          }
          .retry-wrap {
            display: flex;
            justify-content: center;
          }
        `}
      </style>
    </>
  );
};

export default Platform;
