# RC Plus 2 + Matrice4D 指令飞行联调手册

## 1. 全局IP
应做配置后续组网后再另外给出，这里统一用$silas-ip进行标识

## 2. 实时图片流
### 2.1 组件： mqtt
- **Host**: 
```text
mqtt://$silas-ip:1883
```
- **用户名密码**:
```text
pilot/pilot123
```
- **Topic订阅**:
```text
silas/live/image/#
```
- **数据类型**: byte[]

---

## 3. 飞行控制（必须按顺序执行）（HTTP REQUEST）
除 **`POST /manage/api/v1/login`** 外，本章节所列控制类接口均需在请求头携带 `x-auth-token`。
### 3.1 Token 获取（`x-auth-token`）
- **Method**: `POST`
- **说明**： 其他接口的header，必须得提前获取，且每次后端重启，都得重新获取
- **URL**： http://172.18.17.34:6789/manage/api/v1/login
- **请求头**：
```http
Content-Type: application/json
```
- **入参**：
```json
{
  "username": "adminPC",
  "password": "adminPC",
  "flag": 1
}
```
- **x-auth-token 解析取值**： 返参中的data.access_token

---

### 3.2 DRC(Drone Remote Control) 连接鉴权 
- **说明**: 生成 DRC MQTT 的连接凭据，主要是获取client_id（这个接口可以提前获取，但每次手柄重新接管后，要再次申请DRC 需要重新申请client_id）
- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/e3dea0f5-37f2-4d79-ae58-490af3228069/drc/connect`
- **请求头**：
```http
x-auth-token: <登录后 data.access_token，见 3.1>
Content-Type: application/json
```
- **入参**:
```json
{
  "expire_sec": 3600
}
```
- **字段说明**:
- `expire_sec`：会话有效期，范围 `[1800, 86400]`
- **client_id解析获取：**: 返参中的data.client_id

---

### 3.3 进入 DRC 模式 （运行后，手柄会弹出 DRC 模式 控制的确认，确认后才可进行）
- **说明**: 让指定 RC Plus 2 网关进入 DRC 模式，并下发 ACL（pub/sub topics）
- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/e3dea0f5-37f2-4d79-ae58-490af3228069/drc/enter`
- **请求头**：
```http
x-auth-token: <登录后 data.access_token，见 3.1>
Content-Type: application/json
```
- **入参**:
```json
{
  "client_id": "",
  "rc_sn": "9N9CN4R0014JNX",
  "expire_sec": 3600
}
```
- **字段说明**:
- `client_id`：必须与 connect 阶段一致，3.2返参中的data.client_id
- `rc_sn`：RC Plus 2 手柄的 SN，此处为9N9CN4R0014JNX
- `expire_sec`：会话有效期，范围 `[1800, 86400]`
- **正确返参说明**:
data中会有sub和pub的主题名。

---

### 3.4 飞行目标点
- **说明**:
    - 校验 DRC 会话 owner
    - 自动抓取 `flight_authority_grab`
    - 下发 `fly_to_point`
- **Method**: `POST`
- **Path**: `http://172.18.17.34:6789/control/api/v1/pilot/rc-plus-2/workspaces/e3dea0f5-37f2-4d79-ae58-490af3228069/flight/fly-to-point`
- **请求头**：
```http
x-auth-token: <登录后 data.access_token，见 3.1>
Content-Type: application/json
```
- **入参**:
```json
{
  "client_id": "",
  "rc_sn": "9N9CN4R0014JNX",
  "max_speed": 5,
  "points": [
    {
      "latitude": 22.6070293,
      "longitude": 114.0561159,
      "height": 20
    }
  ]
}
```
- **字段说明**:
- `client_id`：必须与 connect 阶段一致，3.2返参中的data.client_id
- `max_speed`：速度，范围 `[1, 15]`
- `points`：目标点列表，至少 1 个
- 每个点包含：
    - `latitude`：纬度
    - `longitude`：经度
    - `height`：高度（相对高度，单位：米，DRC有个默认逻辑，如果这个字段小于20,也会先飞到20）

### 3.5 退出 DRC 
手柄可直接控制，建议手柄直接操作。