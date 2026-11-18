<div align="center">

<img src="code/images/jiumi-icon.png" alt="Jiumi icon" width="80" />

# Jiumi 揪咪

A lightweight Android app behavior monitor and manager.
<br>No root or computer needed. Records stay on your phone.

**[Download APK](https://github.com/ybblab/Jiumi/releases/latest)** · [Guide](https://jiumi.ybblab.com/en/download/) · [Website](https://jiumi.ybblab.com/en/) · [简体中文](README.md)

<sub>Android 11 and later · APK about 4 MB · <a href="LICENSE">Apache 2.0</a></sub>

</div>

## A quick look at the app

<p align="center">
  <a href="code/images/monitor.en.png"><img src="code/images/monitor.en.png" width="132" alt="Monitor overview with monitoring status and activity that needs attention" /></a>&nbsp;
  <a href="code/images/timeline.en.png"><img src="code/images/timeline.en.png" width="132" alt="Timeline of app activity and status" /></a>&nbsp;
  <a href="code/images/by-type.en.png"><img src="code/images/by-type.en.png" width="132" alt="Activity grouped by type with app and record counts" /></a>&nbsp;
  <a href="code/images/apps.en.png"><img src="code/images/apps.en.png" width="132" alt="App list grouped by installation source" /></a>
</p>

## Features

| Feature | What you can do |
| --- | --- |
| Activity records | Browse **56 types of access in 12 groups**, including location, clipboard and device identifiers. View records by time, app or type, and filter for background activity. |
| Needs attention | Review background transfers, sensitive access in the background, frequent access and repeated denied attempts. |
| Network activity | See connection addresses and available traffic measurements by app. With domain recording enabled, matched connections can show a domain. |
| System history | Open an app's details to see the permission access records and totals Android retains, including some activity from before monitoring began. |
| Log export | In Settings › Export logs, save records for a selected time range as a ZIP containing CSV files. |

## Disable, delete and restore apps

Apps are grouped by source. Open an app's details to review its activity and decide whether to make a change. App management actions start with you.

<p align="center">
  <a href="code/images/app-disable.en.png"><img src="code/images/app-disable.en.png" width="250" alt="Website demo showing the confirmation to disable an app" /></a>&nbsp;
  <a href="code/images/app-delete.en.png"><img src="code/images/app-delete.en.png" width="250" alt="Website demo showing the delete confirmation and backup notice" /></a>&nbsp;
  <a href="code/images/app-restore.en.png"><img src="code/images/app-restore.en.png" width="250" alt="Website demo showing Restore and Discard backup for a deleted app" /></a>
</p>

| Action | What happens |
| --- | --- |
| Disable or enable | Disabling keeps the app's data. You can enable it again under Apps › Disabled. If the system refuses to disable it, Jiumi reports failure without suspending or force stopping the app. |
| Force stop | Available as a separate action for eligible apps. It stops the app without disabling it and only shows a notice on failure. |
| Delete an app you installed | Enter the full package name to confirm. Jiumi backs up the APKs before uninstalling. You can try to reinstall the app from Apps › Deleted. **Uninstalling deletes app data. The backup can only restore the installation files.** |
| Remove a preinstalled app | Enable the option in advanced settings to remove supported preinstalled apps for the current user. The system copy and app data are kept, and you can try to restore the app from Apps › Deleted. |

Core system components are protected by default. Changing a component the phone depends on can affect system features. The app details show a warning.

## How does Jiumi see background activity without root?

Android provides permission access records, connection tables, system traces and event logs as apps run. The Jiumi monitor reads available system signals as the **shell user**. The app organizes them by rules and stores them in a database on your phone. Monitoring does not inject code into other apps. Actions such as disabling or deleting require you to start them in app management.

<p align="center"><img src="code/images/how-it-works.en.png" alt="Apps generate Android system signals. The Jiumi monitor reads them as the shell user, and the app organizes and stores the records on the phone. Optional domain recording takes DNS queries through a local VPN." width="900" /></p>

| Signal source | What Jiumi can see |
| --- | --- |
| Permission access records (AppOps) | Which app used a permission and when, along with the history Android retains. |
| Connection tables | Which addresses apps connected to, sampled every five seconds by default. |
| System traces (Perfetto) | Identifiable system service calls and per-connection traffic where the system supports it. |
| Event logs | When a process started and the reason Android recorded. |

**Choose wireless debugging, root or Shizuku.** The monitor runs as the shell user with all three. Wireless debugging pairs on the phone itself. The root option switches to the shell user. The Shizuku option needs Shizuku running as the shell user via ADB, with permission granted to Jiumi.

**Optional domain recording** uses a local VPN to receive DNS queries and forward them unchanged to the current network's DNS servers. Other traffic goes directly to its destination. Domain recording occupies the system VPN slot, and queries using an app's own encrypted DNS or a named Private DNS provider may bypass it.

## Download and start monitoring

1. On a phone running Android 11 or later, download and install the APK from [GitHub Releases](https://github.com/ybblab/Jiumi/releases/latest).
2. Choose a launch method on first opening Jiumi. For wireless debugging, the recommended method, connect to Wi-Fi and follow the setup to allow notifications, turn on Developer options and enter the six-digit system pairing code in Jiumi's notification.
3. Tap Start monitoring on the Monitor page. A persistent notification appears while monitoring is running. You can stop monitoring from the notification or the app.

After a phone restart, tap Resume to continue the same session. With wireless debugging available and pairing still valid, reconnect over Wi-Fi. ADB persistence can let you start or resume offline until the next restart, provided USB debugging is enabled and the initial connection has succeeded. See the [setup guide](https://jiumi.ybblab.com/en/download/) for details.

## Privacy and limitations

- **Records stay on your phone.** Jiumi has no accounts and does not upload monitoring records. The app can check GitHub for updates. If you choose to update, it downloads the APK and verifies its size, SHA-256 and signature. You can turn off update checks.
- **Records describe activity and connections.** Jiumi can show that a permission was used or an access was denied. It cannot see messages, passwords or the contents of network transfers.
- **Coverage depends on the system.** Short connections can end between samples, and actions such as direct file reads may produce no usable signal. Per-connection traffic requires Android 14 or later and system support. Jiumi leaves out some activity it cannot confirm to reduce false reports.
- **Monitoring uses extra battery.** The amount depends on app activity. Stop monitoring from the notification when you no longer need it.

## Build from source

Requires JDK 17 and the Android SDK.

```sh
cd code
./gradlew :app:assembleDebug
```

Monitoring categories, attention rules and protected apps are configured in the [rules directory](code/app/src/main/assets/rules/). See [CONTRIBUTING.md](CONTRIBUTING.md) for the code layout and contribution process.

## Contributing and license

Issues and pull requests are welcome. Report security issues privately as described in [SECURITY.md](SECURITY.md).

Jiumi is licensed under the [Apache License 2.0](LICENSE). Third-party components and their licenses are listed in [NOTICE](NOTICE). Use Jiumi on devices you own or are authorized to use.

## Star history

<a href="https://www.star-history.com/#ybblab/Jiumi&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=ybblab/Jiumi&amp;type=Date&amp;theme=dark" />
    <img alt="Jiumi GitHub star history chart" src="https://api.star-history.com/svg?repos=ybblab/Jiumi&amp;type=Date" />
  </picture>
</a>

<div align="center">

**If Jiumi helps you see what your apps are doing, give us a ⭐ on GitHub!**

</div>
