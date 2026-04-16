# Ubuntu 20.04 部署 MediaMTX（RTMP 收流）

本文说明在系统目录 `/opt/mediamtx` 安装 **MediaMTX**，用于接收 DJI 上云直播的 **RTMP 推流**。与 Java 项目分离，可作为本机公共服务使用。

**适用架构**：x86_64（`linux_amd64`）。若为 ARM，请到 [MediaMTX Releases](https://github.com/bluenviron/mediamtx/releases) 下载对应压缩包（如 `linux_arm64v8`）。

---

## 1. 安装目录与下载

```bash
sudo mkdir -p /opt/mediamtx
cd /opt/mediamtx
```

```bash
sudo curl -sL -o mediamtx.tar.gz https://github.com/bluenviron/mediamtx/releases/download/v1.17.1/mediamtx_v1.17.1_linux_amd64.tar.gz
```

```bash
sudo tar -xzf mediamtx.tar.gz
```

```bash
sudo rm -f mediamtx.tar.gz
```

```bash
sudo chmod +x /opt/mediamtx/mediamtx
```

---

## 2. 配置文件（RTMP 收推流）

本仓库中 RTMP 最终地址形式为：`rtmp://<IP>/live/<机库SN>-<镜头索引>`，因此应用名（path）使用 **`live`**。

### `path` 是什么（和 RTMP 地址怎么对应）

RTMP 推流/拉流地址一般可以看成：

```text
rtmp://<主机或IP>:1935/<path 名>/<流名称 stream name>
```

- **`<path 名>`**：在 MediaMTX 里对应配置里 `paths:` 下面的 **键名**（例如 `live`）。很多教程里也把它叫「应用名 app」。
- **`<流名称>`**：同一路径下区分多路推流的名称（例如 `test`，或 DJI 侧拼出来的 `机库SN-镜头索引`）。**不是** `mediamtx.yml` 里单独再写一项，除非你要给不同流配不同高级参数。

`source: publisher` 表示：**这一路的内容由推流客户端发布上来**（设备/FFmpeg 推上来），而不是由 MediaMTX 去别的 URL 拉流。

**注意**：只配置 `live` 时，推流地址里的 path **必须是** `live`（例如 `rtmp://IP/live/流名`）。若写成其他 path 名，MediaMTX 不会接收。

### 本机 + 同网段其他机器都要能访问

1. **监听地址**：使用 `rtmpAddress: ':1935'`（或 `:1935`）表示在 **所有网卡** 上监听 **TCP 1935**，局域网内其他电脑可以用你的 **局域网 IP** 访问。
2. **不要用** `127.0.0.1:1935` 作为唯一监听地址，否则只有本机能连，别的机器连不上。
3. 查本机局域网 IP（示例）：

   ```bash
   ip -4 addr show
   ```

   在 `application.yml` 里填 **与机场/遥控同一网段、且对方路由可达** 的那个地址，例如 `rtmp://192.168.1.100/live/`。
4. **防火墙**：见下文第 5 节，放行 `1935/tcp`。
5. 同网段另一台机器拉流或测试推流时，把地址里的 IP 换成运行 MediaMTX 的那台机器的局域网 IP 即可。

### `mediamtx.yml`（仅 `live`）

不要写 `rtspAddress: ''` 来「关掉」RTSP：在当前版本里 RTSP 仍算启用，会报错 `rtspAddress must be set`。应使用 **`rtsp: false`**（以及关掉不用的 HLS/WebRTC/SRT）。

```bash
sudo tee /opt/mediamtx/mediamtx.yml >/dev/null <<'EOF'
rtmpAddress: ':1935'
rtsp: false
hls: false
webrtc: false
srt: false

paths:
  live:
    source: publisher
EOF
```

若**仅本机**试推、暂时不希望局域网访问，可临时把 `rtmpAddress` 设为 `'127.0.0.1:1935'`；要与同网段设备/其他电脑互通时，请改回 `':1935'` 并放行防火墙。

### 流名里带斜杠时（DJI 常见：`live/test/1234567890`）

日志里若出现：

```text
path 'live/test/1776318939167' is not configured
```

含义是：推流地址被解析成 **整段 path = `live/test/…`**，而不是「path=`live` + 流名=`test/…`」。**只配置了 `paths.live` 时，不会匹配 `live/test/...`**，连接会被关掉，ffmpeg 侧表现为 `Broken pipe` / `Input/output error`。

**做法二选一：**

1. **推荐：在 `mediamtx.yml` 里为这类 path 增加正则（路径名以 `~` 开头表示正则，见 MediaMTX 文档 “Path name regular expressions”）：**

   ```yaml
   paths:
     live:
       source: publisher
     "~^live/test/.*":
       source: publisher
   ```

   若以后还有 `live/别的前缀/数字`，可把正则放宽，例如 `"~^live/.+/.*":`（按你环境评估安全面后再用）。

2. **或**：在云平台 / Pilot 侧把 RTMP 改成 **单段流名**（不要 `test/数字` 中间斜杠），例如 `rtmp://IP:1935/live/test-1776318939167`，则只保留 `paths.live` 即可。

**注意：** 推流 URL **不要**多写末尾斜杠，例如避免 `rtmp://127.0.0.1/live/test/`（可能被当成异常 path）；用 `rtmp://127.0.0.1:1935/live/test` 即可。

改完配置后需 **重启 MediaMTX**（如 `sudo systemctl restart mediamtx`，服务名以你本机为准）。

---

## 3. 前台试跑（验证服务）

```bash
cd /opt/mediamtx
sudo ./mediamtx mediamtx.yml
```

另开终端，用 FFmpeg 向本机推一条测试流（需已安装 `ffmpeg`）：

```bash
ffmpeg -re -f lavfi -i testsrc=size=1280x720:rate=30 -f lavfi -i sine -c:v libx264 -preset ultrafast -tune zerolatency -c:a aac -f flv rtmp://127.0.0.1/live/test
```

可选：播放验证：

```bash
ffplay rtmp://127.0.0.1/live/test
```

确认无误后，在运行 `mediamtx` 的终端按 **Ctrl+C** 停止。

---

## 4. systemd 常驻服务（可选）

```bash
sudo tee /etc/systemd/system/mediamtx.service >/dev/null <<'EOF'
[Unit]
Description=MediaMTX (RTMP ingest)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=/opt/mediamtx
ExecStart=/opt/mediamtx/mediamtx /opt/mediamtx/mediamtx.yml
Restart=on-failure
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable mediamtx
sudo systemctl start mediamtx
sudo systemctl status mediamtx
```

常用命令：

```bash
sudo systemctl restart mediamtx
sudo journalctl -u mediamtx -f
```

---

## 5. 防火墙（若启用 ufw）

```bash
sudo ufw allow 1935/tcp
sudo ufw reload
```

---

## 6. 与本项目 `application.yml` 的对应关系

在 `sample/src/main/resources/application.yml` 中配置 RTMP **基础 URL**（将 `你的IP` 替换为 **机场/遥控所在网络能访问到的** 本机 IP）：

```yaml
livestream:
  url:
    rtmp:
      url: 'rtmp://你的IP/live/'
```

程序会在该 URL 后自动拼接 `机库SN-镜头索引` 作为流名，例如：

`rtmp://你的IP/live/1581FXXXX-80-0-0`（示例，以实际设备为准）。

---

## 7. 本机抽帧 / 算法（OpenCV / FFmpeg）

推流成功后，拉流地址与 path、流名一致，例如：

```text
rtmp://127.0.0.1/live/test
```

或实际 DJI 流：

```text
rtmp://你的IP/live/<机库SN>-<镜头索引>
```

使用 OpenCV `VideoCapture` 或 FFmpeg 解码后写盘或送入算法即可；**不必**在浏览器中播放。

---

## 8. 外场网络提示

- 设备必须能 **访问** `mediamtx` 所在主机的 **IP:1935**（同网 WiFi、热点或公网 + 端口放行等）。
- 若笔记本与机场不在同一二层网络，仅填内网 IP 可能无法推流，需公网映射、VPN 或云上 RTMP 等方案。

---

## 参考

- [MediaMTX 项目](https://github.com/bluenviron/mediamtx)
- [DJI 上云 API - Pilot 直播说明](https://developer.dji.com/doc/cloud-api-tutorial/cn/feature-set/pilot-feature-set/pilot-livestream.html)
