package com.example.core.terminal

import com.example.core.filesystem.VirtualFilesystem
import com.example.data.local.VirtualAppDao
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VirtualTerminalEngine(
    private val fs: VirtualFilesystem,
    private val appDao: VirtualAppDao? = null
) {
    var cwd: String = "/"
        private set

    var isRoot: Boolean = true
        private set

    private val envVars = mutableMapOf(
        "USER" to "root",
        "UID" to "0",
        "GID" to "0",
        "HOME" to "/root",
        "SHELL" to "/system/bin/sh",
        "PATH" to "/system/xbin:/system/bin:/vendor/bin:/data/local/tmp",
        "ROOTBOX_VIRTUAL" to "1",
        "ROOTBOX_VERSION" to "2.4.0-vaosp",
        "TERM" to "xterm-256color",
        "HOSTNAME" to "rootbox",
        "ANDROID_DATA" to "/data",
        "ANDROID_ROOT" to "/system"
    )

    fun getPrompt(): String {
        val userStr = if (isRoot) "root@rootbox" else "shell@rootbox"
        val symbol = if (isRoot) "#" else "$"
        return "$userStr:$cwd $symbol "
    }

    suspend fun executeCommand(input: String): List<TerminalLine> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<TerminalLine>()
        results.add(TerminalLine("${getPrompt()}$trimmed", LineType.COMMAND))

        // Security check for real-device exploits / hardware root requests
        val lower = trimmed.lowercase()
        val realRootKeywords = listOf(
            "fastboot", "reboot bootloader", "reboot recovery", "insmod",
            "rmmod", "modprobe", "magisk", "supersu", "unlock_bootloader",
            "flash", "dd if=/dev/zero of=/dev/block"
        )
        if (realRootKeywords.any { lower.startsWith(it) || lower.contains(" $it") }) {
            results.add(
                TerminalLine(
                    "This feature requires real device root and is unavailable in RootBox's no-root virtual environment.",
                    LineType.ERROR
                )
            )
            return results
        }

        // Handle pipe or redirection if simple
        if (trimmed.contains(" > ") || trimmed.contains(" >> ")) {
            return handleRedirection(trimmed, results)
        }

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0]
        val args = parts.drop(1)

        when (cmd) {
            "clear" -> {
                // Return special signal
                return listOf(TerminalLine("__CLEAR_SCREEN__", LineType.SYSTEM))
            }

            "help" -> {
                results.add(TerminalLine("RootBox Virtual Android Shell (vAOSP 14)", LineType.SYSTEM))
                results.add(TerminalLine("Commands:", LineType.SYSTEM))
                results.add(TerminalLine("  id, whoami, uname -a, su, exit      : Identity & Privilege controls", LineType.OUTPUT))
                results.add(TerminalLine("  pwd, cd, ls, cat, touch, mkdir, rm  : Virtual Filesystem operations", LineType.OUTPUT))
                results.add(TerminalLine("  cp, mv, chmod, df -h, free, env     : System inspection & file utils", LineType.OUTPUT))
                results.add(TerminalLine("  ps, top, ping, getprop, setprop     : Process, network & properties", LineType.OUTPUT))
                results.add(TerminalLine("  pm [list|install|uninstall]         : Virtual Package Manager", LineType.OUTPUT))
                results.add(TerminalLine("  pkg [list|install <app>]            : RootBox App Repository", LineType.OUTPUT))
                results.add(TerminalLine("  dmesg, uptime, date, echo, clear    : Virtual system utilities", LineType.OUTPUT))
                results.add(TerminalLine("Note: Root privileges are strictly confined to the RootBox container.", LineType.WARNING))
            }

            "id" -> {
                if (isRoot) {
                    results.add(TerminalLine("uid=0(root) gid=0(root) groups=0(root),1004(input),1007(log),1015(sdcard_rw),3003(inet) context=u:r:su:s0", LineType.OUTPUT))
                } else {
                    results.add(TerminalLine("uid=2000(shell) gid=2000(shell) groups=2000(shell),1004(input),1007(log),1015(sdcard_rw),3003(inet) context=u:r:shell:s0", LineType.OUTPUT))
                }
            }

            "whoami" -> {
                results.add(TerminalLine(if (isRoot) "root" else "shell", LineType.OUTPUT))
            }

            "uname" -> {
                if (args.contains("-a")) {
                    results.add(TerminalLine("Linux rootbox 6.1.0-rootbox-vAOSP #1 SMP PREEMPT RootBox Virtual Android aarch64", LineType.OUTPUT))
                } else {
                    results.add(TerminalLine("Linux", LineType.OUTPUT))
                }
            }

            "su" -> {
                if (!isRoot) {
                    isRoot = true
                    envVars["USER"] = "root"
                    envVars["UID"] = "0"
                    envVars["GID"] = "0"
                    envVars["HOME"] = "/root"
                    results.add(TerminalLine("RootBox Virtual SU: Elevated to virtual root (uid 0).", LineType.SUCCESS))
                } else {
                    results.add(TerminalLine("Already virtual root (uid 0).", LineType.OUTPUT))
                }
            }

            "exit" -> {
                if (isRoot) {
                    isRoot = false
                    envVars["USER"] = "shell"
                    envVars["UID"] = "2000"
                    envVars["GID"] = "2000"
                    envVars["HOME"] = "/data/local/tmp"
                    results.add(TerminalLine("Dropped virtual root. Now in user shell (uid 2000).", LineType.WARNING))
                } else {
                    results.add(TerminalLine("Terminal session active in RootBox container.", LineType.OUTPUT))
                }
            }

            "pwd" -> {
                results.add(TerminalLine(cwd, LineType.OUTPUT))
            }

            "cd" -> {
                val target = if (args.isEmpty() || args[0] == "~") {
                    if (isRoot) "/root" else "/data/local/tmp"
                } else {
                    args[0]
                }
                val resolved = resolveVirtualDir(target)
                val f = fs.resolveVirtualPath(resolved)
                if (f.exists() && f.isDirectory) {
                    cwd = resolved
                } else {
                    results.add(TerminalLine("cd: $target: No such directory", LineType.ERROR))
                }
            }

            "ls" -> {
                val showAll = args.contains("-a") || args.contains("-la") || args.contains("-al")
                val showLong = args.contains("-l") || args.contains("-la") || args.contains("-al")
                val targetArg = args.firstOrNull { !it.startsWith("-") }
                val targetDir = if (targetArg != null) resolveVirtualDir(targetArg) else cwd

                val items = fs.listFiles(targetDir)
                if (items.isEmpty()) {
                    val f = fs.resolveVirtualPath(targetDir)
                    if (!f.exists()) {
                        results.add(TerminalLine("ls: $targetDir: No such file or directory", LineType.ERROR))
                    }
                } else {
                    if (showLong) {
                        results.add(TerminalLine("total ${items.size * 4}", LineType.OUTPUT))
                        for (item in items) {
                            if (!showAll && item.name.startsWith(".")) continue
                            val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(item.lastModified))
                            results.add(TerminalLine(
                                String.format("%-10s %-8s %8s %s %s", item.permissions, item.owner, item.formattedSize, dateStr, item.name),
                                if (item.isDirectory) LineType.SUCCESS else LineType.OUTPUT
                            ))
                        }
                    } else {
                        val names = items
                            .filter { showAll || !it.name.startsWith(".") }
                            .joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                        results.add(TerminalLine(names, LineType.OUTPUT))
                    }
                }
            }

            "cat" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("Usage: cat <file>", LineType.WARNING))
                } else {
                    for (fileArg in args) {
                        val vPath = resolveVirtualPath(fileArg)
                        val f = fs.resolveVirtualPath(vPath)
                        if (!f.exists()) {
                            results.add(TerminalLine("cat: $fileArg: No such file or directory", LineType.ERROR))
                        } else if (f.isDirectory) {
                            results.add(TerminalLine("cat: $fileArg: Is a directory", LineType.ERROR))
                        } else {
                            val content = fs.readFile(vPath)
                            if (content.isEmpty()) {
                                results.add(TerminalLine("<empty file>", LineType.SYSTEM))
                            } else {
                                content.lines().forEach {
                                    results.add(TerminalLine(it, LineType.OUTPUT))
                                }
                            }
                        }
                    }
                }
            }

            "echo" -> {
                val text = args.joinToString(" ")
                results.add(TerminalLine(text, LineType.OUTPUT))
            }

            "touch" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("Usage: touch <file>", LineType.WARNING))
                } else {
                    for (fileArg in args) {
                        val vPath = resolveVirtualPath(fileArg)
                        fs.writeFile(vPath, "")
                    }
                }
            }

            "mkdir" -> {
                val dirArg = args.lastOrNull { !it.startsWith("-") }
                if (dirArg == null) {
                    results.add(TerminalLine("Usage: mkdir [-p] <directory>", LineType.WARNING))
                } else {
                    val vPath = resolveVirtualPath(dirArg)
                    val success = fs.createDirectory(vPath)
                    if (!success) {
                        results.add(TerminalLine("mkdir: cannot create directory '$dirArg'", LineType.ERROR))
                    }
                }
            }

            "rm" -> {
                val pathArg = args.lastOrNull { !it.startsWith("-") }
                if (pathArg == null) {
                    results.add(TerminalLine("Usage: rm [-rf] <path>", LineType.WARNING))
                } else {
                    val vPath = resolveVirtualPath(pathArg)
                    val success = fs.delete(vPath)
                    if (!success) {
                        results.add(TerminalLine("rm: cannot remove '$pathArg'", LineType.ERROR))
                    }
                }
            }

            "cp" -> {
                if (args.size < 2) {
                    results.add(TerminalLine("Usage: cp <source> <destination>", LineType.WARNING))
                } else {
                    val src = resolveVirtualPath(args[0])
                    val dst = resolveVirtualPath(args[1])
                    val success = fs.copy(src, dst)
                    if (!success) {
                        results.add(TerminalLine("cp: cannot copy '$src' to '$dst'", LineType.ERROR))
                    }
                }
            }

            "mv" -> {
                if (args.size < 2) {
                    results.add(TerminalLine("Usage: mv <source> <destination>", LineType.WARNING))
                } else {
                    val src = resolveVirtualPath(args[0])
                    val success = fs.rename(src, args[1])
                    if (!success) {
                        results.add(TerminalLine("mv: cannot move '$src' to '${args[1]}'", LineType.ERROR))
                    }
                }
            }

            "chmod" -> {
                if (args.size < 2) {
                    results.add(TerminalLine("Usage: chmod <mode> <file>", LineType.WARNING))
                } else {
                    val mode = args[0]
                    val path = resolveVirtualPath(args[1])
                    fs.chmod(path, mode)
                    results.add(TerminalLine("Mode changed: $mode on $path", LineType.SUCCESS))
                }
            }

            "env", "printenv" -> {
                envVars.forEach { (k, v) ->
                    results.add(TerminalLine("$k=$v", LineType.OUTPUT))
                }
            }

            "export" -> {
                if (args.isEmpty()) {
                    envVars.forEach { (k, v) ->
                        results.add(TerminalLine("declare -x $k=\"$v\"", LineType.OUTPUT))
                    }
                } else {
                    for (arg in args) {
                        val pair = arg.split("=", limit = 2)
                        if (pair.size == 2) {
                            envVars[pair[0]] = pair[1]
                        }
                    }
                }
            }

            "ps" -> {
                results.add(TerminalLine("USER       PID   PPID  VSIZE  RSS   WCHAN            PC  NAME", LineType.SYSTEM))
                results.add(TerminalLine("root         1      0  12416  4128  ep_poll    00000000 S init (vAOSP)", LineType.OUTPUT))
                results.add(TerminalLine("root         2      1   8192  2216  kthreadd   00000000 S vaospd", LineType.OUTPUT))
                results.add(TerminalLine("root        10      1 145216 42100  poll       00000000 S zygote64", LineType.OUTPUT))
                results.add(TerminalLine("system      12     10 412080 120400 ep_poll    00000000 S system_server", LineType.OUTPUT))
                results.add(TerminalLine("root        18      1  18340  6240  ep_poll    00000000 S vnetd", LineType.OUTPUT))
                results.add(TerminalLine("u0_a10     105     10 182300 48200  futex      00000000 S com.termux.virtual", LineType.OUTPUT))
                results.add(TerminalLine(if (isRoot) "root       210      1  14200  5120  sys_pause  00000000 S sh (rootbox)" else "shell      210      1  14200  5120  sys_pause  00000000 S sh (rootbox)", LineType.OUTPUT))
            }

            "top" -> {
                results.add(TerminalLine("Tasks: 7 total, 1 running, 6 sleeping, 0 stopped, 0 zombie", LineType.SYSTEM))
                results.add(TerminalLine("%Cpu(s):  2.4 us,  1.1 sy,  0.0 ni, 96.2 id,  0.3 wa,  0.0 hi", LineType.SYSTEM))
                results.add(TerminalLine("MiB Mem :   3072.0 total,   1420.5 free,   1184.2 used,    467.3 buff/cache", LineType.SYSTEM))
                results.add(TerminalLine("PID  USER     PR  NI    VIRT    RES  S %CPU  %MEM     TIME+ COMMAND", LineType.SYSTEM))
                results.add(TerminalLine(" 12  system   20   0  412.1M 120.4M  S  3.8   3.9   3:45.80 system_server", LineType.OUTPUT))
                results.add(TerminalLine(" 10  root     20   0  145.2M  42.1M  S  1.4   1.4   1:20.15 zygote64", LineType.OUTPUT))
                results.add(TerminalLine("105  u0_a10   20   0  182.3M  48.2M  S  0.8   1.6   0:42.10 com.termux.virtual", LineType.OUTPUT))
                results.add(TerminalLine("  1  root     20   0   12.4M   4.1M  S  0.2   0.1   0:14.22 init", LineType.OUTPUT))
            }

            "df" -> {
                results.add(TerminalLine("Filesystem       Size  Used Avail Use% Mounted on", LineType.SYSTEM))
                results.add(TerminalLine("/dev/block/vda    16G  4.2G   11G  27% /", LineType.OUTPUT))
                results.add(TerminalLine("/dev/block/vdb     8G  1.8G  6.0G  23% /system", LineType.OUTPUT))
                results.add(TerminalLine("/dev/block/vdc    32G  8.4G   23G  27% /data", LineType.OUTPUT))
                results.add(TerminalLine("/dev/block/vdd    32G  1.2G   30G   4% /sdcard", LineType.OUTPUT))
                results.add(TerminalLine("tmpfs              2G  4.0K    2G   1% /tmp", LineType.OUTPUT))
            }

            "free" -> {
                results.add(TerminalLine("               total        used        free      shared  buff/cache   available", LineType.SYSTEM))
                results.add(TerminalLine("Mem:         3072 MB     1184 MB     1420 MB       45 MB      468 MB     1843 MB", LineType.OUTPUT))
                results.add(TerminalLine("Swap:        1024 MB        0 MB     1024 MB", LineType.OUTPUT))
            }

            "ping" -> {
                val host = args.lastOrNull { !it.startsWith("-") } ?: "1.1.1.1"
                results.add(TerminalLine("PING $host (1.1.1.1) 56(84) bytes of data.", LineType.OUTPUT))
                results.add(TerminalLine("64 bytes from 1.1.1.1: icmp_seq=1 ttl=58 time=14.2 ms", LineType.SUCCESS))
                results.add(TerminalLine("64 bytes from 1.1.1.1: icmp_seq=2 ttl=58 time=12.8 ms", LineType.SUCCESS))
                results.add(TerminalLine("64 bytes from 1.1.1.1: icmp_seq=3 ttl=58 time=13.5 ms", LineType.SUCCESS))
                results.add(TerminalLine("--- $host ping statistics ---", LineType.SYSTEM))
                results.add(TerminalLine("3 packets transmitted, 3 received, 0% packet loss, time 2004ms", LineType.OUTPUT))
                results.add(TerminalLine("rtt min/avg/max/mdev = 12.8/13.5/14.2/0.57 ms", LineType.OUTPUT))
            }

            "getprop" -> {
                val propFile = fs.resolveVirtualPath("/system/build.prop")
                if (propFile.exists()) {
                    propFile.readLines().filter { it.contains("=") && !it.startsWith("#") }.forEach {
                        val kv = it.split("=", limit = 2)
                        results.add(TerminalLine("[${kv[0]}]: [${if (kv.size > 1) kv[1] else ""}]", LineType.OUTPUT))
                    }
                } else {
                    results.add(TerminalLine("[ro.product.model]: [RootBox Virtual Android]", LineType.OUTPUT))
                    results.add(TerminalLine("[ro.build.version.release]: [14]", LineType.OUTPUT))
                }
            }

            "setprop" -> {
                if (args.size < 2) {
                    results.add(TerminalLine("Usage: setprop <key> <value>", LineType.WARNING))
                } else {
                    results.add(TerminalLine("Property [${args[0]}] set to [${args.drop(1).joinToString(" ")}]", LineType.SUCCESS))
                }
            }

            "pm" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("Package manager usage: pm [list packages|install <path>|uninstall <pkg>]", LineType.WARNING))
                } else if (args[0] == "list" && args.getOrNull(1) == "packages") {
                    val apps = appDao?.getAllApps()?.firstOrNull() ?: emptyList()
                    if (apps.isEmpty()) {
                        results.add(TerminalLine("package:com.termux.virtual", LineType.OUTPUT))
                        results.add(TerminalLine("package:stericson.busybox.virtual", LineType.OUTPUT))
                        results.add(TerminalLine("package:com.speedsoftware.rootexplorer.virtual", LineType.OUTPUT))
                    } else {
                        apps.forEach {
                            results.add(TerminalLine("package:${it.packageName}", LineType.OUTPUT))
                        }
                    }
                } else if (args[0] == "install") {
                    val apk = args.getOrNull(1)
                    if (apk == null) {
                        results.add(TerminalLine("Usage: pm install <path_to_apk>", LineType.ERROR))
                    } else {
                        results.add(TerminalLine("Success: Package $apk installed into /data/app", LineType.SUCCESS))
                    }
                } else if (args[0] == "uninstall") {
                    val pkg = args.getOrNull(1)
                    if (pkg == null) {
                        results.add(TerminalLine("Usage: pm uninstall <package_name>", LineType.ERROR))
                    } else {
                        appDao?.deleteByPackageName(pkg)
                        results.add(TerminalLine("Success: Package $pkg uninstalled from virtual container", LineType.SUCCESS))
                    }
                }
            }

            "pkg" -> {
                if (args.isEmpty() || args[0] == "list") {
                    results.add(TerminalLine("RootBox Package Repository (Available Virtual Tools):", LineType.SYSTEM))
                    results.add(TerminalLine("  busybox-pro        : Complete Unix toolset (1.36.1)", LineType.OUTPUT))
                    results.add(TerminalLine("  termux-core        : Android terminal environment (v0.118)", LineType.OUTPUT))
                    results.add(TerminalLine("  sqlite3-bin        : Standalone SQLite3 terminal CLI", LineType.OUTPUT))
                    results.add(TerminalLine("  nano-editor        : Terminal text editor", LineType.OUTPUT))
                    results.add(TerminalLine("  tcpdump-arm64      : Network packet capture utility", LineType.OUTPUT))
                    results.add(TerminalLine("  micro-httpd        : Embedded HTTP microserver", LineType.OUTPUT))
                } else if (args[0] == "install") {
                    val tool = args.getOrNull(1) ?: "package"
                    results.add(TerminalLine("Downloading $tool from RootBox Virtual Mirror...", LineType.OUTPUT))
                    results.add(TerminalLine("Unpacking binary into /system/xbin/$tool", LineType.OUTPUT))
                    results.add(TerminalLine("Setting permissions: 0755 root:root", LineType.OUTPUT))
                    results.add(TerminalLine("Package $tool installed successfully.", LineType.SUCCESS))
                }
            }

            "dmesg" -> {
                results.add(TerminalLine("[    0.000000] Booting RootBox Virtual Linux kernel 6.1.0-rootbox-vAOSP", LineType.SYSTEM))
                results.add(TerminalLine("[    0.001420] Virtual CPU: 4 Cores allocated [ARM_v8.2_NEON]", LineType.OUTPUT))
                results.add(TerminalLine("[    0.003512] Memory: 3145728K/3145728K available", LineType.OUTPUT))
                results.add(TerminalLine("[    0.015240] RootBox vFS mounted on / [rw,noatime,relatime]", LineType.OUTPUT))
                results.add(TerminalLine("[    0.021045] Virtual network interface vnet0 initialized (192.168.100.2)", LineType.SUCCESS))
                results.add(TerminalLine("[    0.048910] Virtual root subsystem: UID 0 granted to rootbox", LineType.SUCCESS))
                results.add(TerminalLine("[    0.061002] Security Isolation: Host physical kernel boundary enforced", LineType.WARNING))
                results.add(TerminalLine("[    0.089100] init: Virtual AOSP stage 2 completed", LineType.OUTPUT))
            }

            "uptime" -> {
                results.add(TerminalLine(" 16:20:10 up 3:15,  1 user,  load average: 0.12, 0.08, 0.05", LineType.OUTPUT))
            }

            "date" -> {
                val now = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
                results.add(TerminalLine(now, LineType.OUTPUT))
            }

            else -> {
                results.add(TerminalLine("$cmd: command not found. Type 'help' for available commands.", LineType.ERROR))
            }
        }

        return results
    }

    private fun handleRedirection(command: String, results: MutableList<TerminalLine>): List<TerminalLine> {
        val isAppend = command.contains(" >> ")
        val delim = if (isAppend) " >> " else " > "
        val parts = command.split(delim, limit = 2)
        val textPart = parts[0].trim()
        val filePart = parts[1].trim()

        val textToWrite = if (textPart.startsWith("echo ")) {
            textPart.substring(5).trim('"', '\'')
        } else {
            textPart
        }

        val targetPath = resolveVirtualPath(filePart)
        val existing = if (isAppend) fs.readFile(targetPath) else ""
        val newContent = if (isAppend && existing.isNotEmpty()) "$existing\n$textToWrite" else textToWrite

        val success = fs.writeFile(targetPath, newContent)
        if (success) {
            results.add(TerminalLine("Wrote ${textToWrite.length} bytes to $filePart", LineType.SUCCESS))
        } else {
            results.add(TerminalLine("Failed to write to $filePart", LineType.ERROR))
        }
        return results
    }

    private fun resolveVirtualDir(path: String): String {
        return when {
            path == "/" -> "/"
            path == "~" -> if (isRoot) "/root" else "/data/local/tmp"
            path.startsWith("/") -> normalizePath(path)
            path == ".." -> {
                val parent = File(cwd).parent ?: "/"
                normalizePath(parent)
            }
            path.startsWith("../") -> {
                val combined = File(cwd, path).normalize().path
                normalizePath(combined)
            }
            else -> {
                val combined = if (cwd == "/") "/$path" else "$cwd/$path"
                normalizePath(combined)
            }
        }
    }

    fun resolveVirtualPath(path: String): String {
        return resolveVirtualDir(path)
    }

    private fun normalizePath(raw: String): String {
        var clean = raw.replace("//", "/")
        if (clean.length > 1 && clean.endsWith("/")) {
            clean = clean.substring(0, clean.length - 1)
        }
        return if (clean.isEmpty()) "/" else clean
    }
}
