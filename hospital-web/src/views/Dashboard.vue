<script setup>
import {onMounted, ref} from "vue";
import {Money, User, UserFilled} from "@element-plus/icons-vue";
import http from "../api/http";

const d = ref({
  patientCount: 0,
  enabledDoctorCount: 0,
  unpaidAmount: 0,
  todayRegistrations: [],
  prescriptions: [],
});
const load = async () => (d.value = await http.get("/dashboard/overview"));
const n = (s) =>
  ({
    WAITING: "待接诊",
    IN_PROGRESS: "接诊中",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    UNPAID: "待收费",
    PAID: "待发药",
    DISPENSED: "已发药",
    PICKED_UP: "已取药",
  })[s] || s;
onMounted(load);
</script>
<template>
  <div class="dashboard">
    <div class="dashboard-head">
      <div>
        <h2>今日运营概览</h2>
        <p>查看门诊服务各环节的实时状态</p>
      </div>
      <el-button type="primary" @click="load">刷新数据</el-button>
    </div>
    <div class="metrics">
      <el-card class="metric"
        ><div class="metric-icon patients">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div>
          <span>患者档案</span><b>{{ d.patientCount }}</b>
        </div></el-card
      ><el-card class="metric"
        ><div class="metric-icon doctors">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <span>在岗医生</span><b>{{ d.enabledDoctorCount }}</b>
        </div></el-card
      ><el-card class="metric"
        ><div class="metric-icon revenue">
          <el-icon><Money /></el-icon>
        </div>
        <div>
          <span>待收费金额</span><b>¥{{ Number(d.unpaidAmount).toFixed(2) }}</b>
        </div></el-card
      >
    </div>
    <div class="grid">
      <el-card header="今日挂号状态"
        ><div v-for="x in d.todayRegistrations" :key="x.status" class="bar">
          <span>{{ n(x.status) }}</span
          ><el-progress
            :percentage="Math.min(100, x.total * 20)"
            :format="() => x.total"
          />
        </div>
        <el-empty
          v-if="!d.todayRegistrations.length"
          :image-size="72"
          description="今日暂无挂号记录" /></el-card
      ><el-card header="处方流转"
        ><div v-for="x in d.prescriptions" :key="x.status" class="bar">
          <span>{{ n(x.status) }}</span
          ><el-progress
            :percentage="Math.min(100, x.total * 20)"
            :format="() => x.total"
          />
        </div>
        <el-empty
          v-if="!d.prescriptions.length"
          :image-size="72"
          description="暂无处方流转记录"
      /></el-card>
    </div>
  </div>
</template>
<style scoped>
.dashboard-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin: 3px 2px 24px;
}
.dashboard-head h2 {
  margin: 0;
  color: #173a5f;
  font-size: 24px;
  letter-spacing: -0.025em;
}
.dashboard-head p {
  margin: 5px 0 0;
  color: #71869b;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.metric :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 19px 22px;
}
.metric-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  font-size: 21px;
}
.patients {
  background: #e7f2fc;
  color: #2375ae;
}
.doctors {
  background: #e7f7f3;
  color: #168b76;
}
.revenue {
  background: #fff2e7;
  color: #c66923;
}
.metric span {
  display: block;
  font-size: 13px;
  color: #71869b;
}
.metric b {
  display: block;
  margin-top: 2px;
  color: #173a5f;
  font-size: 25px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.bar {
  display: grid;
  grid-template-columns: 78px 1fr;
  gap: 14px;
  align-items: center;
  margin: 18px 0;
  color: #58718a;
  font-size: 13px;
}
.bar :deep(.el-progress-bar__outer) {
  background: #edf3f6;
}
@media (max-width: 720px) {
  .dashboard-head {
    align-items: start;
    gap: 14px;
    flex-direction: column;
  }
  .metrics {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .metric :deep(.el-card__body) {
    padding: 16px 18px;
  }
}
</style>
