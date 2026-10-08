import axios from "axios";

import { PlatformInfo } from "../types/platformType";
import { SERVER_BASE_URL } from "../utils/constant";

const PlatformAPI = {
  getInfo: async (): Promise<PlatformInfo> => {
    const response = await axios.get(`${SERVER_BASE_URL}/platform/info`);
    return response.data;
  },
};

export default PlatformAPI;
