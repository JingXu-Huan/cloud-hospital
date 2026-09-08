# 云医院门诊闭环（Java + MySQL + Vue）

一个带管理员端与患者端的门诊业务 Demo，覆盖：患者建档、挂号、医生接诊、病历、处方、收费订单、患者缴费、发药通知和就诊结束。

## 项目结构

```text
cloud-hospital/
├── hospital-server/    Spring Boot 3 + MyBatis（Java 17）
├── hospital-web/       Vue 3 + Vite + Element Plus
└── sql/                MySQL 8 建表与演示数据
```

## 运行

1. 启动本地 MySQL 8（首次启动会自动执行唯一的 `sql/init.sql`）：

   ```powershell
   docker compose up -d mysql
   docker compose ps
   ```

2. 启动后端。Compose 配置与应用默认配置均为
   `127.0.0.1:3306/cloud_hospital`、`root/root`，因此本地开发无需额外设置环境变量：

   ```powershell
   Set-Location hospital-server
   mvn spring-boot:run
   ```

3. 新开一个终端启动前端：

   ```powershell
   Set-Location hospital-web
   npm install
   npm run dev
   ```

打开 Vite 输出的地址（默认 `http://localhost:5173`）。开发服务器会把 `/api` 代理到 `8080`。

停止数据库可运行 `docker compose down`；数据保存在命名卷中。若要清空并重新导入演示数据，运行
`docker compose down -v` 后再运行 `docker compose up -d mysql`。

## 已实现的业务规则

- 身份证号唯一；同患者、同医生、同日不得重复创建未取消挂号。
- 医生只能从 `WAITING` 状态开始接诊，取消也只允许 `WAITING`。
- 病历、处方只能在挂号 `IN_PROGRESS` 时写入；处方总金额始终由服务端用 `BigDecimal` 计算。
- 医生提交处方后自动结束本次接诊，并自动生成待支付收费订单；患者仅能支付本人订单，付款仅允许 `UNPAID → PAID`，发药仅允许 `PAID → DISPENSED`，患者确认取药仅允许 `DISPENSED → PICKED_UP`。
- 管理员端只查看待患者支付的订单；患者在本人端完成模拟支付，药房发药后向患者端发送取药通知。
- 对尚未注册患者端账号的患者，管理员可人工确认院内现金收款；后端会拒绝对已注册患者执行此操作。
- 所有状态变更采用带旧状态条件的更新（CAS 风格），并发重复点击只有一个请求能成功。

## 主要接口

接口前缀是 `/api/v1`。认证接口为 `/auth/login` 与 `/auth/register`；患者自助挂号和支付接口位于 `/patient-portal`，管理员接口包括 `/patients`、`/doctors`、`/registrations`、`/registrations/{id}/medical-record`、`/registrations/{id}/prescriptions`、`/prescriptions/{id}/manual-pay` 与 `/prescriptions/{id}/dispense`。

> 当前版本的支付为教学模拟，不对接第三方支付；生产环境仍应补 Spring Security/JWT、操作者审计、支付幂等键和支付回调验签。
