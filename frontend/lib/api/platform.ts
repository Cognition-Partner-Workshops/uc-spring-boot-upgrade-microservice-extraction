import axios from "axios";

import { SERVER_BASE_URL } from "../utils/constant";
import { PlatformInfo } from "../types/platformType";
import platformMock from "./platformMock";

const isMockEnabled = process.env.NEXT_PUBLIC_PLATFORM_MOCK === "true";

export const getPlatformInfo = async (): Promise<PlatformInfo> => {
  if (isMockEnabled) {
    return platformMock;
  }
  const { data } = await axios.get<PlatformInfo>(
    `${SERVER_BASE_URL}/platform/info`
  );
  return data;
};

const PlatformAPI = {
  getPlatformInfo,
};

export default PlatformAPI;
