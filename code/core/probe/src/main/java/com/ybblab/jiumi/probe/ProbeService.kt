// SPDX-FileCopyrightText: 2026 ybblab
// SPDX-License-Identifier: Apache-2.0

package com.ybblab.jiumi.probe

import android.app.ActivityManager
import android.content.Context
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal class ProbeService(
    private val context: Context,
    private val packageName: String,
    private val ownerUid: Int,
    private val dataDirectory: File,
    private val spool: EventSpool,
    private val accessWatcher: AccessWatcher,
    private val tracer: PerfettoTracer,
    private val connectionWatcher: ConnectionWatcher,
    private val onDetached: () -> Unit
) : IProbeService.Stub() {
    private val startedAt = SystemClock.elapsedRealtime()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val configFile = File(dataDirectory, "config.json")
    @Volatile private var attachedToken: IBinder? = null
    @Volatile private var watchedOps: Set<String> = emptySet()

    val isAttached: Boolean get() = attachedToken?.isBinderAlive == true

    override fun protocolVersion(): Int = ProbeProtocol.VERSION

    override fun attach(token: IBinder) {
        enforceOwner()
        attachedToken = token
        runCatching {
            token.linkToDeath({
                if (attachedToken === token) {
                    attachedToken = null
                    mainHandler.post(onDetached)
                }
            }, 0)
        }.onFailure {
            attachedToken = null
            mainHandler.post(onDetached)
        }
    }

    override fun configure(config: String) {
        enforceOwner()
        val parsed = JSONObject(config)
        configFile.writeText(parsed.toString())
        apply(parsed)
    }

    /** The monitor keeps watching after restarts with the last configuration it was given. */
    fun restoreConfiguration() {
        runCatching { apply(JSONObject(configFile.readText())) }
    }

    private fun apply(config: JSONObject) {
        // 0 keeps everything until the app reads it, short of filling the phone.
        config.optJSONObject("spool")?.let { spool.limitBytes = it.optLong("limitMb", 256) * 1024 * 1024 }
        val ops = config.optJSONArray("ops") ?: JSONArray()
        val list = (0 until ops.length()).map { ops.getString(it) }
        watchedOps = list.toSet()
        accessWatcher.watch(list)
        val calls = config.optJSONObject("calls")
        val methods = calls?.optJSONArray("methods") ?: JSONArray()
        val network = config.optJSONObject("network")
        connectionWatcher.configure(network?.optBoolean("enabled") == true, network?.optLong("intervalMs") ?: 0L)
        tracer.configure(
            calls = calls?.optBoolean("enabled") == true,
            selected = (0 until methods.length()).map { methods.getString(it) },
            providerOperations = calls?.optJSONObject("providerOperations")?.let { o ->
                o.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to o.getString(k) } }.toMap()
            }.orEmpty(),
            packets = network?.optBoolean("enabled") == true && network.optBoolean("packets")
        )
    }

    override fun readEvents(afterSeq: Long, max: Int): String {
        enforceOwner()
        return spool.read(afterSeq, max.coerceIn(1, MAX_EVENTS_PER_READ)).toString()
    }

    override fun readDiscreteHistory(fromTime: Long, max: Int): String {
        enforceOwner()
        val dump = Shell.run(listOf("dumpsys", "appops", "--include-discrete", "0"), timeoutMs = 30_000).output
        return DiscreteHistory.parse(dump, watchedOps, fromTime, max.coerceIn(1, MAX_HISTORY_PER_READ)).toString()
    }

    override fun readHistoricalOps(sinceTime: Long, skip: Int, max: Int): String {
        enforceOwner()
        val dump = Shell.run(listOf("dumpsys", "appops", "--history"), timeoutMs = 30_000).output
        val slots = AppOpsHistory.parse(dump, watchedOps, sinceTime, System.currentTimeMillis(), ::isMakerOp)
        val page = org.json.JSONArray()
        for (i in skip.coerceAtLeast(0) until minOf(slots.length(), skip.coerceAtLeast(0) + max.coerceIn(1, MAX_HISTORY_SLOTS))) page.put(slots.get(i))
        return page.toString()
    }

    /**
     * An operation the maker added: the platform does not know it, or numbers it apart from its own
     * (makers that register theirs start far above the platform's hundred-odd).
     */
    private fun isMakerOp(op: String): Boolean {
        val code = runCatching {
            android.app.AppOpsManager::class.java.getMethod("strOpToOp", String::class.java).invoke(null, op) as Int
        }.getOrDefault(-1)
        return code < 0 || code >= FIRST_MAKER_OP
    }

    override fun footprint(): String {
        enforceOwner()
        val status = runCatching { File("/proc/self/status").readLines() }.getOrDefault(emptyList())
        // Proportional memory counts shared framework pages fairly; RSS would count them in full.
        val rollup = runCatching { File("/proc/self/smaps_rollup").readLines() }.getOrDefault(emptyList())
        fun kb(lines: List<String>, key: String) = lines.firstOrNull { it.startsWith("$key:") }
            ?.filter { it.isDigit() }?.toLongOrNull() ?: -1L
        val stat = runCatching { File("/proc/self/stat").readText().substringAfterLast(')').trim().split(' ') }
            .getOrDefault(emptyList())
        // utime and stime are fields 14 and 15 of /proc/<pid>/stat, i.e. 11 and 12 after the name.
        val cpuTicks = (stat.getOrNull(11)?.toLongOrNull() ?: 0L) + (stat.getOrNull(12)?.toLongOrNull() ?: 0L)
        return JSONObject()
            .put("pid", Process.myPid())
            .put("pssKb", kb(rollup, "Pss"))
            .put("privateDirtyKb", kb(rollup, "Private_Dirty"))
            .put("threads", kb(status, "Threads"))
            .put("cpuMs", cpuTicks * 10)
            .put("uptimeMs", SystemClock.elapsedRealtime() - startedAt)
            .put("spoolBytes", spool.sizeBytes())
            .toString()
    }

    override fun shutdown() {
        enforceOwner()
        ProbeMain.recordState("stop", "requested")
        mainHandler.postDelayed({ ProbeMain.exit(0, "shutdown requested") }, 100)
    }

    private val appAccess by lazy { AppAccess(context, dataDirectory, packageName) }

    override fun appAccess(packageName: String, uid: Int, action: String, permission: String): String {
        enforceOwner()
        require(PACKAGE_NAME.matches(packageName))
        val user = Binder.getCallingUid() / PER_USER_RANGE
        val token = Binder.clearCallingIdentity()
        return try {
            appAccess.run(packageName, uid, user, action, permission).toString()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    override fun packageProcessState(packageName: String): Int {
        enforceOwner()
        require(PACKAGE_NAME.matches(packageName)) { "not a package name" }
        val user = Binder.getCallingUid() / PER_USER_RANGE
        val token = Binder.clearCallingIdentity()
        val processes = try {
            runCatching { context.getSystemService(ActivityManager::class.java)?.runningAppProcesses }.getOrNull()
        } finally {
            Binder.restoreCallingIdentity(token)
        } ?: return ProbeProtocol.PROCESS_UNKNOWN
        // Use Android's package membership, not UID or process name alone (shared UIDs and :services).
        if (processes.any { it.uid / PER_USER_RANGE == user && it.pkgList?.contains(packageName) == true }) {
            return ProbeProtocol.PROCESS_PRESENT
        }
        // If even the app making this request is invisible, don't turn a limited snapshot into "not running".
        return if (processes.any { it.uid == ownerUid && it.pkgList?.contains(this.packageName) == true }) {
            ProbeProtocol.PROCESS_ABSENT
        } else ProbeProtocol.PROCESS_UNKNOWN
    }

    override fun managePackage(action: String, packageName: String): String {
        enforceOwner()
        // The app decides what may be changed; the monitor only refuses what is never a target.
        require(PACKAGE_NAME.matches(packageName)) { "not a package name" }
        require(packageName != this.packageName) { "the monitor does not change its own app" }
        val user = Binder.getCallingUid() / PER_USER_RANGE
        val command = when (action) {
            "disable" -> return disable(user, packageName)
            "enable" -> return enable(user, packageName)
            "force_stop" -> return forceStop(user, packageName)
            "uninstall" -> listOf("pm", "uninstall", packageName)
            "remove" -> listOf("pm", "uninstall", "-k", "--user", "$user", packageName)
            "restore" -> listOf("cmd", "package", "install-existing", "--user", "$user", packageName)
            "reinstall" -> return reinstall(user, packageName).let { (ok, output) ->
                JSONObject().put("ok", ok).put("output", output.take(MAX_OUTPUT)).toString()
            }
            else -> throw IllegalArgumentException("unknown action $action")
        }
        val (ok, output) = runPm(command)
        return manageResult(ok, output)
    }

    private fun manageResult(ok: Boolean, output: String) =
        JSONObject().put("ok", ok).put("output", output.take(MAX_OUTPUT)).toString()

    /** Runs a package command; pm prints "Success" or the new state, and a failure exits non-zero or names it. */
    private fun runPm(command: List<String>): Pair<Boolean, String> {
        val result = runCatching { Shell.run(command, timeoutMs = 60_000) }
        val output = result.getOrNull()?.output?.trim() ?: result.exceptionOrNull()?.message.orEmpty()
        val ok = result.getOrNull()?.let {
            it.exitCode == 0 && !it.output.contains("Failure") && !it.output.contains("Error") && !it.output.contains("Exception")
        } == true
        return ok to output
    }

    private fun disable(user: Int, pkg: String): String {
        val (ok, output) = runPm(listOf("pm", "disable-user", "--user", "$user", pkg))
        val disabled = output.lineSequence().any { it.trim() == "Package $pkg new state: disabled-user" }
        return manageResult(ok && disabled, output)
    }

    private fun forceStop(user: Int, pkg: String): String {
        val (ok, output) = runPm(listOf("am", "force-stop", "--user", "$user", pkg))
        if (!ok) return manageResult(false, output)
        // Activity Manager can return no error even when it leaves a protected app alone.
        val dump = runCatching { Shell.run(listOf("dumpsys", "package", pkg)) }.getOrNull()
        val state = dump?.takeIf { it.exitCode == 0 }?.output?.lineSequence()?.map(String::trim)
            ?.dropWhile { !it.startsWith("Package [$pkg] (") }?.drop(1)
            ?.takeWhile { !it.startsWith("Package [") }
            ?.firstOrNull { it.startsWith("User $user: ") && " installed=true " in it }
        val stopped = state?.split(' ')?.contains("stopped=true") == true
        return manageResult(stopped, if (stopped) output else "Could not verify the package's stopped state")
    }

    /** Lifts a legacy Jiumi suspension, then enables the app if it was disabled. */
    private fun enable(user: Int, pkg: String): String {
        val (_, lifted) = runPm(listOf("pm", "unsuspend", "--user", "$user", pkg))
        val disabled = Shell.run(listOf("pm", "list", "packages", "-d", "--user", "$user", pkg), timeoutMs = 30_000).output
            .lineSequence().any { it.trim() == "package:$pkg" }
        if (!disabled) return manageResult(lifted.contains("suspended state: false"), lifted)
        val (ok, output) = runPm(listOf("pm", "enable", "--user", "$user", pkg))
        return manageResult(ok, output)
    }

    override fun alarmWakeups(packageName: String): String {
        enforceOwner()
        require(PACKAGE_NAME.matches(packageName)) { "not a package name" }
        val user = Binder.getCallingUid() / PER_USER_RANGE
        val dump = Shell.run(listOf("dumpsys", "alarm"), timeoutMs = 30_000).output
        return (AlarmWakeups.parse(dump, packageName, user) ?: JSONObject())
            .put("sinceBootMs", SystemClock.elapsedRealtime()).toString()
    }

    override fun locationProviders(): String {
        enforceOwner()
        return LocationProviders.parse(Shell.run(listOf("dumpsys", "location"), timeoutMs = 30_000).output).toString()
    }

    override fun listPackages(): String {
        enforceOwner()
        val user = Binder.getCallingUid() / PER_USER_RANGE
        // "package:<name> uid:<uid>" per line; -u adds packages removed for the user but kept on the phone.
        // "package:<name> installer=<pkg> uid:<uid>" per line; -i adds the installer, -u packages removed
        // for the user but kept on the phone.
        val installers = HashMap<String, String>()
        fun list(vararg flags: String): Map<String, Int> =
            Shell.run(listOf("pm", "list", "packages", "-U", "--user", "$user") + flags, timeoutMs = 30_000).output
                .lineSequence().mapNotNull { line ->
                    val name = line.substringAfter("package:", "").substringBefore(' ').trim()
                    val uid = line.substringAfter("uid:", "").trim().substringBefore(',').substringBefore(' ').toIntOrNull() ?: -1
                    if ("installer=" in line) {
                        val installer = line.substringAfter("installer=").substringBefore(' ').trim()
                        if (installer.isNotEmpty() && installer != "null") synchronized(installers) { installers[name] = installer }
                    }
                    name.takeIf { PACKAGE_NAME.matches(it) }?.let { it to uid }
                }.toMap()
        // Each list is its own shell command; run side by side they take as long as the slowest one.
        // The packages with an icon in the launcher, which some phones hide from the app itself.
        val launcherFuture = java.util.concurrent.CompletableFuture.supplyAsync {
            runCatching {
                Shell.run(listOf("cmd", "package", "query-activities", "--components", "--user", "$user",
                    "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER"), timeoutMs = 30_000).output
                    .lineSequence().map { it.substringBefore('/').trim() }.filter { PACKAGE_NAME.matches(it) }.toSet()
            }.getOrNull()
        }
        // When each package was first installed: the phone maker's preloads in the data partition carry
        // the time of the first boot, as the system itself does; anything the user added comes later.
        val dumpFuture = java.util.concurrent.CompletableFuture.supplyAsync {
            runCatching { packageDump(Shell.run(listOf("dumpsys", "package", "packages"), timeoutMs = 30_000).output, user) }.getOrNull()
        }
        val lists = listOf(arrayOf("-u", "-i"), emptyArray(), arrayOf("-s", "-u"), arrayOf("-d"))
            .map { flags -> java.util.concurrent.CompletableFuture.supplyAsync { list(*flags) } }
            .map { it.join() }
        val launcher = launcherFuture.join()
        val dump = dumpFuture.join()
        val firstInstall = dump?.firstInstall
        val suspended = dump?.suspendedByShell.orEmpty()
        val firstBoot = firstInstall?.get("android")
        val all = lists[0]
        val installed = lists[1].keys
        val system = lists[2].keys
        val disabled = lists[3].keys
        val packages = JSONArray()
        all.forEach { (pkg, uid) ->
            packages.put(JSONObject().put("pkg", pkg).put("uid", uid).put("system", pkg in system)
                .put("disabled", pkg in disabled).put("suspended", pkg in suspended).put("installed", pkg in installed)
                .apply { installers[pkg]?.let { put("installer", it) }; launcher?.let { put("launcher", pkg in it) }
                    if (firstBoot != null) firstInstall[pkg]?.let { put("factory", it - firstBoot in 0..FACTORY_WINDOW_MS) } })
        }
        return JSONObject().put("packages", packages).toString()
    }

    private class PackageDump(val firstInstall: Map<String, Long>, val suspendedByShell: Set<String>)

    /**
     * From `dumpsys package packages`: each package's first install time in ms (the first user's when
     * several), and the packages the shell suspended for [user], including legacy Jiumi suspensions.
     */
    private fun packageDump(dump: String, user: Int): PackageDump {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        val times = HashMap<String, Long>()
        val suspended = HashSet<String>()
        var current: String? = null
        var suspendedForUser = false
        for (line in dump.lineSequence()) {
            val t = line.trimStart()
            when {
                t.startsWith("Package [") -> { current = t.substringAfter('[').substringBefore(']'); suspendedForUser = false }
                // Each user's state line; later "User n:" lines list permissions.
                t.startsWith("User ") && " installed=" in t -> suspendedForUser = t.startsWith("User $user:") && " suspended=true" in t
                t.startsWith("firstInstallTime=") -> current?.let { pkg ->
                    if (pkg !in times) runCatching { format.parse(t.substringAfter('=').trim())?.time }.getOrNull()?.let { times[pkg] = it }
                }
                t.startsWith("suspendingPackage=") && suspendedForUser && "com.android.shell" in t -> current?.let { suspended += it }
            }
        }
        return PackageDump(times, suspended)
    }

    /**
     * Installs the APKs the app backed up in its external files. The system server cannot open files
     * on shared storage, so they are streamed to an install session through the shell instead.
     */
    private fun reinstall(user: Int, pkg: String): Pair<Boolean, String> {
        val dir = File("/storage/emulated/$user/Android/data/$packageName/files/backups/$pkg")
        val apks = dir.listFiles { f -> f.isFile && f.name.endsWith(".apk") }.orEmpty().sortedBy { it.name }
        if (apks.isEmpty()) return false to "no backup"
        val created = Shell.run(listOf("pm", "install-create", "-r", "--user", "$user")).output
        val session = Regex("\\[(\\d+)]").find(created)?.groupValues?.get(1) ?: return false to created
        for (apk in apks) {
            val written = runCatching {
                Shell.run(listOf("pm", "install-write", "-S", "${apk.length()}", session, apk.name, "-"), timeoutMs = 120_000, stdinFile = apk)
            }.getOrNull()
            if (written == null || !written.output.contains("Success")) {
                runCatching { Shell.run(listOf("pm", "install-abandon", session)) }
                return false to (written?.output ?: "write failed")
            }
        }
        val committed = runCatching { Shell.run(listOf("pm", "install-commit", session), timeoutMs = 120_000) }.getOrNull()
        return (committed?.output?.contains("Success") == true) to committed?.output.orEmpty()
    }

    private fun enforceOwner() {
        if (Binder.getCallingUid() != ownerUid) throw SecurityException("caller is not the owning app")
    }

    private companion object {
        const val MAX_EVENTS_PER_READ = 500
        const val MAX_HISTORY_PER_READ = 2_000
        const val MAX_HISTORY_SLOTS = 8
        const val FIRST_MAKER_OP = 1_000
        const val PER_USER_RANGE = 100_000
        const val MAX_OUTPUT = 2_000
        /** A package first installed this soon after the system itself came with the phone. */
        const val FACTORY_WINDOW_MS = 5 * 60_000L
        val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    }
}
