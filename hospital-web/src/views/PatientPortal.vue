<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import http from "../api/http";

const data = ref(null);
const doctors = ref([]);
const form = ref({
  doctorId: null,
  visitDate: new Date().toISOString().slice(0, 10),
});
const confirm = ref(false);
const preview = ref(null);
const prescriptionDetail = ref(null);
const detailVisible = ref(false);
const registrationPage = ref(1);
const prescriptionPage = ref(1);
const notificationPage = ref(1);
const pageSize = 5;
const notificationPageSize = 3;

const load = async () => {
  data.value = await http.get("/patient-portal/overview");
  doctors.value = await http.get("/patient-portal/doctors");
};
const doctor = computed(() =>
  doctors.value.find((item) => item.id === form.value.doctorId),
);
const statusLabel = (status) =>
  ({
    WAITING: "待接诊",
    IN_PROGRESS: "接诊中",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    UNPAID: "待缴费",
    PAID: "药房配药中",
    DISPENSED: "可取药",
    PICKED_UP: "已取药",
  })[status] || status;
const gender = (value) => (value === "MALE" ? "男" : "女");
const registrations = computed(() => data.value?.registrations || []);
const prescriptions = computed(() => data.value?.prescriptions || []);
const notifications = computed(() => data.value?.notifications || []);
const unreadNotificationCount = computed(
  () => notifications.value.filter((item) => !item.readAt).length,
);
const registrationRows = computed(() =>
  registrations.value.slice(
    (registrationPage.value - 1) * pageSize,
    registrationPage.value * pageSize,
  ),
);
const prescriptionRows = computed(() =>
  prescriptions.value.slice(
    (prescriptionPage.value - 1) * pageSize,
    prescriptionPage.value * pageSize,
  ),
);
const notificationRows = computed(() =>
  notifications.value.slice(
    (notificationPage.value - 1) * notificationPageSize,
    notificationPage.value * notificationPageSize,
  ),
);
const clampPage = (page, total, size) =>
  Math.min(page.value, Math.max(1, Math.ceil(total / size)));
watch(registrations, (rows) => {
  registrationPage.value = clampPage(registrationPage, rows.length, pageSize);
});
watch(prescriptions, (rows) => {
  prescriptionPage.value = clampPage(prescriptionPage, rows.length, pageSize);
});
watch(notifications, (rows) => {
  notificationPage.value = clampPage(
    notificationPage,
    rows.length,
    notificationPageSize,
  );
});
const prepare = async () => {
  if (!form.value.doctorId) return ElMessage.warning("请先选择医生");
  preview.value = await http.get("/patient-portal/queue-preview", {
    params: form.value,
  });
  confirm.value = true;
};
const register = async () => {
  await http.post("/patient-portal/registrations", form.value);
  confirm.value = false;
  ElMessage.success("挂号成功，已加入候诊队列");
  await load();
};
const cancelRegistration = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认取消 ${row.visitDate} ${row.departmentName} 的挂号吗？取消后需重新挂号。`,
      "取消挂号",
      {
        confirmButtonText: "确认取消",
        cancelButtonText: "暂不取消",
        type: "warning",
      },
    );
    await http.post(`/patient-portal/registrations/${row.id}/cancel`, {
      reason: "患者自助取消",
    });
    ElMessage.success("挂号已取消");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const pay = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认支付收费订单 ${row.prescriptionNo}，金额 ¥${Number(row.totalAmount).toFixed(2)}？`,
      "确认支付",
      {
        confirmButtonText: "确认支付",
        cancelButtonText: "暂不支付",
        type: "warning",
      },
    );
    await http.post(`/patient-portal/prescriptions/${row.id}/pay`);
    ElMessage.success("支付成功，药房将为您配药");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const pickUp = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认已领取处方 ${row.prescriptionNo} 的药品？`,
      "确认取药",
      {
        confirmButtonText: "确认已取药",
        cancelButtonText: "暂不确认",
        type: "success",
      },
    );
    await http.post(`/patient-portal/prescriptions/${row.id}/pickup`);
    ElMessage.success("已确认取药，祝您早日康复");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
  }
};
const viewPrescription = async (row) => {
  prescriptionDetail.value = {
    ...row,
    items: await http.get(`/patient-portal/prescriptions/${row.id}/items`),
  };
  detailVisible.value = true;
};
const markNotificationsRead = async () => {
  await http.post("/patient-portal/notifications/read");
  ElMessage.success("取药通知已标记为已读");
  await load();
};

onMounted(load);
const refreshTimer = window.setInterval(load, 30000);
onBeforeUnmount(() => window.clearInterval(refreshTimer));
</script>

<template>
  <div v-if="data" class="portal">
    <div class="portal-head">
      <div>
        <h2>您好，{{ data.patient.name }}</h2>
        <p>{{ gender(data.patient.gender) }} · {{ data.patient.patientNo }}</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <div class="portal-primary">
      <el-card class="registration-card" header="自助挂号">
        <el-form label-position="top">
          <el-form-item label="选择医生"
            ><el-select v-model="form.doctorId" style="width: 100%"
              ><el-option
                v-for="item in doctors"
                :key="item.id"
                :value="item.id"
                :label="`${item.departmentName} · ${item.realName}（${item.title}）`" /></el-select
          ></el-form-item>
          <el-form-item label="就诊日期"
            ><el-date-picker
              v-model="form.visitDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
          /></el-form-item>
          <el-button type="primary" @click="prepare">下一步</el-button>
        </el-form>
      </el-card>

      <el-card class="notification-card">
        <template #header
          ><div class="card-title">
            <div>取药通知 <small>药房发药后会在此提醒</small></div>
            <el-button
              v-if="unreadNotificationCount"
              text
              type="primary"
              @click="markNotificationsRead"
              >全部设为已读</el-button
            >
          </div></template
        >
        <el-timeline v-if="notifications.length" class="notification-list">
          <el-timeline-item
            v-for="notification in notificationRows"
            :key="notification.id"
            :hollow="Boolean(notification.readAt)"
            :type="notification.readAt ? 'info' : 'success'"
            :timestamp="notification.createdAt"
            ><b :class="{ unread: !notification.readAt }">{{
              notification.title
            }}</b>
            <p>{{ notification.content }}</p></el-timeline-item
          >
        </el-timeline>
        <el-empty
          v-else
          :image-size="84"
          description="药房完成发药后，取药通知会显示在这里"
        />
        <div
          v-if="notifications.length"
          class="list-footer notification-footer"
        >
          <span>共 {{ notifications.length }} 条通知</span>
          <el-pagination
            v-if="notifications.length > notificationPageSize"
            v-model:current-page="notificationPage"
            :page-size="notificationPageSize"
            layout="prev, pager, next"
            :total="notifications.length"
          />
        </div>
      </el-card>
    </div>

    <div class="portal-records">
      <el-card class="record-card">
        <template #header
          ><div class="card-title">
            我的挂号 <small>候诊前可自助取消</small>
          </div></template
        >
        <el-table
          class="desktop-table"
          :data="registrationRows"
          table-layout="auto"
        >
          <el-table-column prop="visitDate" label="日期" min-width="112" />
          <el-table-column prop="departmentName" label="科室" min-width="96" />
          <el-table-column prop="doctorName" label="医生" min-width="92" />
          <el-table-column label="候诊" min-width="110"
            ><template #default="scope"
              ><span v-if="scope.row.status === 'WAITING'" class="queue-number"
                >前方 {{ scope.row.queueAhead }} 人</span
              ><span v-else>—</span></template
            ></el-table-column
          >
          <el-table-column label="状态" min-width="100"
            ><template #default="scope"
              ><el-tag
                :type="scope.row.status === 'WAITING' ? 'warning' : 'success'"
                >{{ statusLabel(scope.row.status) }}</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column label="操作" min-width="86"
            ><template #default="scope"
              ><el-button
                v-if="scope.row.status === 'WAITING'"
                type="danger"
                link
                @click="cancelRegistration(scope.row)"
                >取消挂号</el-button
              ><span v-else>—</span></template
            ></el-table-column
          >
        </el-table>
        <div class="mobile-record-list">
          <article
            v-for="row in registrationRows"
            :key="row.id"
            class="mobile-record"
          >
            <div class="mobile-record-head">
              <span>{{ row.departmentName }}</span
              ><el-tag
                :type="row.status === 'WAITING' ? 'warning' : 'success'"
                >{{ statusLabel(row.status) }}</el-tag
              >
            </div>
            <div class="mobile-record-meta">
              <span>{{ row.visitDate }} · {{ row.doctorName }}</span
              ><span v-if="row.status === 'WAITING'" class="queue-number"
                >前方 {{ row.queueAhead }} 人</span
              >
            </div>
            <div v-if="row.status === 'WAITING'" class="mobile-record-actions">
              <el-button
                type="danger"
                size="small"
                plain
                @click="cancelRegistration(row)"
                >取消挂号</el-button
              >
            </div>
          </article>
        </div>
        <div class="list-footer">
          <span>共 {{ registrations.length }} 条挂号记录</span
          ><el-pagination
            v-if="registrations.length > pageSize"
            v-model:current-page="registrationPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="registrations.length"
          />
        </div>
      </el-card>

      <el-card class="record-card prescription-card">
        <template #header
          ><div class="card-title">
            我的处方 <small>医院开具处方后会生成收费订单</small>
          </div></template
        >
        <el-table
          class="desktop-table"
          :data="prescriptionRows"
          table-layout="auto"
        >
          <el-table-column
            prop="prescriptionNo"
            label="处方号"
            min-width="190"
          />
          <el-table-column label="状态" min-width="120"
            ><template #default="scope">{{
              statusLabel(scope.row.status)
            }}</template></el-table-column
          >
          <el-table-column label="金额" min-width="110"
            ><template #default="scope"
              >¥{{ Number(scope.row.totalAmount).toFixed(2) }}</template
            ></el-table-column
          >
          <el-table-column label="操作" min-width="210"
            ><template #default="scope"
              ><div class="row-actions">
                <el-button size="small" @click="viewPrescription(scope.row)"
                  >查看药品</el-button
                ><el-button
                  v-if="scope.row.status === 'UNPAID'"
                  type="primary"
                  size="small"
                  @click="pay(scope.row)"
                  >立即支付</el-button
                ><el-button
                  v-else-if="scope.row.status === 'DISPENSED'"
                  type="success"
                  size="small"
                  @click="pickUp(scope.row)"
                  >确认已取药</el-button
                >
              </div></template
            ></el-table-column
          >
        </el-table>
        <div class="mobile-record-list">
          <article
            v-for="row in prescriptionRows"
            :key="row.id"
            class="mobile-record"
          >
            <div class="mobile-record-head">
              <span>{{ row.prescriptionNo }}</span
              ><span class="amount"
                >¥{{ Number(row.totalAmount).toFixed(2) }}</span
              >
            </div>
            <div class="mobile-record-meta">
              <span>{{ statusLabel(row.status) }}</span>
            </div>
            <div class="mobile-record-actions">
              <el-button size="small" @click="viewPrescription(row)"
                >查看药品</el-button
              ><el-button
                v-if="row.status === 'UNPAID'"
                type="primary"
                size="small"
                @click="pay(row)"
                >立即支付</el-button
              ><el-button
                v-else-if="row.status === 'DISPENSED'"
                type="success"
                size="small"
                @click="pickUp(row)"
                >确认已取药</el-button
              >
            </div>
          </article>
        </div>
        <div class="list-footer">
          <span>共 {{ prescriptions.length }} 条处方记录</span
          ><el-pagination
            v-if="prescriptions.length > pageSize"
            v-model:current-page="prescriptionPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="prescriptions.length"
          />
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="confirm"
      title="确认挂号"
      width="min(460px, calc(100vw - 32px))"
    >
      <div v-if="preview" class="confirm">
        <div>
          <span>就诊科室</span><b>{{ doctor?.departmentName }}</b>
        </div>
        <div>
          <span>就诊医生</span
          ><b>{{ doctor?.realName }} · {{ doctor?.title }}</b>
        </div>
        <div>
          <span>就诊日期</span><b>{{ form.visitDate }}</b>
        </div>
        <div class="queue">
          <span>您前面候诊人数</span
          ><strong>{{ preview.aheadCount }} <small>人</small></strong>
          <p>该人数包含待接诊和正在接诊的患者，实际等待时间以现场叫号为准。</p>
        </div>
      </div>
      <template #footer
        ><el-button @click="confirm = false">返回修改</el-button
        ><el-button type="primary" @click="register"
          >确认挂号</el-button
        ></template
      >
    </el-dialog>
    <el-dialog
      v-model="detailVisible"
      :title="`处方明细 · ${prescriptionDetail?.prescriptionNo || ''}`"
      width="min(720px, calc(100vw - 32px))"
    >
      <el-table
        class="desktop-table"
        :data="prescriptionDetail?.items || []"
        max-height="360"
        ><el-table-column
          prop="drugName"
          label="药品"
          min-width="150"
        /><el-table-column
          prop="specification"
          label="规格"
          min-width="120"
        /><el-table-column label="数量" width="90"
          ><template #default="scope"
            >{{ scope.row.quantity }}{{ scope.row.unit }}</template
          ></el-table-column
        ><el-table-column
          prop="route"
          label="用法"
          min-width="110"
        /><el-table-column label="小计" width="110"
          ><template #default="scope"
            >¥{{ Number(scope.row.itemAmount).toFixed(2) }}</template
          ></el-table-column
        ></el-table
      >
      <div class="mobile-record-list">
        <article
          v-for="item in prescriptionDetail?.items || []"
          :key="item.id || `${item.drugName}-${item.specification}`"
          class="mobile-record"
        >
          <div class="mobile-record-head">
            <span>{{ item.drugName }}</span
            ><span>¥{{ Number(item.itemAmount).toFixed(2) }}</span>
          </div>
          <div class="mobile-record-meta">
            <span>{{ item.specification || "规格未填写" }}</span
            ><span
              >{{ item.quantity }}{{ item.unit }} ·
              {{ item.route || "用法未填写" }}</span
            >
          </div>
        </article>
      </div>
      <template #footer
        ><span class="dialog-total"
          >合计 ¥{{
            Number(prescriptionDetail?.totalAmount || 0).toFixed(2)
          }}</span
        ><el-button @click="detailVisible = false">关闭</el-button></template
      >
    </el-dialog>
  </div>
  <el-skeleton v-else :rows="6" animated />
</template>

<style scoped>
.portal {
  max-width: 1360px;
  margin: auto;
}
.portal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.portal-head h2 {
  margin: 0;
  color: #183b67;
}
.portal-head p,
.el-timeline p {
  color: var(--muted);
}
.portal-primary,
.portal-records {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}
.portal-primary {
  grid-template-columns: minmax(330px, 0.72fr) minmax(500px, 1.28fr);
}
.portal-records {
  grid-template-columns: minmax(520px, 1fr) minmax(620px, 1.3fr);
}
.registration-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
}
.registration-card :deep(.el-form) {
  width: 100%;
  max-width: 440px;
}
.notification-list {
  margin: 3px 0 0 8px;
}
.notification-list :deep(.el-timeline-item) {
  padding-bottom: 19px;
}
.notification-list :deep(.el-timeline-item:last-child) {
  padding-bottom: 0;
}
.notification-list :deep(.el-timeline-item__timestamp) {
  margin-bottom: 5px;
}
.notification-list p {
  margin: 6px 0 0;
  line-height: 1.6;
}
.notification-footer {
  margin-top: 8px;
}
.record-card {
  min-width: 0;
}
.queue-number,
.dialog-total {
  color: #1d4e89;
  font-weight: 700;
  white-space: nowrap;
}
.confirm {
  display: grid;
  gap: 16px;
}
.confirm > div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}
.confirm span {
  color: var(--muted);
}
.queue {
  flex-wrap: wrap;
  align-items: flex-start !important;
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid #e8eef5;
}
.queue strong {
  color: #1d4e89;
  font-size: 28px;
}
.queue small,
.card-title small {
  color: var(--muted);
  font-size: 13px;
  font-weight: 400;
}
.queue p {
  width: 100%;
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}
.card-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.card-title > div {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.unread {
  color: #173a5f;
}
.row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.dialog-total {
  display: inline-block;
  margin-right: 16px;
}

@media (max-width: 1240px) {
  .portal-primary,
  .portal-records {
    grid-template-columns: 1fr;
  }
  .registration-card :deep(.el-form) {
    max-width: none;
  }
}
@media (max-width: 700px) {
  .portal-head {
    align-items: flex-start;
    gap: 12px;
  }
  .card-title,
  .card-title > div {
    align-items: flex-start;
    flex-direction: column;
  }
  .card-title small {
    display: block;
    margin-top: 3px;
  }
  .notification-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
