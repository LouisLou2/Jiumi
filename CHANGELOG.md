# Changelog

Notable changes to Jiumi, newest first. The English in-app update notes come from this file.
Chinese in-app notes are supplied when preparing the release.
Versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

- Disabling an app no longer falls back to suspension. Eligible apps have a separate Force stop action; both actions only show a toast on failure. The app details show whether its process is running and gray out Force stop when none is detected.

## [0.2.1]

- Streamlined setup for root and Shizuku, with clearer authorization steps.
- Refresh root and Shizuku authorization when returning to Jiumi.
- Improved navigation to Developer options for wireless and USB debugging.
- Fixed stale authorization errors after access is granted.

## [0.2.0]

- Manage permissions from each app’s details. Turn them off individually or together, and restore permissions Jiumi previously revoked.
- Block or restore network access per app on supported devices, without a VPN.
- See why a control is unavailable and check the state reported by Android when a change fails.

## [0.1.0]

The first public release.

### Added

- Monitoring through Wireless debugging, with no root: 56 kinds of personal data in 12 groups, from AppOps records and traced system-service calls, plus network connections; each record notes whether the app was in the foreground and the screen on.
- An overview of what needs a look: sensitive data accessed in the background, frequent access, repeated refused attempts, and data sent in the background (the bytes measured, with nothing inferred about why).
- Three ways to start the monitor: Wireless debugging, root (Magisk, KernelSU, APatch; used only to switch to the shell identity) or Shizuku, chosen on the first page and in Settings.
- Android's own records are read too: its per-access record (about a week, with times) and its long-term counts (about a month, every permission), so an app's page shows what happened before monitoring began and parts of the system that cannot be watched live. Permissions a phone maker added are listed apart as the maker's own.
- Connections appear within seconds of being seen, and are updated while they last.
- An app's page shows how many times its alarms woke the sleeping phone since it was last restarted.
- Monitoring picks itself up again after Jiumi is updated.
- Optional domain recording: a VPN on the phone that receives DNS queries only records the domains each app looks up, and connections show them.
- First-launch pairing guide: one card walks through allowing notifications, turning on Developer options and pairing with Wireless debugging, ticking each step off when done.
- Timeline: an app's network connections close together fold into one line, and the timeline can be narrowed to Background or Refused; Connect to the network sums up by app, each opening the domains and addresses it reached.
- Judgements lean towards missing rather than misreporting: a system call made without the permission it needs is told as refused; calls that return only the app's own information are left out; always-on system processes do not count as background; records with no known caller are not shown; a traced call is shown only when Android's own record confirms the access, since without it what the call returned cannot be told; work an app does for another app, such as a location service scanning Wi‑Fi for the app that asked, is left out.
- ADB persistence, on by default: after one connection over Wireless debugging, monitoring starts through a local channel even offline, until the phone restarts.
- App management: disable, uninstall or remove for this user, with backups to bring apps back, warnings before changing apps the phone relies on, and an expert mode for core system components. On phones that refuse to disable system apps, such as HyperOS, the app is suspended and stopped instead.
- Apps grouped by where they came from (app stores, installed directly, preinstalled, system components), with counts that jump to each group.
- Updates from the project's GitHub releases: checked on opening at most every six hours (can be turned off), offered once, and verified by size, SHA-256 and signing key before installing.
- Export of the records as a ZIP; nothing ever leaves the phone otherwise.
