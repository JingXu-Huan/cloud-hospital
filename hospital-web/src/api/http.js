import axios from "axios";
import {ElMessage} from "element-plus";

const persistToken = (headers) => {
  const token = headers?.["x-auth-token"];
  if (!token) return;
  localStorage.setItem("cloud-hospital-token", token);
  const session = JSON.parse(
    localStorage.getItem("cloud-hospital-session") || "null",
  );
  if (session)
    localStorage.setItem(
      "cloud-hospital-session",
      JSON.stringify({ ...session, token }),
    );
};

const http = axios.create({ baseURL: "/api/v1", timeout: 10000 });
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("cloud-hospital-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
http.interceptors.response.use(
  (response) => {
    persistToken(response.headers);
    const { data } = response;
    if (data.code !== 0) {
      ElMessage.error(data.message || "请求失败");
      return Promise.reject(new Error(data.message));
    }
    return data.data;
  },
  (err) => {
    persistToken(err.response?.headers);
    ElMessage.error(err.response?.data?.message || "服务连接失败");
    return Promise.reject(err);
  },
);
export default http;
