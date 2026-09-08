<script setup>
import { ref,onMounted } from 'vue'
import { ElMessageBox,ElMessage } from 'element-plus'
import http from '../api/http'
const rows=ref([])
const load=async()=>rows.value=await http.get('/prescriptions',{params:{status:'PAID'}})
const detail=async row=>{const d=await http.get(`/prescriptions/${row.id}`);await ElMessageBox.alert(d.items.map(i=>`${i.drugName} × ${i.quantity}${i.unit}`).join('<br>'),'处方明细',{dangerouslyUseHTMLString:true})}
const dispense=async row=>{try{await ElMessageBox.confirm(`确认向 ${row.patientName} 发药？`,'发药核对',{type:'warning'});await http.post(`/prescriptions/${row.id}/dispense`);ElMessage.success('发药成功，已通知患者到药房取药');load()}catch(e){if(e==='cancel'||e==='close')return}}
onMounted(load)
</script>
<template><div class="page"><div class="toolbar"><el-button type="primary" @click="load">刷新已缴费处方</el-button><span class="hint">未缴费处方不会出现在此列表中。</span></div><el-card header="待发药处方"><el-table :data="rows"><el-table-column prop="prescriptionNo" label="处方号"/><el-table-column prop="patientName" label="患者"/><el-table-column prop="doctorName" label="医生"/><el-table-column label="金额"><template #default="s"><span class="amount">¥{{s.row.totalAmount}}</span></template></el-table-column><el-table-column label="操作" width="190"><template #default="s"><el-button @click="detail(s.row)">查看药品</el-button><el-button type="success" @click="dispense(s.row)">确认发药</el-button></template></el-table-column></el-table><el-empty v-if="!rows.length" description="暂无已缴费待发药处方"/></el-card></div></template>
