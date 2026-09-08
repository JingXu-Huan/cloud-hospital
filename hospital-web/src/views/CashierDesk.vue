<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import http from '../api/http'
const rows=ref([])
const load=async()=>rows.value=await http.get('/prescriptions',{params:{status:'UNPAID'}})
const pay=async row=>{try{await ElMessageBox.confirm(`确认收取 ${row.patientName} 的 ¥${row.totalAmount}？`,'模拟收费',{confirmButtonText:'微信支付',cancelButtonText:'取消',type:'warning'});await http.post(`/prescriptions/${row.id}/pay`,{paymentMethod:'WECHAT'});ElMessage.success('收费成功');load()}catch(e){if(e==='cancel'||e==='close')return}}
onMounted(load)
</script>
<template><div class="page"><div class="toolbar"><el-button type="primary" @click="load">刷新待缴费处方</el-button><span class="hint">支付为教学模拟，不对接第三方支付。</span></div><el-card header="待缴费处方"><el-table :data="rows"><el-table-column prop="prescriptionNo" label="处方号"/><el-table-column prop="patientName" label="患者"/><el-table-column prop="doctorName" label="开方医生"/><el-table-column label="金额"><template #default="s"><span class="amount">¥{{s.row.totalAmount}}</span></template></el-table-column><el-table-column prop="prescribedAt" label="开方时间"/><el-table-column label="操作" width="110"><template #default="s"><el-button type="primary" @click="pay(s.row)">收费</el-button></template></el-table-column></el-table><el-empty v-if="!rows.length" description="暂无待缴费处方"/></el-card></div></template>
