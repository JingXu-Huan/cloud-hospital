<script setup>
import RegistrationDesk from './views/RegistrationDesk.vue'
import DoctorDesk from './views/DoctorDesk.vue'
import CashierDesk from './views/CashierDesk.vue'
import PharmacyDesk from './views/PharmacyDesk.vue'
</script>
<template>
  <el-container>
    <el-header class="app-header"><div class="brand">云医院 <span>门诊业务闭环</span></div><div class="sub">六表蒸馏版 · Spring Boot + Vue</div></el-header>
    <el-main class="app-main"><el-tabs type="border-card"><el-tab-pane label="前台挂号"><RegistrationDesk/></el-tab-pane><el-tab-pane label="医生工作站"><DoctorDesk/></el-tab-pane><el-tab-pane label="收费窗口"><CashierDesk/></el-tab-pane><el-tab-pane label="药房发药"><PharmacyDesk/></el-tab-pane></el-tabs></el-main>
  </el-container>
</template>
<style scoped>
.app-header{height:64px;background:#1d4e89;color:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 36px}.brand{font-size:21px;font-weight:700}.brand span{font-size:14px;font-weight:400;margin-left:10px;opacity:.8}.sub{font-size:13px;opacity:.8}.app-main{max-width:1320px;width:100%;margin:0 auto;padding:28px}
</style>
