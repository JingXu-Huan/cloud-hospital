<script setup>
import {computed, onMounted, ref} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import http from "../api/http";

const props = defineProps({
  doctorMode: { type: Boolean, default: false },
  session: { type: Object, default: null },
});
const doctors = ref([]);
const doctorId = ref(null);
const date = ref(new Date().toISOString().slice(0, 10));
const queue = ref([]);
const current = ref(null);
const recordExists = ref(false);
const doctorDialog = ref(false);
const doctorManagementDialog = ref(false);
const doctorManagementRows = ref([]);
const currentPage = ref(1);
const pageSize = 5;
const record = ref({
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  allergyHistory: "",
  physicalExam: "",
  diagnosis: "",
  advice: "",
});
const doctorForm = ref({
  loginName: "",
  realName: "",
  departmentName: "",
  title: "",
});
const rx = ref({
  remark: "",
  items: [
    {
      drugCode: "D001",
      drugName: "对乙酰氨基酚片",
      specification: "0.5g*12片",
      unit: "盒",
      unitPrice: 12.8,
      quantity: 1,
      dosage: "0.5g",
      frequency: "每日3次",
      route: "口服",
    },
  ],
});

const total = computed(() =>
  rx.value.items
    .reduce(
      (sum, item) =>
        sum + Number(item.unitPrice || 0) * Number(item.quantity || 0),
      0,
    )
    .toFixed(2),
);
const pagedQueue = computed(() =>
  queue.value.slice(
    (currentPage.value - 1) * pageSize,
    currentPage.value * pageSize,
  ),
);
// 医生账户只处理自身的待诊队列；会话角色作为兜底，避免属性传递异常时退回管理员模式。
const doctorMode = computed(
  () => props.doctorMode || props.session?.role === "DOCTOR",
);
const statusLabel = (status) =>
  ({
    WAITING: "待接诊",
    IN_PROGRESS: "接诊中",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
  })[status] || status;
const loadDoctors = async () => {
  doctors.value = await http.get("/doctors", { params: { enabled: true } });
  doctorId.value ??= doctors.value[0]?.id;
};
const loadQueue = async () => {
  if (doctorMode.value)
    queue.value = await http.get("/doctor-workstation/queue", {
      params: { visitDate: date.value },
    });
  else {
    if (!doctorId.value) return;
    queue.value = await http.get(`/doctors/${doctorId.value}/registrations`, {
      params: { visitDate: date.value },
    });
  }
  currentPage.value = 1;
};
const createDoctor = async () => {
  const doctor = await http.post("/doctors", doctorForm.value);
  doctorDialog.value = false;
  doctorForm.value = {
    loginName: "",
    realName: "",
    departmentName: "",
    title: "",
  };
  await loadDoctors();
  doctorId.value = doctor.id;
  ElMessage.success("医生建档成功，初始密码为 123456，可交由医生登录后使用");
};
const openDoctorManagement = async () => {
  doctorManagementRows.value = await http.get("/doctors");
  doctorManagementDialog.value = true;
};
const removeDoctor = async (doctor) => {
  try {
    await ElMessageBox.confirm(
      `确认删除医生「${doctor.realName}」吗？仅无挂号记录的医生可删除，登录账号也会一并移除。`,
      "删除医生档案",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" },
    );
    await http.delete(`/doctors/${doctor.id}`);
    if (doctorId.value === doctor.id) doctorId.value = null;
    doctorManagementRows.value = doctorManagementRows.value.filter((row) => row.id !== doctor.id);
    await loadDoctors();
    ElMessage.success("医生档案及登录账号已删除");
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const removeRegistration = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除 ${row.patientName} 的挂号吗？仅待接诊且尚未产生诊疗数据的挂号可删除。`,
      "删除挂号",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" },
    );
    await http.delete(`/registrations/${row.id}`);
    await loadQueue();
    ElMessage.success("挂号已删除");
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const start = async (row) => {
  await http.post(
    doctorMode.value
      ? `/doctor-workstation/registrations/${row.id}/start`
      : `/registrations/${row.id}/start`,
  );
  ElMessage.success("已开始接诊");
  await loadQueue();
  current.value = queue.value.find((item) => item.id === row.id);
  record.value = {
    chiefComplaint: "",
    presentIllness: "",
    pastHistory: "",
    allergyHistory: "",
    physicalExam: "",
    diagnosis: "",
    advice: "",
  };
  recordExists.value = false;
};
const select = async (row) => {
  if (row.status !== "IN_PROGRESS") return ElMessage.info("请先开始接诊");
  current.value = row;
  const recordUrl = doctorMode.value
    ? `/doctor-workstation/registrations/${row.id}/medical-record`
    : `/registrations/${row.id}/medical-record`;
  const saved = await http.get(recordUrl);
  recordExists.value = Boolean(saved);
  record.value = saved || {
    chiefComplaint: "",
    presentIllness: "",
    pastHistory: "",
    allergyHistory: "",
    physicalExam: "",
    diagnosis: "",
    advice: "",
  };
};
const saveRecord = async () => {
  if (!current.value) return;
  const recordUrl = doctorMode.value
    ? `/doctor-workstation/registrations/${current.value.id}/medical-record`
    : `/registrations/${current.value.id}/medical-record`;
  await http.put(recordUrl, record.value);
  recordExists.value = true;
  ElMessage.success("病历已保存");
};
const removeRecord = async () => {
  if (!current.value) return;
  try {
    await ElMessageBox.confirm(
      "确认删除当前病历吗？删除后不可恢复。",
      "删除病历",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" },
    );
    const recordUrl = doctorMode.value
      ? `/doctor-workstation/registrations/${current.value.id}/medical-record`
      : `/registrations/${current.value.id}/medical-record`;
    await http.delete(recordUrl);
    record.value = {
      chiefComplaint: "",
      presentIllness: "",
      pastHistory: "",
      allergyHistory: "",
      physicalExam: "",
      diagnosis: "",
      advice: "",
    };
    recordExists.value = false;
    ElMessage.success("病历已删除");
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const addItem = () =>
  rx.value.items.push({
    drugCode: "",
    drugName: "",
    specification: "",
    unit: "盒",
    unitPrice: 0,
    quantity: 1,
    dosage: "",
    frequency: "",
    route: "口服",
  });
const createRx = async () => {
  if (!current.value) return;
  if (!rx.value.items.length)
    return ElMessage.warning("请至少添加一种药品后再提交");
  if (
    rx.value.items.some(
      (item) =>
        !item.drugName?.trim() ||
        !item.unit?.trim() ||
        Number(item.unitPrice) < 0 ||
        Number(item.quantity) < 1,
    )
  )
    return ElMessage.warning("请完善每项药品的名称、单位、单价和数量");
  const prescriptionUrl = doctorMode.value
    ? `/doctor-workstation/registrations/${current.value.id}/prescriptions`
    : `/registrations/${current.value.id}/prescriptions`;
  await http.post(prescriptionUrl, rx.value);
  ElMessage.success(`处方已提交，总金额 ¥${total.value}；本次接诊已结束`);
  rx.value = { remark: "", items: [] };
  current.value = null;
  await loadQueue();
};

onMounted(async () => {
  if (doctorMode.value) await loadQueue();
  else await loadDoctors();
});
</script>

<template>
  <div class="page">
    <section v-if="doctorMode" class="doctor-welcome">
      <div>
        <h2>{{ props.session?.doctorName || props.session?.username }} 医生</h2>
        <p>
          {{ props.session?.departmentName || "门诊" }} ·
          {{
            props.session?.doctorTitle || "医生"
          }}，以下仅展示您负责的患者队列。
        </p>
      </div>
      <div class="doctor-welcome-actions">
        <el-date-picker
          v-model="date"
          value-format="YYYY-MM-DD"
          type="date"
          aria-label="接诊日期"
        />
        <el-button type="primary" @click="loadQueue">刷新队列</el-button>
      </div>
    </section>
    <div v-else class="toolbar">
      <el-select v-model="doctorId" placeholder="医生" style="width: 240px"
        ><el-option
          v-for="doctor in doctors"
          :key="doctor.id"
          :value="doctor.id"
          :label="`${doctor.departmentName} · ${doctor.realName}`"
      /></el-select>
      <el-date-picker v-model="date" value-format="YYYY-MM-DD" type="date" />
      <el-button type="primary" @click="loadQueue">查询待诊队列</el-button>
      <el-button @click="doctorDialog = true">新增医生</el-button>
      <el-button @click="openDoctorManagement">管理医生</el-button>
    </div>

    <div class="grid doctor-layout">
      <el-card header="今日待诊">
        <el-table class="desktop-table" :data="pagedQueue" @row-click="select">
          <el-table-column prop="visitNo" label="流水号" width="150" />
          <el-table-column prop="patientName" label="患者" />
          <el-table-column prop="status" label="状态" width="110"
            ><template #default="scope"
              ><el-tag
                :type="scope.row.status === 'WAITING' ? 'warning' : 'primary'"
                >{{ statusLabel(scope.row.status) }}</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column label="操作" width="150"
            ><template #default="scope"
              ><el-button
                v-if="scope.row.status === 'WAITING'"
                type="primary"
                link
                @click.stop="start(scope.row)"
                >开始接诊</el-button
              ><el-button
                v-if="!doctorMode && scope.row.status === 'WAITING'"
                type="danger"
                link
                @click.stop="removeRegistration(scope.row)"
                >删除</el-button
              ><el-button v-if="scope.row.status !== 'WAITING'" link @click.stop="select(scope.row)"
                >继续</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <div class="mobile-record-list">
          <article
            v-for="row in pagedQueue"
            :key="row.id"
            class="mobile-record"
          >
            <div class="mobile-record-head">
              <span>{{ row.patientName }}</span
              ><el-tag
                :type="row.status === 'WAITING' ? 'warning' : 'primary'"
                >{{ statusLabel(row.status) }}</el-tag
              >
            </div>
            <div class="mobile-record-meta">
              <span>流水号：{{ row.visitNo }}</span>
            </div>
            <div class="mobile-record-actions">
              <el-button
                v-if="row.status === 'WAITING'"
                type="primary"
                size="small"
                @click="start(row)"
                >开始接诊</el-button
              ><el-button
                v-if="!doctorMode && row.status === 'WAITING'"
                type="danger"
                size="small"
                @click="removeRegistration(row)"
                >删除</el-button
              ><el-button v-if="row.status !== 'WAITING'" size="small" @click="select(row)"
                >继续接诊</el-button
              >
            </div>
          </article>
        </div>
        <div class="list-footer">
          <span>共 {{ queue.length }} 位待诊患者</span>
          <el-pagination
            v-if="queue.length > pageSize"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="queue.length"
          />
        </div>
        <p class="hint">队列只包含待接诊和接诊中的患者，按挂号时间排序。</p>
      </el-card>

      <div class="doctor-workspace">
        <template v-if="current">
          <el-card>
            <template #header
              ><div class="workspace-header">
                <span>门诊病历 · {{ current.patientName }}</span
                ><el-tag>{{ statusLabel(current.status) }}</el-tag>
              </div></template
            >
            <el-form :model="record" label-position="top">
              <el-form-item label="主诉"
                ><el-input v-model="record.chiefComplaint"
              /></el-form-item>
              <el-form-item label="现病史"
                ><el-input
                  v-model="record.presentIllness"
                  type="textarea"
                  :rows="2"
              /></el-form-item>
              <el-form-item label="既往史 / 过敏史"
                ><div class="medical-history-fields">
                  <el-input
                    v-model="record.pastHistory"
                    placeholder="既往史"
                  /><el-input
                    v-model="record.allergyHistory"
                    placeholder="过敏史"
                  /></div
              ></el-form-item>
              <el-form-item label="体格检查"
                ><el-input v-model="record.physicalExam"
              /></el-form-item>
              <el-form-item label="初步诊断" required
                ><el-input v-model="record.diagnosis"
              /></el-form-item>
              <el-form-item label="医嘱"
                ><el-input v-model="record.advice" type="textarea" :rows="2"
              /></el-form-item>
              <el-button type="primary" @click="saveRecord">保存病历</el-button>
              <el-button v-if="recordExists" type="danger" plain @click="removeRecord">删除病历</el-button>
            </el-form>
          </el-card>

          <el-card class="prescription-card">
            <template #header>开具处方</template>
            <el-table class="desktop-table" :data="rx.items" size="small">
              <el-table-column label="药品" min-width="130"
                ><template #default="scope"
                  ><el-input v-model="scope.row.drugName" /></template
              ></el-table-column>
              <el-table-column label="规格" width="120"
                ><template #default="scope"
                  ><el-input v-model="scope.row.specification" /></template
              ></el-table-column>
              <el-table-column label="单价" width="105"
                ><template #default="scope"
                  ><el-input-number
                    v-model="scope.row.unitPrice"
                    :min="0"
                    :precision="2"
                    controls-position="right" /></template
              ></el-table-column>
              <el-table-column label="数量" width="105"
                ><template #default="scope"
                  ><el-input-number
                    v-model="scope.row.quantity"
                    :min="1"
                    controls-position="right" /></template
              ></el-table-column>
              <el-table-column label="用法" width="120"
                ><template #default="scope"
                  ><el-input v-model="scope.row.route" /></template
              ></el-table-column>
              <el-table-column width="60"
                ><template #default="scope"
                  ><el-button
                    type="danger"
                    link
                    @click="rx.items.splice(scope.$index, 1)"
                    >删除</el-button
                  ></template
                ></el-table-column
              >
            </el-table>
            <div class="prescription-editor-mobile">
              <div
                v-for="(item, index) in rx.items"
                :key="index"
                class="medicine-item"
              >
                <div class="medicine-item-head">
                  <b>药品 {{ index + 1 }}</b
                  ><el-button
                    type="danger"
                    link
                    @click="rx.items.splice(index, 1)"
                    >删除</el-button
                  >
                </div>
                <label>药品名称<el-input v-model="item.drugName" /></label>
                <label>规格<el-input v-model="item.specification" /></label>
                <div class="medicine-item-numbers">
                  <label
                    >单价<el-input-number
                      v-model="item.unitPrice"
                      :min="0"
                      :precision="2"
                      controls-position="right" /></label
                  ><label
                    >数量<el-input-number
                      v-model="item.quantity"
                      :min="1"
                      controls-position="right"
                  /></label>
                </div>
                <label>用法<el-input v-model="item.route" /></label>
              </div>
            </div>
            <div class="prescription-actions">
              <el-button @click="addItem">添加药品</el-button>
              <el-input v-model="rx.remark" placeholder="处方备注" />
              <span class="amount">合计 ¥{{ total }}</span>
              <el-button type="primary" @click="createRx">提交处方</el-button>
            </div>
          </el-card>
        </template>
        <el-empty
          v-else
          class="doctor-workspace-empty"
          description="选择一条接诊中的挂号，开始填写病历与处方"
        />
      </div>
    </div>

    <el-dialog
      v-model="doctorDialog"
      title="医生建档"
      width="min(520px, calc(100vw - 32px))"
    >
      <el-form :model="doctorForm" label-width="90px">
        <el-form-item label="登录名" required
          ><el-input
            v-model="doctorForm.loginName"
            placeholder="用于系统登录，创建后不可重复"
        /></el-form-item>
        <el-form-item label="姓名" required
          ><el-input v-model="doctorForm.realName"
        /></el-form-item>
        <el-form-item label="科室" required
          ><el-input
            v-model="doctorForm.departmentName"
            placeholder="例如：内科"
        /></el-form-item>
        <el-form-item label="职称" required
          ><el-input v-model="doctorForm.title" placeholder="例如：主治医师"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="doctorDialog = false">取消</el-button
        ><el-button type="primary" @click="createDoctor"
          >保存并启用</el-button
        ></template
      >
    </el-dialog>
    <el-dialog
      v-model="doctorManagementDialog"
      title="医生管理"
      width="min(720px, calc(100vw - 32px))"
    >
      <el-table class="desktop-table" :data="doctorManagementRows">
        <el-table-column prop="doctorNo" label="医生编号" min-width="150" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="departmentName" label="科室" min-width="120" />
        <el-table-column prop="title" label="职称" min-width="120" />
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button type="danger" link @click="removeDoctor(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mobile-record-list">
        <article v-for="doctor in doctorManagementRows" :key="doctor.id" class="mobile-record">
          <div class="mobile-record-head"><b>{{ doctor.realName }}</b><span>{{ doctor.doctorNo }}</span></div>
          <div class="mobile-record-meta"><span>{{ doctor.departmentName }}</span><span>{{ doctor.title }}</span></div>
          <div class="mobile-record-actions"><el-button type="danger" size="small" @click="removeDoctor(doctor)">删除</el-button></div>
        </article>
      </div>
      <el-empty v-if="!doctorManagementRows.length" description="暂无医生档案" />
    </el-dialog>
  </div>
</template>

<style scoped>
.doctor-layout {
  grid-template-columns: minmax(360px, 0.75fr) minmax(500px, 1.25fr);
}
.doctor-welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 20px 24px;
  border-radius: var(--radius);
  background: #e5f4f1;
  color: #173a5f;
}
.doctor-welcome h2 {
  margin: 0;
  font-size: 22px;
  letter-spacing: -0.02em;
}
.doctor-welcome p {
  margin: 6px 0 0;
  color: #527187;
  font-size: 14px;
}
.doctor-welcome-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}
.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.medical-history-fields {
  display: flex;
  width: 100%;
  gap: 8px;
}
.prescription-card {
  margin-top: 16px;
}
.prescription-editor-mobile {
  display: none;
}
.prescription-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}
.prescription-actions .el-input {
  flex: 1 1 220px;
}
.doctor-workspace-empty {
  min-height: 280px;
  background: rgba(255, 255, 255, 0.58);
  border-radius: var(--radius);
}

@media (max-width: 900px) {
  .doctor-layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 700px) {
  .doctor-welcome {
    align-items: stretch;
    flex-direction: column;
    padding: 18px;
  }
  .doctor-welcome-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
  .doctor-welcome-actions :deep(.el-date-editor),
  .doctor-welcome-actions .el-button {
    width: 100%;
    margin: 0;
  }
  .medical-history-fields {
    flex-direction: column;
  }
  .prescription-editor-mobile {
    display: grid;
    gap: 16px;
  }
  .medicine-item {
    display: grid;
    gap: 10px;
    padding: 16px 0;
    border-bottom: 1px solid var(--line);
  }
  .medicine-item:first-child {
    padding-top: 0;
  }
  .medicine-item:last-child {
    padding-bottom: 0;
    border-bottom: 0;
  }
  .medicine-item label {
    display: grid;
    gap: 5px;
    color: var(--muted);
    font-size: 13px;
  }
  .medicine-item-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #173a5f;
  }
  .medicine-item-numbers {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .medicine-item :deep(.el-input-number) {
    width: 100%;
  }
  .prescription-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .prescription-actions .el-input {
    width: 100%;
  }
  .prescription-actions .el-button {
    width: 100%;
    margin: 0;
  }
}
</style>
