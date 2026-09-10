<script setup>
import {computed, onMounted, ref} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import http from "../api/http";

const keyword = ref("");
const patients = ref([]);
const doctors = ref([]);
const selected = ref(null);
const dialog = ref(false);
const currentPage = ref(1);
const pageSize = 5;
const form = ref({
  doctorId: null,
  visitDate: new Date().toISOString().slice(0, 10),
});
const patientForm = ref({
  idCard: "",
  name: "",
  gender: "MALE",
  birthday: "",
  phone: "",
  address: "",
});

const pagedPatients = computed(() =>
  patients.value.slice(
    (currentPage.value - 1) * pageSize,
    currentPage.value * pageSize,
  ),
);
const search = async () => {
  patients.value = await http.get("/patients", {
    params: { keyword: keyword.value },
  });
  currentPage.value = 1;
};
const loadDoctors = async () => {
  doctors.value = await http.get("/doctors", { params: { enabled: true } });
};
const createPatient = async () => {
  const patient = await http.post("/patients", patientForm.value);
  selected.value = patient;
  dialog.value = false;
  patientForm.value = {
    idCard: "",
    name: "",
    gender: "MALE",
    birthday: "",
    phone: "",
    address: "",
  };
  await search();
  ElMessage.success("患者建档成功");
};
const removePatient = async (patient) => {
  try {
    await ElMessageBox.confirm(
      `确认删除患者「${patient.name}」吗？仅无挂号记录且未注册患者端账号的档案可删除。`,
      "删除患者档案",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" },
    );
    await http.delete(`/patients/${patient.id}`);
    if (selected.value?.id === patient.id) selected.value = null;
    await search();
    ElMessage.success("患者档案已删除");
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const register = async () => {
  if (!selected.value || !form.value.doctorId)
    return ElMessage.warning("请选择患者和医生");
  await http.post("/registrations", {
    patientId: selected.value.id,
    doctorId: form.value.doctorId,
    visitDate: form.value.visitDate,
  });
  ElMessage.success("挂号成功，已进入医生待诊队列");
};

onMounted(async () => {
  await Promise.all([search(), loadDoctors()]);
});
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按姓名或患者编号查询"
        clearable
        @keyup.enter="search"
      />
      <el-button @click="search">查询</el-button>
      <el-button type="primary" @click="dialog = true">新建患者档案</el-button>
    </div>

    <div class="grid">
      <el-card header="选择患者">
        <el-table
          class="desktop-table"
          :data="pagedPatients"
          highlight-current-row
          @current-change="(patient) => (selected = patient)"
        >
          <el-table-column prop="patientNo" label="患者编号" width="150" />
          <el-table-column prop="name" label="姓名" />
          <el-table-column label="性别" width="80"
            ><template #default="scope">{{
              scope.row.gender === "MALE" ? "男" : "女"
            }}</template></el-table-column
          >
          <el-table-column prop="phone" label="电话" />
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button type="danger" link @click.stop="removePatient(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="mobile-record-list">
          <button
            v-for="patient in pagedPatients"
            :key="patient.id"
            class="mobile-record patient-option"
            :class="{ selected: selected?.id === patient.id }"
            type="button"
            @click="selected = patient"
          >
            <span class="mobile-record-head"
              ><b>{{ patient.name }}</b
              ><span>{{ patient.patientNo }}</span></span
            >
            <span class="mobile-record-meta"
              ><span>{{ patient.gender === "MALE" ? "男" : "女" }}</span
              ><span>{{ patient.phone || "未留联系电话" }}</span></span
            >
          </button>
        </div>
        <div class="list-footer">
          <span>共 {{ patients.length }} 位患者</span>
          <el-pagination
            v-if="patients.length > pageSize"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="patients.length"
          />
        </div>
        <p class="hint">
          当前选择：{{
            selected ? `${selected.name}（${selected.patientNo}）` : "未选择"
          }}
        </p>
        <el-button v-if="selected" type="danger" plain @click="removePatient(selected)">删除当前患者</el-button>
      </el-card>

      <el-card header="挂号信息">
        <el-form label-width="95px">
          <el-form-item label="就诊医生"
            ><el-select
              v-model="form.doctorId"
              placeholder="请选择启用医生"
              style="width: 100%"
              @focus="loadDoctors"
              ><el-option
                v-for="doctor in doctors"
                :key="doctor.id"
                :value="doctor.id"
                :label="`${doctor.departmentName} · ${doctor.realName}（${doctor.title}）`" /></el-select
          ></el-form-item>
          <el-form-item label="就诊日期"
            ><el-date-picker
              v-model="form.visitDate"
              value-format="YYYY-MM-DD"
              type="date"
              style="width: 100%"
          /></el-form-item>
          <el-form-item
            ><el-button type="primary" size="large" @click="register"
              >确认挂号</el-button
            ></el-form-item
          >
        </el-form>
        <el-alert
          type="info"
          :closable="false"
          title="规则：同一患者当天不能重复挂同一位医生的未取消号源。"
        />
      </el-card>
    </div>

    <el-dialog
      v-model="dialog"
      title="患者建档"
      width="min(520px, calc(100vw - 32px))"
    >
      <el-form :model="patientForm" label-width="90px">
        <el-form-item label="身份证号" required
          ><el-input v-model="patientForm.idCard"
        /></el-form-item>
        <el-form-item label="姓名" required
          ><el-input v-model="patientForm.name"
        /></el-form-item>
        <el-form-item label="性别"
          ><el-radio-group v-model="patientForm.gender"
            ><el-radio value="MALE">男</el-radio
            ><el-radio value="FEMALE">女</el-radio></el-radio-group
          ></el-form-item
        >
        <el-form-item label="出生日期"
          ><el-date-picker
            v-model="patientForm.birthday"
            value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label="电话"
          ><el-input v-model="patientForm.phone"
        /></el-form-item>
        <el-form-item label="地址"
          ><el-input v-model="patientForm.address"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="createPatient"
          >保存档案</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>

<style scoped>
.patient-option {
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  font: inherit;
}
.patient-option.selected {
  margin: 0 -8px;
  padding-right: 8px;
  padding-left: 8px;
  background: #eaf7f5;
}
.patient-option .mobile-record-head span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 400;
}
</style>
