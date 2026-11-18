<div align="center">

<img src="code/images/jiumi-icon.png" alt="揪咪图标" width="80" />

# 揪咪 Jiumi

一款轻量的 Android 应用行为监测与管理工具。
<br>无需 Root，也无需电脑，记录保存在本机。

**[下载 APK](https://github.com/ybblab/Jiumi/releases/latest)** · [使用指南](https://jiumi.ybblab.com/download/) · [官网](https://jiumi.ybblab.com) · [English](README.en.md)

<sub>Android 11 及以上 · 安装包约 4 MB · <a href="LICENSE">Apache 2.0</a></sub>

</div>

## 界面速览

<p align="center">
  <a href="code/images/monitor.zh.png"><img src="code/images/monitor.zh.png" width="132" alt="监测概览，显示监测状态和需要关注的行为" /></a>&nbsp;
  <a href="code/images/timeline.zh.png"><img src="code/images/timeline.zh.png" width="132" alt="时间线，按时间查看应用行为和状态" /></a>&nbsp;
  <a href="code/images/by-type.zh.png"><img src="code/images/by-type.zh.png" width="132" alt="按类型，查看各类行为涉及的应用和次数" /></a>&nbsp;
  <a href="code/images/apps.zh.png"><img src="code/images/apps.zh.png" width="132" alt="应用，按来源查看及筛选应用" /></a>
</p>

## 功能

| 功能 | 可以看到或做到什么 |
| --- | --- |
| 行为记录 | 按时间、应用或类型查看位置、剪贴板、设备标识等 **12 类数据、56 种访问**，也可以筛选后台记录。 |
| 需要关注 | 集中查看后台发送数据、后台读取敏感数据、短时间内频繁访问和反复被拒的尝试。 |
| 网络活动 | 按应用查看连接地址和可用的流量统计。开启域名记录后，可显示关联到的域名。 |
| 系统历史 | 在应用详情查看 Android 保留的权限访问明细和累计次数，包括开始监测前的部分行为。 |
| 日志导出 | 在「设置 › 导出日志」将选定时段的记录导出为包含 CSV 的 ZIP，供保存和分析。 |

## 停用、删除与恢复应用

「应用」页按来源整理应用。在应用详情里查看行为记录，再决定要不要改动。所有管理操作都由你发起。

<p align="center">
  <a href="code/images/app-disable.zh.png"><img src="code/images/app-disable.zh.png" width="250" alt="官网演示：停用应用的确认弹窗" /></a>&nbsp;
  <a href="code/images/app-delete.zh.png"><img src="code/images/app-delete.zh.png" width="250" alt="官网演示：删除应用的确认弹窗与备份说明" /></a>&nbsp;
  <a href="code/images/app-restore.zh.png"><img src="code/images/app-restore.zh.png" width="250" alt="官网演示：已删除应用的恢复和清除备份按钮" /></a>
</p>

| 操作 | 会发生什么 |
| --- | --- |
| 停用与启用 | 停用会保留应用数据，可在「应用 › 已停用」重新启用。系统拒绝停用时只提示失败，不会挂起或强制停止应用。 |
| 强制停止 | 对符合条件的应用可单独执行。停止应用，但不会将其停用；失败时才提示。 |
| 删除自己安装的应用 | 输入完整包名确认后，揪咪先备份安装包再卸载。之后可在「应用 › 已删除」尝试装回。**应用数据会随卸载删除，备份只能恢复安装包。** |
| 移除预装应用 | 在「设置 › 高级」开启后，可为当前用户移除受支持的预装应用。系统中的原件和应用数据会保留，可在「应用 › 已删除」尝试恢复。 |

核心系统组件默认受保护。改动手机依赖的组件可能影响系统功能，应用详情会提示风险。

## 不用 Root，怎么看到后台？

Android 在应用运行时会产生权限访问记录、连接表、系统跟踪和事件日志。揪咪探针以 **shell 身份**读取可用的系统信号，App 再按规则整理成记录，存入手机上的数据库。监测过程不会向其他应用注入代码。停用和删除等操作需要你在应用管理中主动发起。

<p align="center"><img src="code/images/how-it-works.zh.png" alt="应用行为产生 Android 系统信号，揪咪探针以 shell 身份读取，揪咪 App 整理并保存在本机。可选的域名记录通过本机 VPN 接收 DNS 查询。" width="900" /></p>

| 信号来源 | 揪咪能看到什么 |
| --- | --- |
| 权限访问记录（AppOps） | 哪个应用在什么时候使用了哪项权限，以及系统保留的历史。 |
| 连接表 | 默认每 5 秒读取一次，查看应用连接了哪些地址。 |
| 系统跟踪（Perfetto） | 可识别的系统服务调用，以及系统支持时的按连接流量。 |
| 事件日志 | 进程何时被拉起，以及系统记录的启动原因。 |

**无线调试、Root、Shizuku 三选一**，探针最终都以 shell 身份运行。无线调试可以在手机上完成配对。Root 用来切换到 shell 身份。使用 Shizuku 时，需要它已通过 ADB 以 shell 身份运行，并授权揪咪。

**域名记录（可选）**使用本机 VPN 接收 DNS 查询，并原样转发给当前网络的 DNS 服务器。其他流量照常直连。开启时会占用系统的 VPN 位置，应用自带的加密 DNS 或指定服务商的私人 DNS 可能绕过记录。

## 下载与开始监测

1. 在 Android 11 或更高版本的手机上，从 [GitHub Releases](https://github.com/ybblab/Jiumi/releases/latest) 下载并安装 APK。
2. 首次打开时选择启动方式。推荐使用无线调试，在连接 Wi-Fi 后按引导允许通知、开启开发者选项，并在揪咪的通知里输入系统显示的 6 位配对码。
3. 到「监测」页点「开始监测」。监测期间通知栏会显示常驻通知，可以从通知或 App 停止监测。

手机重启后，在揪咪点「恢复」可以继续同一次监测。无线调试已配对且仍可用时，连上 Wi-Fi 即可重新连接。需要在重启前断开 Wi-Fi 后也能启动或恢复监测，可使用 ADB 持久化，这需要同时开启 USB 调试并完成首次连接。详细操作见[使用指南](https://jiumi.ybblab.com/download/)。

## 隐私与使用边界

- **记录留在本机。** 揪咪没有账号，也不上传监测记录。App 可从 GitHub 检查更新，选择更新后会下载安装包并校验大小、SHA-256 和签名。更新检查可关闭。
- **记录行为和连接信息。** 揪咪可以显示某项权限被使用或访问被拒绝，无法看到聊天内容、密码或网络传输正文。
- **能看到的范围取决于系统。** 短连接可能在两次读取之间结束，直接读取文件等行为可能没有可用信号。按连接统计流量需要 Android 14 或更高版本及系统支持。为减少误报，揪咪会略去部分无法确认的行为。
- **监测时会增加耗电。** 耗电量取决于应用活动，不需要时可从通知停止监测。

## 从源码构建

需要 JDK 17 和 Android SDK。

```sh
cd code
./gradlew :app:assembleDebug
```

监测分类、提醒和受保护应用的规则位于 [rules 目录](code/app/src/main/assets/rules/)。代码结构与贡献流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 贡献与许可

欢迎提交 Issue 和 Pull Request。安全问题请按 [SECURITY.md](SECURITY.md) 私下报告。

项目采用 [Apache License 2.0](LICENSE)，第三方组件及其许可见 [NOTICE](NOTICE)。请在你拥有或已获授权的设备上使用。

## Star 历史

<a href="https://www.star-history.com/#ybblab/Jiumi&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=ybblab/Jiumi&amp;type=Date&amp;theme=dark" />
    <img alt="揪咪的 GitHub Star 趋势图" src="https://api.star-history.com/svg?repos=ybblab/Jiumi&amp;type=Date" />
  </picture>
</a>

<div align="center">

**如果揪咪帮你看清了手机里的应用在做什么，欢迎点个 ⭐ 支持我们！**

</div>
