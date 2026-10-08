import axios from "axios";

import platformMock from "./platformMock";
import { PlatformInfo } from "../types/platformType";
import { SERVER_BASE_URL } from "../utils/constant";

const USE_MOCK = true;

const PlatformAPI = {
  getInfo: async (): Promise<PlatformInfo> => {
    if (USE_MOCK) {
      return platformMock;
    }
    const response = await axios.get(`${SERVER_BASE_URL}/platform/info`);
    return response.data;
  },
};

export default PlatformAPI;
