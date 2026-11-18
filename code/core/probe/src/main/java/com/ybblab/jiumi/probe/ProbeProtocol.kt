// SPDX-FileCopyrightText: 2026 ybblab
// SPDX-License-Identifier: Apache-2.0

package com.ybblab.jiumi.probe

/** Contract shared by the app and the shell-identity monitor started from the same APK. */
object ProbeProtocol {
    // Do not reuse an older monitor that still falls back to suspension when disabling fails.
    const val VERSION = 15
    const val PROCESS_UNKNOWN = -1
    const val PROCESS_ABSENT = 0
    const val PROCESS_PRESENT = 1
    const val SHELL_UID = 2000
    const val MAIN_CLASS = "com.ybblab.jiumi.probe.ProbeMain"
    const val PROCESS_NAME = "jiumi_probe"
    const val ACTION_READY = "com.ybblab.jiumi.action.PROBE_READY"
    /** Only holders of DUMP (the shell) may deliver the monitor binder. */
    const val SENDER_PERMISSION = "android.permission.DUMP"
    const val EXTRA_BINDER = "probe_binder"
    const val EXTRA_UID = "probe_uid"
    const val EXTRA_PROTOCOL = "probe_protocol"
    const val EXTRA_SOURCE = "probe_source"
    const val EXTRA_UPDATED_AT = "probe_updated_at"

    fun dataDirectory(packageName: String): String = "/data/local/tmp/$packageName"
}
