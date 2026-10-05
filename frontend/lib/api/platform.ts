import axios from "axios";

import { SERVER_BASE_URL } from "../utils/constant";
import { PlatformInfoResponse } from "../types/platformType";

const PlatformAPI = {
  getInfo: async (): Promise<PlatformInfoResponse> => {
    const { data } = await axios.get<PlatformInfoResponse>(
      `${SERVER_BASE_URL}/platform/info`
    );
    return data;
  },
};

export default PlatformAPI;
