import axios from "axios";

import { SERVER_BASE_URL } from "../utils/constant";
import { PingResult, PlatformInfoResponse } from "../types/platformType";
import { USE_PLATFORM_MOCK, getMockPlatformInfo } from "./platformMock";

const PlatformAPI = {
  getInfo: async (): Promise<PlatformInfoResponse> => {
    if (USE_PLATFORM_MOCK) return getMockPlatformInfo();
    const response = await axios.get<PlatformInfoResponse>(
      `${SERVER_BASE_URL}/platform/info`
    );
    return response.data;
  },
  ping: async (path: string): Promise<PingResult> => {
    const startedAt = Date.now();
    try {
      const response = await axios.get(`${SERVER_BASE_URL}${path}`, {
        validateStatus: () => true,
      });
      return { status: response.status, latencyMs: Date.now() - startedAt };
    } catch (error) {
      return { status: 0, latencyMs: Date.now() - startedAt };
    }
  },
};

export default PlatformAPI;
