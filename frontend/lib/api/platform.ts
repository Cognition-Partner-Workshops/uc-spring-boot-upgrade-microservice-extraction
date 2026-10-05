import axios from "axios";

import { SERVER_BASE_URL } from "../utils/constant";
import { PlatformInfoResponse } from "../types/platformType";
import platformInfoMock from "./platformMock";

// Set to false (and delete ./platformMock.ts + its import above) once the
// backend serves GET /platform/info.
const USE_MOCK = true;

const PlatformAPI = {
  getInfo: async (): Promise<PlatformInfoResponse> => {
    if (USE_MOCK) {
      return platformInfoMock;
    }
    const { data } = await axios.get<PlatformInfoResponse>(
      `${SERVER_BASE_URL}/platform/info`
    );
    return data;
  },
};

export default PlatformAPI;
