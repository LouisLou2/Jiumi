// SPDX-FileCopyrightText: 2026 ybblab
// SPDX-License-Identifier: Apache-2.0

package com.ybblab.jiumi.probe;

/** Shell-identity monitor. Every method except protocolVersion accepts only the owning app. */
interface IProbeService {
    int protocolVersion();
    /** The monitor re-offers itself after this token dies and the app process starts again. */
    void attach(IBinder token);
    /** JSON {"ops":[...],"calls":{...},"network":{...},"spool":{"limitMb":n}}. Kept across monitor restarts. */
    void configure(String config);
    /**
     * JSON {"events":[...],"last":seq,"first":seq,"more":bool}: spooled events after afterSeq, oldest
     * first. Reading also releases the events at or before afterSeq; "first" is the oldest still kept.
     */
    String readEvents(long afterSeq, int max);
    /**
     * The system's per-access record (the one behind its privacy dashboard; a few kinds of data, kept
     * about a week) for the operations being watched, at or after fromTime, oldest first, at most max.
     * JSON array of {"t","end","op","uid","pkg","attr","state","via"}.
     */
    String readDiscreteHistory(long fromTime, int max);
    /**
     * The system's long-term access counts for the operations being watched: its time slots that ended
     * after sinceTime, newest first, skipping the first skip and returning at most max. JSON array of
     * {"from","to","ops":[{"uid","pkg","op","fg","bg","fgRejected","bgRejected","durationMs"}]}.
     */
    String readHistoricalOps(long sinceTime, int skip, int max);
    /** The monitor's own resource use. Returns JSON. */
    String footprint();
    void shutdown();
    /**
     * Changes an installed package for the calling user: action "disable" (no fallback), "enable"
     * (also lifts legacy suspensions), "force_stop", "uninstall" (removes it and its data), "remove"
     * (a preinstalled app, for this user only), "restore" (brings a removed preinstalled app back)
     * or "reinstall" (installs the APKs the app backed up under its external files, backups/<packageName>/).
     * Returns JSON {"ok":bool,"output":text}.
     */
    String managePackage(String action, String packageName);
    /**
     * Every package for the calling user as the shell sees it, which some phones hide from apps.
     * JSON {"packages":[{"pkg":name,"uid":n,"system":bool,"disabled":bool,"suspended":bool,"installed":bool}]};
     * "suspended" only for a suspension made by the shell (including from older versions).
     */
    String listPackages();
    /**
     * The apps the system has bound as its location providers, from its location dump. JSON array of
     * {"name":provider,"uid":n,"pkg":name}.
     */
    String locationProviders();
    /**
     * How often the calling user's app's alarms woke the phone since boot, from the alarm service.
     * JSON {"sinceBootMs":n,"wakeups":n,"receivers":[{"tag","wakes","lastAgoMs"}]}; "wakeups" and
     * "receivers" are missing when the system lists no alarm of the app.
     */
    String alarmWakeups(String packageName);
    /** Reads or changes a single installed app’s access. Returns verified state and operation results. */
    String appAccess(String packageName, int uid, String action, String permission);
    /** Snapshot for the calling user: 1 = process found, 0 = none found, -1 = unavailable. */
    int packageProcessState(String packageName);
}
