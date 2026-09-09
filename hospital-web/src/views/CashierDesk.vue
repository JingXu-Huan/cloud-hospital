<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const rows = ref([])
const keyword = ref('')
const status = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const load = async () => { rows.value = await http.get('/prescriptions') }
const manualPay = async row => {
  try {
    await ElMessageBox.confirm(`确认已在院内收取 ${row.patientName} 的 ¥${Number(row.totalAmount).toFixed(2)}？此操作仅适用于尚未注册患者端的患者。`, '人工确认收款', { confirmButtonText: '确认已收款', cancelButtonText: '取消', type: 'warning' })
    await http.post(`/prescriptions/${row.id}/manual-pay`)
    ElMessage.success('已确认院内收款，订单已转入药房配药')
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}
const statusLabel = value => ({ UNPAID: '未支付', PAID: '药房配药中', DISPENSED: '已发药', PICKED_UP: '已取药', CANCELLED: '已作废' }[value] || value)
const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return rows.value.filter(row => (!status.value || row.status === status.value) && (!query || [row.prescriptionNo, row.patientName, row.doctorName].some(value => String(value || '').toLowerCase().includes(query))))
})
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
watch([keyword, status, pageSize], () => { currentPage.value = 1 })
watch(filteredRows, records => { currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(records.length / pageSize.value))) })

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新订单</el-button>
      <span class="hint">已注册患者在本人端支付；未注册患者可由院内确认收款。</span>
    </div>

    <el-card header="全部收费订单">
      <div class="order-filters">
        <el-input v-model="keyword" clearable placeholder="搜索处方号、患者或医生" />
        <el-select v-model="status" clearable placeholder="全部状态">
          <el-option label="未支付" value="UNPAID" />
          <el-option label="药房配药中" value="PAID" />
          <el-option label="已发药" value="DISPENSED" />
          <el-option label="已取药" value="PICKED_UP" />
          <el-option label="已作废" value="CANCELLED" />
        </el-select>
      </div>

      <el-table class="desktop-table" :data="pagedRows">
        <el-table-column prop="prescriptionNo" label="处方号" min-width="180" />
        <el-table-column prop="patientName" label="患者" min-width="110" />
        <el-table-column prop="doctorName" label="开方医生" min-width="120" />
        <el-table-column label="金额" min-width="100"><template #default="scope"><span class="amount">¥{{ Number(scope.row.totalAmount).toFixed(2) }}</span></template></el-table-column>
        <el-table-column prop="prescribedAt" label="发送时间" min-width="180" />
        <el-table-column label="订单状态" min-width="130"><template #default="scope"><el-tag :type="scope.row.status === 'UNPAID' ? 'warning' : scope.row.status === 'CANCELLED' ? 'info' : 'success'">{{ statusLabel(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" min-width="150"><template #default="scope"><el-button v-if="scope.row.status === 'UNPAID' && !scope.row.patientRegistered" type="primary" size="small" @click="manualPay(scope.row)">确认院内收款</el-button><span v-else-if="scope.row.status === 'UNPAID'" class="patient-pay-tip">患者端支付</span><span v-else>—</span></template></el-table-column>
      </el-table>
      <div class="mobile-record-list">
        <article v-for="row in pagedRows" :key="row.id" class="mobile-record">
          <div class="mobile-record-head"><span>{{ row.patientName }}</span><el-tag :type="row.status === 'UNPAID' ? 'warning' : row.status === 'CANCELLED' ? 'info' : 'success'">{{ statusLabel(row.status) }}</el-tag></div>
          <div class="mobile-record-meta"><span>{{ row.prescriptionNo }}</span><span>{{ row.doctorName }} · {{ row.prescribedAt }}</span></div>
          <div class="mobile-record-actions"><span class="amount">¥{{ Number(row.totalAmount).toFixed(2) }}</span><el-button v-if="row.status === 'UNPAID' && !row.patientRegistered" type="primary" size="small" @click="manualPay(row)">确认院内收款</el-button><span v-else-if="row.status === 'UNPAID'" class="patient-pay-tip">请在患者端完成支付</span></div>
        </article>
      </div>
      <el-empty v-if="!filteredRows.length" description="没有匹配的收费订单" />
      <div v-if="filteredRows.length" class="list-footer pagination">
        <span>共 {{ filteredRows.length }} 笔订单</span>
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" :total="filteredRows.length" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.order-filters { display: flex; gap: 12px; margin-bottom: 18px; }
.order-filters .el-input { max-width: 320px; }
.order-filters .el-select { width: 160px; }
.patient-pay-tip { color: var(--muted); font-size: 13px; }

@media (max-width: 700px) {
  .order-filters { align-items: stretch; flex-direction: column; }
  .order-filters .el-input, .order-filters .el-select { max-width: none; width: 100%; }
  .pagination :deep(.el-pagination__sizes) { display: none; }
}
</style>
