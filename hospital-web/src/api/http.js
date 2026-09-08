import axios from 'axios'
import { ElMessage } from 'element-plus'
const http = axios.create({ baseURL: '/api/v1', timeout: 10000 })
http.interceptors.response.use(({ data }) => { if (data.code !== 0) { ElMessage.error(data.message || '请求失败'); return Promise.reject(new Error(data.message)) } return data.data }, err => { ElMessage.error(err.response?.data?.message || '服务连接失败'); return Promise.reject(err) })
export default http
