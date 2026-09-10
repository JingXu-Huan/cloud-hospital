<script setup>
import {computed, onMounted, ref} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import http from "../api/http";

const rows = ref([]);
const currentPage = ref(1);
const pageSize = 8;
const pagedRows = computed(() =>
  rows.value.slice(
    (currentPage.value - 1) * pageSize,
    currentPage.value * pageSize,
  ),
);
const load = async () => {
  rows.value = await http.get("/prescriptions", { params: { status: "PAID" } });
  currentPage.value = Math.min(
    currentPage.value,
    Math.max(1, Math.ceil(rows.value.length / pageSize)),
  );
};
const detail = async (row) => {
  const prescription = await http.get(`/prescriptions/${row.id}`);
  await ElMessageBox.alert(
    prescription.items
      .map((item) => `${item.drugName} × ${item.quantity}${item.unit}`)
      .join("<br>"),
    "处方明细",
    { dangerouslyUseHTMLString: true },
  );
};
const dispense = async (row) => {
  try {
    await ElMessageBox.confirm(`确认向 ${row.patientName} 发药？`, "发药核对", {
      type: "warning",
    });
    await http.post(`/prescriptions/${row.id}/dispense`);
    ElMessage.success("发药成功，已通知患者到药房取药");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};

onMounted(load);
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新已缴费处方</el-button>
      <span class="hint">未缴费处方不会出现在此列表中。</span>
    </div>

    <el-card header="待发药处方">
      <el-table class="desktop-table" :data="pagedRows">
        <el-table-column prop="prescriptionNo" label="处方号" min-width="180" />
        <el-table-column prop="patientName" label="患者" min-width="110" />
        <el-table-column prop="doctorName" label="医生" min-width="120" />
        <el-table-column label="金额" min-width="110"
          ><template #default="scope"
            ><span class="amount"
              >¥{{ Number(scope.row.totalAmount).toFixed(2) }}</span
            ></template
          ></el-table-column
        >
        <el-table-column label="操作" width="240"
          ><template #default="scope"
            ><div class="prescription-actions">
              <el-button @click="detail(scope.row)">查看药品</el-button
              ><el-button type="success" @click="dispense(scope.row)"
                >确认发药</el-button
              >
            </div></template
          ></el-table-column
        >
      </el-table>
      <div class="mobile-record-list">
        <article v-for="row in pagedRows" :key="row.id" class="mobile-record">
          <div class="mobile-record-head">
            <span>{{ row.patientName }}</span
            ><span class="amount"
              >¥{{ Number(row.totalAmount).toFixed(2) }}</span
            >
          </div>
          <div class="mobile-record-meta">
            <span>{{ row.prescriptionNo }}</span
            ><span>开方医生：{{ row.doctorName }}</span>
          </div>
          <div class="mobile-record-actions">
            <el-button size="small" @click="detail(row)">查看药品</el-button
            ><el-button type="success" size="small" @click="dispense(row)"
              >确认发药</el-button
            >
          </div>
        </article>
      </div>
      <el-empty v-if="!rows.length" description="暂无已缴费待发药处方" />
      <div v-if="rows.length" class="list-footer">
        <span>共 {{ rows.length }} 张待发药处方</span>
        <el-pagination
          v-if="rows.length > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="rows.length"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.prescription-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.prescription-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
