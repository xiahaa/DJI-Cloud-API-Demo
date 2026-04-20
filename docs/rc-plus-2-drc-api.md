# RC Plus 2 指令飞行接口文档

本文档对应 `sample` 模块新增接口：

- 控制器：`/home/idea/workspace/DJI-Cloud-API-Demo/sample/src/main/java/com/dji/sample/control/controller/RcPlus2ControlController.java`
- 服务：`/home/idea/workspace/DJI-Cloud-API-Demo/sample/src/main/java/com/dji/sample/control/service/impl/RcPlus2ControlServiceImpl.java`

适用场景：基于 **Pilot-to-Cloud DRC**（RC Plus 2）实现云端指令飞行，提供以下业务能力：

1. 飞向目标点（经纬高）
2. 调整姿态 yaw（DRC 摇杆 `w` 通道）

---

## 1. 基础信息

### 1.1 Base URL

```text
http://172.18.17.34:6789/
```

### 1.2 接口前缀

```text
http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2
```

### 1.3 通用请求头

```http
x-auth-token: <登录后token>
Content-Type: application/json
```

### 1.4 Token 获取（`x-auth-token`）

除 **`POST /manage/api/v1/login`** 外，本文档所列控制类接口均需在请求头携带 `x-auth-token`。

- **获取方式**：调用登录接口  
  `POST http://172.18.17.34:6789/manage/api/v1/login`  
  请求体字段：`username`、`password`、`flag`（整数，示例见下文）。
- **取值位置**：响应 JSON 中的 **`data.access_token`**（对应后端 `UserDTO.accessToken`，序列化名为 `access_token`）。
- **使用方式**：后续请求头设置 `x-auth-token: <data.access_token>`。

**示例请求体**（与初始化 SQL 中默认账号一致时可先试）：

```json
{
  "username": "adminPC",
  "password": "adminPC",
  "flag": 1
}
```

**一键 cURL（登录并查看整段响应，手动复制 `access_token`）**：

```bash
curl -sS -X POST 'http://172.18.17.34:6789/manage/api/v1/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"adminPC","password":"adminPC","flag":1}'
```

> 默认账号见：`/home/idea/workspace/DJI-Cloud-API-Demo/sql/cloud_sample.sql` 表 `manage_user`。

### 1.5 推荐顺序

必须按以下顺序调用：

1. `drc/connect`
2. `drc/enter`
3. `flight/fly-to-point` 或 `flight/yaw`
4. `drc/exit`

### 1.6 `gateway_sn` 与请求体 `rc_sn`

[DJI 上云 API — RC Plus 2 — DRC](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html) 中，`thing/product/{gateway_sn}/events`、`/services`、`/services_reply`、`/drc/down` 等 Topic 的占位符均为 **`{gateway_sn}`**（不是飞机机身 `device_sn`）。

在本仓库的 HTTP 接口里：**`gateway_sn` = 请求 JSON 里的 `rc_sn`**（Pilot 侧遥控器/网关序列号）。飞机序列号仅出现在设备拓扑里（如 Redis / `childDeviceSn`），**不得**用来拼上述 Topic。官网该页为 VuePress 单页应用，若需离线核对正文，可在浏览器「另存为」后查看，或从页面加载的 `assets/js/v-c2b06e90.*.js` 等 chunk 中检索 `gateway_sn`（随构建哈希变化，以实际网络请求为准）。

---

## 2. 接口清单

## 2.1 DRC 连接鉴权

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/drc/connect`
- **说明**: 生成 DRC MQTT 的连接凭据（address/username/password/client_id）

### 请求头

与下方 cURL 一致，调用时需携带：

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "expire_sec": 3600
}
```

### 字段说明

- `client_id`：可选，不传则服务端生成
- `expire_sec`：会话有效期，范围 `[1800, 86400]`

### cURL（本地终端）

```bash
# 先登录拿到 TOKEN（见文档 1.4），再设置 WORKSPACE_ID、RC_SN、CLIENT_ID（见文档 4.1）
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/drc/connect' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","expire_sec":3600}'
```

---

## 2.2 进入 DRC 模式

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/drc/enter`
- **说明**: 让指定 RC Plus 2 网关进入 DRC 模式，并下发 ACL（pub/sub topics）

### 与「遥控器确认控制」弹框的关系

- **`2.2` 在本仓库中的范围**：只下发云端协议里的 **`drc_mode_enter`**（并写入 Redis ACL），**不会**调用 **`flight_authority_grab`**。因此很多情况下 **遥控器在 `2.2` 后不会出现** 你印象里的「是否允许云端/DRC 控制」类弹框，这是预期现象。
- **弹框更常出现的环节**：DJI 文档里与**飞行控制权**相关的交互，通常对应 MQTT 的 **`flight_authority_grab`**。在本接口清单里，这一步放在 **`2.3`（飞向点）** 与 **`2.4`（Yaw）**：实现里会先抢飞行控制权，再发飞行/DRC 指令（见 `RcPlus2ControlServiceImpl.ensureFlightAuthority`）。
- **若 `2.3` / `2.4` 仍无弹框**：可能与 Pilot 版本、安全/授权策略、当前是否已由云端持有控制权（`control_source` 等状态）有关；以机上实际提示与 MQTT `services_reply` 为准。

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "expire_sec": 3600
}
```

### 字段说明

- `client_id`：必须与 connect 阶段一致
- `rc_sn`：RC Plus 2 的网关 SN
- `expire_sec`：会话有效期，范围 `[1800, 86400]`

### cURL（本地终端）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/drc/enter' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","rc_sn":"'"${RC_SN}"'","expire_sec":3600}'
```

### 常见错误（210003）

若返回 **`Error Code: 210003`**（*The current type of the device does not support this function*），在 RC Plus 2 / 遥控器网关上通常**不是**固件声明“不支持 DRC”，而是旧版 cloud-sdk 在 `AbstractControlService` 上对 `drc_mode_enter` 等方法误加了 `@CloudSDKVersion(exclude = RC)`，`CloudSDKHandler` 在调用前会直接拦截并抛出该错误。当前仓库已去掉上述 RC 排除，重新编译部署后端后再调 2.2 即可越过该拦截；若仍有失败，再看飞行器/遥控器固件与云端返回的 `result`。

---

## 2.3 飞向目标点

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/flight/fly-to-point`
- **说明**:
  - 校验 DRC 会话 owner
  - 自动抓取 `flight_authority_grab`
  - 下发 `fly_to_point`
- **调试提示**：`2.3` 依赖飞机与遥控器已建立正常链路；若**飞机未连接**，MQTT 可能在超时内收不到 `services_reply`，后端表现为 **`211001`（No message reply received）**，详见 **§5**。
- **官方协议说明（FlyTo 飞向目标点）**：[DJI 上云 API — RC Plus 2 DRC — FlyTo 飞向目标点](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html#flyto-%E9%A3%9E%E5%90%91%E7%9B%AE%E6%A0%87%E7%82%B9)（与云端 `fly_to_point` / `thing/product/{gateway_sn}/services` 等约定对照用）。该节正文备份摘录见文末 **[附录 A](#appendix-a-flyto)**。

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "max_speed": 5,
  "points": [
    {
      "latitude": 31.2304,
      "longitude": 121.4737,
      "height": 50
    }
  ]
}
```

### 字段说明

- `max_speed`：速度，范围 `[1, 15]`
- `points`：目标点列表，至少 1 个
- 每个点包含：
  - `latitude`：纬度
  - `longitude`：经度
  - `height`：高度（相对高度，单位按平台定义）

### cURL（本地终端）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/flight/fly-to-point' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id":"'"${CLIENT_ID}"'",
    "rc_sn":"'"${RC_SN}"'",
    "max_speed":5,
    "points":[{"latitude":31.2304,"longitude":121.4737,"height":50}]
  }'
```

---

## 2.4 Yaw 调整（DRC）

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/flight/yaw`
- **说明**:
  - 校验 DRC 会话 owner
  - 自动抓取 `flight_authority_grab`
  - 下发 DRC `stick_control`，通过 `yaw` 杆量控制偏航
- **协议变更说明**：`drone_control` 已废弃，当前实现已改为 RC Plus 2 文档中的 `stick_control`（详见文末 **[附录 B](#appendix-b-stick-control)**）。

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体（仅调 yaw，其他通道默认中位）

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "seq": 10001,
  "yaw": 1084
}
```

### 请求体（四通道完整输入）

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "seq": 10001,
  "roll": 1024,
  "pitch": 1024,
  "throttle": 1024,
  "yaw": 964
}
```

### 字段范围

- `roll`：`[364, 1684]`，默认 `1024`（横滚/A 通道）
- `pitch`：`[364, 1684]`，默认 `1024`（俯仰/E 通道）
- `throttle`：`[364, 1684]`，默认 `1024`（升降/T 通道）
- `yaw`：`[364, 1684]`，**必填**（偏航/R 通道）
- `seq`：可选，不传时服务端使用当前时间戳（置于 `method` 同级）

### cURL（本地终端，仅调 yaw）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/flight/yaw' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","rc_sn":"'"${RC_SN}"'","seq":10001,"yaw":1084}'
```

### cURL（本地终端，四通道完整输入）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/flight/yaw' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","rc_sn":"'"${RC_SN}"'","seq":10001,"roll":1024,"pitch":1024,"throttle":1024,"yaw":964}'
```

---

## 2.5 获取当前 OSD 信息（device /osd）

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/flight/osd/latest`
- **说明**:
  - 接口调用后，临时等待下一条飞机 `device_sn` 对应 Topic `thing/product/{device_sn}/osd` 上报（默认超时 10s）
  - 收到后立即返回，不做持久缓存
  - 需要同时传 `rc_sn`（用于 DRC 会话归属校验）和 `device_sn`（用于订阅 OSD 主题）
  - 返回值直接为 MQTT 上报中的 `data` 整体对象
- **官方协议说明（OSD Topic）**：[DJI 上云 API — RC Plus 2 DRC — DRC 高频 OSD 信息上报](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html#drc-%E9%AB%98%E9%A2%91-osd-%E4%BF%A1%E6%81%AF%E4%B8%8A%E6%8A%A5)（摘录见文末 **[附录 C](#appendix-c-drc-osd-info-push)**）。

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "device_sn": "AIRCRAFT_SN_EXAMPLE",
  "wait_timeout_ms": 10000
}
```

### 字段说明

- `device_sn`：必填，飞机 SN，用于匹配 `thing/product/{device_sn}/osd`。
- `wait_timeout_ms`：可选，等待下一条 `/osd` 的超时时间（毫秒），范围 `[1000, 30000]`；不传默认 `10000`。

### cURL（本地终端）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/flight/osd/latest' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","rc_sn":"'"${RC_SN}"'","device_sn":"'"${DEVICE_SN}"'","wait_timeout_ms":10000}'
```

---

## 2.6 退出 DRC 模式

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/drc/exit`
- **说明**: 退出 DRC 模式并清理 redis 会话/ACL

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "expire_sec": 3600
}
```

### cURL（本地终端）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/drc/exit' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"'"${CLIENT_ID}"'","rc_sn":"'"${RC_SN}"'","expire_sec":3600}'
```

---

## 2.7 飞向目标点（actions 变形）

- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/{workspace_id}/flight/fly-to-point-by-actions`
- **说明**:
  - 入参给动作序列 `actions`，服务端在 `delay_ms` 后读取当前 OSD（`heading/latitude/longitude/height`）
  - OSD 读取 Topic 为 `thing/product/{device_sn}/osd`，因此该接口新增 `device_sn`
  - `data.attitude_head` 来源范围是 `[-180, 180]`，服务端会先转换为 `[0, 360)` 再参与动作计算（机头 0 度，右侧 `(0,180]`，左侧 `[-180,0)`）
  - 按动作序列计算新的目标经纬度；目标高度使用入参 `height`（不传默认 `145`）
  - 内部复用现有 `fly_to_point` 下发链路（topic/method 与 `2.3` 一致）

### 请求头

```http
x-auth-token: <登录后 data.access_token，见 1.4>
Content-Type: application/json
```

### 请求体

```json
{
  "client_id": "postman-rc2-001",
  "rc_sn": "RCPLUS2_SN_EXAMPLE",
  "device_sn": "AIRCRAFT_SN_EXAMPLE",
  "max_speed": 3,
  "height": 145,
  "delay_ms": 0,
  "wait_timeout_ms": 10000,
  "actions": [
    {
      "operator": 2,
      "value": 15
    },
    {
      "operator": 1,
      "value": 10
    }
  ]
}
```

### 字段说明

- `operator`：
  - `1`：向前（`value` 单位：米）
  - `2`：左旋转（`value` 单位：度）
  - `3`：右旋转（`value` 单位：度）
- `device_sn`：必填，飞机 SN，用于读取 `thing/product/{device_sn}/osd`。
- `height`：可选，`fly_to_point` 的目标高度，范围 `[20, 500]`；不传默认 `145`。
- `delay_ms`：读取当前 OSD 前的延时；`0` 表示不延时
- `wait_timeout_ms`：等待下一条 `/osd` 的超时（毫秒），范围 `[1000, 30000]`

### cURL（本地终端）

```bash
curl -sS -X POST 'http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/'"${WORKSPACE_ID}"'/flight/fly-to-point-by-actions' \
  -H 'x-auth-token: '"${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id":"'"${CLIENT_ID}"'",
    "rc_sn":"'"${RC_SN}"'",
    "device_sn":"'"${DEVICE_SN}"'",
    "max_speed":3,
    "height":145,
    "delay_ms":0,
    "wait_timeout_ms":10000,
    "actions":[
      {"operator":2,"value":15},
      {"operator":1,"value":10}
    ]
  }'
```

---

## 3. Postman 变量建议

建议在 Postman 环境中配置：

- `baseUrl`：`http://172.18.17.34:6789`
- `token`：登录后 token
- `workspaceId`：工作空间 ID
- `rcSn`：RC Plus 2 SN
- `deviceSn`：飞机 `device_sn`
- `clientId`：例如 `postman-rc2-001`

---

## 4. workspaceId 获取说明（启动测试前必看）

### 4.1 推荐方式：通过接口获取当前 workspaceId

- 登录后调用：
  - `GET http://172.18.17.34:6789/manage/api/v1/workspaces/current`
- 请求头：
  - `x-auth-token: <登录后 data.access_token，见 1.4>`
- 从响应中读取 **`data.workspaceId`**（`WorkspaceDTO` 序列化为 camelCase）作为路径中的 `{workspace_id}`。

#### cURL：登录（获取 TOKEN）

**登录接口请求头**（与 cURL 一致）：

```http
Content-Type: application/json
```

（无需 `x-auth-token`。）

```bash
# 示例账号见 sql/cloud_sample.sql（如 adminPC / adminPC）
curl -sS -X POST 'http://172.18.17.34:6789/manage/api/v1/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"adminPC","password":"adminPC","flag":1}'
# 从返回 JSON 的 data.access_token 复制到环境变量：
export TOKEN='<粘贴 access_token>'
```

#### cURL：当前 workspace（获取 WORKSPACE_ID）

**当前 workspace 接口请求头**（与 cURL 一致）：

```http
x-auth-token: <登录后 data.access_token，见 1.4>
```

```bash
curl -sS 'http://172.18.17.34:6789/manage/api/v1/workspaces/current' \
  -H 'x-auth-token: '"${TOKEN}"
# 从返回 JSON 的 data.workspaceId 复制到环境变量（WorkspaceDTO 序列化为 camelCase）：
export WORKSPACE_ID='<粘贴 workspaceId>'
export RC_SN='<你的 RC Plus 2 网关 SN>'
export CLIENT_ID='postman-rc2-001'
```

### 4.2 数据库来源（用于核对）

- 数据库脚本文件：
  - `/home/idea/workspace/DJI-Cloud-API-Demo/sql/cloud_sample.sql`
- 表名：
  - `manage_workspace`
- 字段名：
  - `workspace_id`
- 默认初始化值（脚本中内置）：
  - `e3dea0f5-37f2-4d79-ae58-490af3228069`

---

## 5. 常见报错与排查

- **`RC gateway is offline or unknown`**
  - 说明 `rc_sn` 不在线，先确认设备上线与 SN。

- **`RC is not in DRC mode. Please call drc/enter first.`**
  - 先调用 `drc/connect` + `drc/enter`。

- **`DRC session is occupied by another client.`**
  - 当前设备已被其他 `client_id` 占用。

- **`Failed to grab flight authority`**
  - 飞控权限未拿到，检查设备状态/控制权冲突。

- **`Error Code: 211001`**（*The sending of mqtt message is abnormal.*，附带 *No message reply received.*）
  - **含义（本仓库实现）**：云端已通过 MQTT 下发 `services` 请求，但在默认超时与重试内**未收到**设备侧匹配的 `services_reply`（见 cloud-sdk `MqttGatewayPublish.publishWithReply` → `CloudSDKErrorEnum.MQTT_PUBLISH_ABNORMAL`）。
  - **与「飞机未连接」的关系**：**不能**仅凭 211001 断定唯一原因就是飞机未连（遥控器离线、Broker/网络、SN/topic 异常等也会同样超时无回包）。但在**飞机未与 RC 建立链路、或未处于可应答的飞行/作业状态**时，`2.3` / `2.4` 涉及的 `flight_authority_grab`、`fly_to_point` 等**很容易出现长时间无正常回包**，从而表现为 211001，**与未连接场景是相符的**。
  - **建议**：先保证 RC 在线、飞机与遥控器已连接且满足官方文档要求的作业态，再重试 `2.3`；若仍 211001，再查 MQTT 连通与设备是否真实订阅/应答。
  - **`fly_to_point_progress` 与 `need_reply`**：[官方 DRC 文档](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html) 中该事件可能带 `need_reply: 1`，云端需回 `events_reply`。旧版 cloud-sdk 的 `EventsRouter` 只填了 `from`、未填 `gateway`，导致 `SDKControlService.flyToPointProgress` 用 `getGateway()` 查不到设备、**无法回包**。当前仓库已在 `EventsRouter` 中从 Topic 同时设置 **`gateway` = `from` = `{gateway_sn}`**（与 `thing/product/{gateway_sn}/events` 一致）。若你仍用旧包，可对比升级。

- **`Please configure the drc link parameters of mqtt in the backend configuration file first.`**
  - 原因：后端 `MqttPropertyConfiguration.getMqttBrokerWithDrc` 需要 **`mqtt.DRC`** 配置块；未配置时 `drc/connect`（及 DRC 相关逻辑）会直接抛此异常。
  - 处理：在 **`/home/idea/workspace/DJI-Cloud-API-Demo/sample/src/main/resources/application.yml`** 中补全 `mqtt.DRC`（可与 `mqtt.BASIC` 指向同一 EMQX，或按你环境使用 `WS` + `path`），**修改后需重启 sample 服务**。
  - 参考：仓库内已提供与 `BASIC` 同机 `172.18.17.34:1883` 的默认 `DRC` 示例；若你实际使用 WebSocket，请改为 `protocol: WS`、`port: 8083`、`path: /mqtt`（以你的 EMQX 为准）。

---

## 6. 参考

- [DJI RC Plus 2 DRC API](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html)

---

<a id="appendix-a-flyto"></a>

## 附录 A. FlyTo 飞向目标点（官方摘录 · 备查）

以下文字自 DJI 上云 API 页面 **「flyto 飞向目标点」** 一节摘录（与 **§2.3** 中链接指向内容对应），便于链接调整或改版后仍可对读 MQTT 约定。**若与当前官网不一致，以 [DJI 上云 API 在线文档](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html) 为准。**

**特别说明**：飞机有最低飞行高度（20m）安全保障机制，如果飞机相对起飞点高度低于 20m，会先上升到 20m。

**Topic**：`thing/product/{gateway_sn}/services`  
**Direction**：down  
**Method**：`fly_to_point`

**Data：**

| Column | Name | Type | constraint | Description |
| --- | --- | --- | --- | --- |
| fly_to_id | 飞向目标点 ID | text | | |
| max_speed | flyto 的飞行过程中能达到的最大速度 | int | {"max":15,"min":0,"unit_name":"米每秒 / m/s"} | |
| points | flyto 目标点列表 | array | {"size": -, "item_type": struct} | 仅支持 1 个目标点 |
| »latitude | 目标点纬度(角度值) | double | {"max":90,"min":-90} | 目标点纬度，角度值，南纬是负，北纬是正，精度到小数点后 6 位 |
| »longitude | 目标点经度(角度值) | double | {"max":180,"min":-180} | 目标点经度，角度值，东经是正，西经是负，精度到小数点后 6 位 |
| »height | 目标点高度 | float | {"max":10000,"min":2,"step":0.1,"unit_name":"米 / m"} | 目标点高度（椭球高），使用 WGS84 模型 |

**Example（下行请求体示例）：**

```json
{
	"bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
	"data": {
		"fly_to_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
		"max_speed": 12,
		"points": [
			{
				"height": 100,
				"latitude": 12.23,
				"longitude": 12.23
			}
		]
	},
	"tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
	"timestamp": 1654070968655,
	"method": "fly_to_point"
}
```

**Topic**：`thing/product/{gateway_sn}/services_reply`  
**Direction**：up  
**Method**：`fly_to_point`

**Data：**

| Column | Name | Type | Description |
| --- | --- | --- | --- |
| result | 返回码 | int | 非 0 代表错误 |

**Example（上行应答示例）：**

```json
{
	"bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
	"data": {
		"result": 0
	},
	"tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
	"timestamp": 1654070968655,
	"method": "fly_to_point"
}
```

---

<a id="appendix-b-stick-control"></a>

## 附录 B. DRC 杆量控制（官方摘录 · 备查）

以下文字对应 RC Plus 2 官方页面“DRC-杆量控制”章节，用于对照当前 `2.4` 的实现（`stick_control`）。
若与官网更新内容不一致，请以在线文档为准：  
[DJI 上云 API - RC Plus 2 - remote control](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/remote-control.html#drc-%E6%9D%86%E9%87%8F%E6%8E%A7%E5%88%B6)

建立 DRC 链路之后，可通过“DRC-杆量控制”指令控制飞行器姿态。发送频率需要保持 `5-10Hz`，才能比较精准地控制飞行器运动。本协议**无回包机制**。

**Topic**：`thing/product/{gateway_sn}/drc/down`  
**Direction**：down  
**Method**：`stick_control`

**Data：**

| Column | Name | Type | constraint | Description |
| --- | --- | --- | --- | --- |
| roll | 横滚通道 | int | {"max":"1684","min":"364"} | 对应遥控器 A 通道，控制飞行器横滚（左右平移）。`1024` 为中值（无动作）。 |
| pitch | 俯仰通道 | int | {"max":"1684","min":"364"} | 对应遥控器 E 通道，控制飞行器俯仰（前后平移）。`1024` 为中值（无动作）。 |
| throttle | 升降通道 | int | {"max":"1684","min":"364"} | 对应遥控器 T 通道，控制飞行器升降。`1024` 为悬停状态。 |
| yaw | 偏航通道 | int | {"max":"1684","min":"364"} | 对应遥控器 R 通道，控制飞行器偏航（左右旋转）。`1024` 为中值（无动作）。 |

**Example（官方示例）：**

```json
{
  "seq": 1,
  "method": "stick_control",
  "data": {
    "roll": 1024,
    "pitch": 1024,
    "throttle": 1024,
    "yaw": 1024
  }
}
```

---

<a id="appendix-c-drc-osd-info-push"></a>

## 附录 C. DRC 高频 OSD 信息上报（官方摘录 · 备查）

以下内容对应 RC Plus 2 官方文档章节“DRC 高频 OSD 信息上报”，用于对照 `2.5` 的最新 OSD 查询接口。  
若与官网更新不一致，请以在线文档为准：  
[DJI 上云 API - RC Plus 2 - DRC 高频 OSD 信息上报](https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html#drc-%E9%AB%98%E9%A2%91-osd-%E4%BF%A1%E6%81%AF%E4%B8%8A%E6%8A%A5)

**Topic**：`thing/product/{gateway_sn}/drc/up`  
**Direction**：up  
**Method**：`osd_info_push`

**Data：**

| Column | Name | Type | constraint | Description |
| --- | --- | --- | --- | --- |
| attitude_head | 飞行器姿态 head 角 | double | {"unit_name":"度 / °"} | 飞行器姿态 head 角 |
| latitude | 飞行器纬度 | double | {"unit_name":"度 / °"} | |
| longitude | 飞行器经度 | double | {"unit_name":"度 / °"} | |
| height | 飞行器高度 | double | {"unit_name":"度 / °"} | 飞行器海拔高度 |
| speed_x | 当前飞行器 x 坐标方向的速度 | double | {"unit_name":"米每秒 / m/s"} | 当前飞行器 x 坐标方向的速度 |
| speed_y | 当前飞行器 y 坐标方向的速度 | double | {"unit_name":"米每秒 / m/s"} | 当前飞行器 y 坐标方向的速度 |
| speed_z | 当前飞行器 z 坐标方向的速度 | double | {"unit_name":"米每秒 / m/s"} | |
| gimbal_pitch | 云台 pitch 角 | double | {"unit_name":"度 / °"} | |
| gimbal_roll | 云台 roll 角 | double | {"unit_name":"度 / °"} | |
| gimbal_yaw | 云台 yaw 角 | double | {"unit_name":"度 / °"} | |

**Example（官方示例）：**

```json
{
	"data": {
		"attitude_head": 60,
		"gimbal_pitch": 60,
		"gimbal_roll": 60,
		"gimbal_yaw": 60,
		"height": 10,
		"latitude": 10,
		"longitude": 10,
		"speed_x": 10,
		"speed_y": 10,
		"speed_z": 10
	},
	"timestamp": 1670415891013
}
```

**本仓库接口映射说明：**
- `2.5` 在调用时临时等待一条 `thing/product/{device_sn}/osd`，收到后返回 `data` 整体对象，不额外拼装字段。
- `2.7` 读取 `data.attitude_head` 后会先把 `[-180, 180]` 归一化到 `[0, 360)`，再执行动作序列换算。
