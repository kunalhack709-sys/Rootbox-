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

    var isKaliMode: Boolean = true
        private set

    // Installed packages in the Kali/RootBox repository
    val installedTools = mutableSetOf(
        "nmap",
        "netcat",
        "curl",
        "wget",
        "whois",
        "dnsutils",
        "traceroute",
        "ifconfig",
        "netstat",
        "neofetch",
        "python3",
        "git",
        "busybox",
        "nano"
    )

    private val availableAptRepo = mapOf(
        "nmap" to "Network exploration tool and security / port scanner",
        "netcat" to "TCP/IP swiss army knife (nc utility)",
        "curl" to "Command line tool for transferring data with URLs",
        "wget" to "Network utility to retrieve files from the Web",
        "whois" to "Intelligent WHOIS client",
        "dnsutils" to "DNS utilities including dig, nslookup and host",
        "traceroute" to "Traces route packets take to network host",
        "tcpdump" to "Network packet capture and protocol analyzer",
        "hydra" to "Network authentication and login auditing tool",
        "sqlmap" to "Database inspection and SQL security tool",
        "wireshark" to "Network traffic packet analyzer CLI (tshark)",
        "aircrack-ng" to "Wireless network security assessment suite",
        "john" to "John the Ripper password security auditing tool",
        "nikto" to "Web server security and configuration scanner",
        "metasploit-framework" to "Security auditing and penetration testing suite",
        "neofetch" to "Fast, highly customizable system info script",
        "python3" to "Interactive Python 3.11 runtime environment",
        "git" to "Fast, scalable, distributed revision control system",
        "htop" to "Interactive process viewer and system monitor",
        "nano" to "Small, friendly text editor"
    )

    private val envVars = mutableMapOf(
        "USER" to "root",
        "UID" to "0",
        "GID" to "0",
        "HOME" to "/root",
        "SHELL" to "/bin/bash",
        "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/xbin:/system/bin",
        "ROOTBOX_VIRTUAL" to "1",
        "ROOTBOX_VERSION" to "2.4.0-vaosp",
        "TERM" to "xterm-256color",
        "HOSTNAME" to "kali",
        "OS" to "Kali GNU/Linux Rolling",
        "ANDROID_DATA" to "/data",
        "ANDROID_ROOT" to "/system"
    )

    fun getPrompt(): String {
        return if (isKaliMode) {
            val userStr = if (isRoot) "root㉿kali" else "kali㉿kali"
            val displayCwd = if (cwd == "/root" && isRoot) "~" else if (cwd == "/home/kali" && !isRoot) "~" else cwd
            val symbol = if (isRoot) "#" else "$"
            "┌──($userStr)-[$displayCwd]\n└─$symbol "
        } else {
            val userStr = if (isRoot) "root@rootbox" else "shell@rootbox"
            val symbol = if (isRoot) "#" else "$"
            "$userStr:$cwd $symbol "
        }
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
            "rmmod", "modprobe", "magisk", "unlock_bootloader",
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

        // Handle redirection
        if (trimmed.contains(" > ") || trimmed.contains(" >> ")) {
            return handleRedirection(trimmed, results)
        }

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0]
        val args = parts.drop(1)

        when (cmd) {
            "clear" -> {
                return listOf(TerminalLine("__CLEAR_SCREEN__", LineType.SYSTEM))
            }

            "help" -> {
                results.add(TerminalLine("Kali Linux & RootBox Virtual Shell (vAOSP 14)", LineType.SYSTEM))
                results.add(TerminalLine("Package Management:", LineType.SYSTEM))
                results.add(TerminalLine("  apt update                           : Update Kali repository package index", LineType.OUTPUT))
                results.add(TerminalLine("  apt install <tool>                   : Install tools (nmap, netcat, curl, etc.)", LineType.OUTPUT))
                results.add(TerminalLine("  apt list [--installed]               : List available or installed tools", LineType.OUTPUT))
                results.add(TerminalLine("  apt remove <tool>                    : Remove installed tool", LineType.OUTPUT))
                results.add(TerminalLine("Network & Security Tools:", LineType.SYSTEM))
                results.add(TerminalLine("  nmap [-sS|-sT|-p] <target>           : Network and open port scanner", LineType.OUTPUT))
                results.add(TerminalLine("  ifconfig / ip addr                   : Display virtual network interfaces (vnet0)", LineType.OUTPUT))
                results.add(TerminalLine("  netstat / ss                         : Show active virtual sockets & ports", LineType.OUTPUT))
                results.add(TerminalLine("  curl [-I] <url>                      : Transfer data / HTTP requests", LineType.OUTPUT))
                results.add(TerminalLine("  wget <url>                           : Download file from web into virtual storage", LineType.OUTPUT))
                results.add(TerminalLine("  whois <domain>                       : Query domain WHOIS registration", LineType.OUTPUT))
                results.add(TerminalLine("  dig / nslookup <domain>              : Query DNS records via 8.8.8.8", LineType.OUTPUT))
                results.add(TerminalLine("  traceroute <target>                  : Trace packet route across virtual hops", LineType.OUTPUT))
                results.add(TerminalLine("  nc / netcat <host> <port>            : Test TCP port connection", LineType.OUTPUT))
                results.add(TerminalLine("  tcpdump                              : Capture live packet traffic on vnet0", LineType.OUTPUT))
                results.add(TerminalLine("  msfconsole                           : Launch Metasploit console banner", LineType.OUTPUT))
                results.add(TerminalLine("  neofetch                             : Show Kali Linux system information", LineType.OUTPUT))
                results.add(TerminalLine("System & Filesystem:", LineType.SYSTEM))
                results.add(TerminalLine("  id, whoami, uname -a, su, exit       : User identity & privilege control", LineType.OUTPUT))
                results.add(TerminalLine("  pwd, cd, ls, cat, touch, mkdir, rm   : Virtual Filesystem operations", LineType.OUTPUT))
                results.add(TerminalLine("  kali-mode [on|off]                   : Toggle Kali prompt style", LineType.OUTPUT))
                results.add(TerminalLine("  pm [list|install|uninstall]          : Android Package Manager", LineType.OUTPUT))
            }

            // Kali / Root prompt toggle
            "kali", "kali-mode" -> {
                if (args.isNotEmpty() && args[0] == "off") {
                    isKaliMode = false
                    results.add(TerminalLine("Switched to standard RootBox prompt style.", LineType.OUTPUT))
                } else {
                    isKaliMode = true
                    results.add(TerminalLine("Switched to Kali Linux prompt style: ┌──(root㉿kali)-[/]└─#", LineType.SUCCESS))
                }
            }

            "kali-banner", "banner" -> {
                printKaliBanner(results)
            }

            "neofetch", "fastfetch" -> {
                printNeofetch(results)
            }

            // APT Package Manager
            "apt", "apt-get" -> {
                handleAptCommand(args, results)
            }

            "dpkg" -> {
                if (args.contains("-l")) {
                    results.add(TerminalLine("Desired=Unknown/Install/Remove/Purge/Hold", LineType.SYSTEM))
                    results.add(TerminalLine("Status=Not/Inst/Conf-files/Unpacked/halF-conf/Half-inst/trig-aWait/Trig-pend", LineType.SYSTEM))
                    results.add(TerminalLine("++- ==========================================================================", LineType.SYSTEM))
                    installedTools.forEach { tool ->
                        val desc = availableAptRepo[tool] ?: "Kali Linux utility package"
                        results.add(TerminalLine("ii  $tool  2.4.0-vaosp  arm64  $desc", LineType.OUTPUT))
                    }
                } else {
                    results.add(TerminalLine("dpkg: usage: dpkg -l (list packages)", LineType.WARNING))
                }
            }

            // Network Tools: NMAP
            "nmap" -> {
                if (!installedTools.contains("nmap")) {
                    results.add(TerminalLine("bash: nmap: command not found. You can install it with: apt install nmap", LineType.ERROR))
                } else {
                    val target = args.lastOrNull { !it.startsWith("-") } ?: "192.168.100.1"
                    val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                    results.add(TerminalLine("Starting Nmap 7.94 ( https://nmap.org ) at $now UTC", LineType.SYSTEM))
                    results.add(TerminalLine("Nmap scan report for $target", LineType.OUTPUT))
                    results.add(TerminalLine("Host is up (0.0012s latency).", LineType.SUCCESS))
                    results.add(TerminalLine("Not shown: 995 closed tcp ports (conn-refused)", LineType.OUTPUT))
                    results.add(TerminalLine("PORT     STATE SERVICE       VERSION", LineType.SYSTEM))
                    results.add(TerminalLine("22/tcp   open  ssh           OpenSSH 9.3p1 Debian (protocol 2.0)", LineType.OUTPUT))
                    results.add(TerminalLine("53/tcp   open  domain        dnsmasq 2.89", LineType.OUTPUT))
                    results.add(TerminalLine("80/tcp   open  http          nginx 1.24.0 (RootBox Virtual vAOSP)", LineType.OUTPUT))
                    results.add(TerminalLine("443/tcp  open  ssl/https     nginx 1.24.0", LineType.OUTPUT))
                    results.add(TerminalLine("8080/tcp open  http-proxy    RootBox Internal Proxy", LineType.OUTPUT))
                    results.add(TerminalLine("Service Info: OS: Linux; CPE: cpe:/o:linux:linux_kernel", LineType.OUTPUT))
                    results.add(TerminalLine("Nmap done: 1 IP address (1 host up) scanned in 1.48 seconds", LineType.SUCCESS))
                }
            }

            // IFCONFIG / IP
            "ifconfig" -> {
                results.add(TerminalLine("vnet0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500", LineType.SYSTEM))
                results.add(TerminalLine("        inet 192.168.100.2  netmask 255.255.255.0  broadcast 192.168.100.255", LineType.OUTPUT))
                results.add(TerminalLine("        inet6 fe80::a00:27ff:fe4e:66a1  prefixlen 64  scopeid 0x20<link>", LineType.OUTPUT))
                results.add(TerminalLine("        ether 08:00:27:4e:66:a1  txqueuelen 1000  (Ethernet)", LineType.OUTPUT))
                results.add(TerminalLine("        RX packets 4210  bytes 3849120 (3.8 MB)", LineType.OUTPUT))
                results.add(TerminalLine("        TX packets 3890  bytes 2491020 (2.4 MB)", LineType.OUTPUT))
                results.add(TerminalLine("", LineType.OUTPUT))
                results.add(TerminalLine("lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536", LineType.SYSTEM))
                results.add(TerminalLine("        inet 127.0.0.1  netmask 255.0.0.0", LineType.OUTPUT))
                results.add(TerminalLine("        inet6 ::1  prefixlen 128  scopeid 0x10<host>", LineType.OUTPUT))
                results.add(TerminalLine("        loop  txqueuelen 1000  (Local Loopback)", LineType.OUTPUT))
            }

            "ip" -> {
                if (args.isEmpty() || args[0] == "a" || args[0] == "addr") {
                    results.add(TerminalLine("1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default", LineType.SYSTEM))
                    results.add(TerminalLine("    inet 127.0.0.1/8 scope host lo", LineType.OUTPUT))
                    results.add(TerminalLine("2: vnet0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP group default", LineType.SYSTEM))
                    results.add(TerminalLine("    link/ether 08:00:27:4e:66:a1 brd ff:ff:ff:ff:ff:ff", LineType.OUTPUT))
                    results.add(TerminalLine("    inet 192.168.100.2/24 brd 192.168.100.255 scope global vnet0", LineType.SUCCESS))
                } else if (args[0] == "route" || args[0] == "r") {
                    results.add(TerminalLine("default via 192.168.100.1 dev vnet0 proto static", LineType.OUTPUT))
                    results.add(TerminalLine("192.168.100.0/24 dev vnet0 proto kernel scope link src 192.168.100.2", LineType.OUTPUT))
                } else {
                    results.add(TerminalLine("Usage: ip [addr|route]", LineType.WARNING))
                }
            }

            // NETSTAT / SS
            "netstat", "ss" -> {
                results.add(TerminalLine("Active Internet connections (only servers)", LineType.SYSTEM))
                results.add(TerminalLine("Proto Recv-Q Send-Q Local Address           Foreign Address         State", LineType.SYSTEM))
                results.add(TerminalLine("tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN", LineType.OUTPUT))
                results.add(TerminalLine("tcp        0      0 0.0.0.0:80              0.0.0.0:*               LISTEN", LineType.OUTPUT))
                results.add(TerminalLine("tcp        0      0 127.0.0.1:5037          0.0.0.0:*               LISTEN", LineType.OUTPUT))
                results.add(TerminalLine("tcp        0      0 192.168.100.2:48102     1.1.1.1:443             ESTABLISHED", LineType.SUCCESS))
                results.add(TerminalLine("udp        0      0 0.0.0.0:53              0.0.0.0:*", LineType.OUTPUT))
            }

            // CURL
            "curl" -> {
                val targetUrl = args.lastOrNull { !it.startsWith("-") } ?: "https://httpbin.org/get"
                val headersOnly = args.contains("-I") || args.contains("--head")
                if (headersOnly) {
                    results.add(TerminalLine("HTTP/2 200 OK", LineType.SUCCESS))
                    results.add(TerminalLine("date: ${Date()}", LineType.OUTPUT))
                    results.add(TerminalLine("server: gunicorn/20.1.0", LineType.OUTPUT))
                    results.add(TerminalLine("content-type: application/json", LineType.OUTPUT))
                    results.add(TerminalLine("content-length: 284", LineType.OUTPUT))
                    results.add(TerminalLine("access-control-allow-origin: *", LineType.OUTPUT))
                } else {
                    results.add(TerminalLine("{", LineType.OUTPUT))
                    results.add(TerminalLine("  \"url\": \"$targetUrl\",", LineType.OUTPUT))
                    results.add(TerminalLine("  \"origin\": \"192.168.100.2\",", LineType.OUTPUT))
                    results.add(TerminalLine("  \"headers\": {", LineType.OUTPUT))
                    results.add(TerminalLine("    \"User-Agent\": \"curl/8.2.1-Kali-Linux\",", LineType.OUTPUT))
                    results.add(TerminalLine("    \"Host\": \"${targetUrl.replace("https://", "").replace("http://", "").split("/")[0]}\"", LineType.OUTPUT))
                    results.add(TerminalLine("  }", LineType.OUTPUT))
                    results.add(TerminalLine("}", LineType.OUTPUT))
                }
            }

            // WGET
            "wget" -> {
                val url = args.lastOrNull { !it.startsWith("-") } ?: "https://example.com/index.html"
                val filename = url.substringAfterLast("/").ifBlank { "index.html" }
                results.add(TerminalLine("--${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}--  $url", LineType.SYSTEM))
                results.add(TerminalLine("Resolving host via 8.8.8.8... 93.184.216.34", LineType.OUTPUT))
                results.add(TerminalLine("Connecting to 93.184.216.34:443... connected.", LineType.OUTPUT))
                results.add(TerminalLine("HTTP request sent, awaiting response... 200 OK", LineType.SUCCESS))
                results.add(TerminalLine("Length: 1256 (1.2K) [text/html]", LineType.OUTPUT))
                results.add(TerminalLine("Saving to: '$filename'", LineType.OUTPUT))
                fs.writeFile(resolveVirtualPath(filename), "<html><body><h1>Downloaded via RootBox wget</h1><p>$url</p></body></html>")
                results.add(TerminalLine("'$filename' saved [1256/1256]", LineType.SUCCESS))
            }

            // WHOIS
            "whois" -> {
                val domain = args.firstOrNull() ?: "example.com"
                results.add(TerminalLine("Domain Name: ${domain.uppercase()}", LineType.SYSTEM))
                results.add(TerminalLine("Registry Domain ID: 2138514_DOMAIN_COM-VRSN", LineType.OUTPUT))
                results.add(TerminalLine("Registrar: RootBox Registrar Services LLC", LineType.OUTPUT))
                results.add(TerminalLine("Updated Date: 2025-08-14T07:00:00Z", LineType.OUTPUT))
                results.add(TerminalLine("Creation Date: 1995-10-02T04:00:00Z", LineType.OUTPUT))
                results.add(TerminalLine("Registry Expiry Date: 2028-10-02T04:00:00Z", LineType.OUTPUT))
                results.add(TerminalLine("Name Server: NS1.${domain.uppercase()}", LineType.OUTPUT))
                results.add(TerminalLine("Name Server: NS2.${domain.uppercase()}", LineType.OUTPUT))
                results.add(TerminalLine("DNSSEC: unsigned", LineType.OUTPUT))
            }

            // DIG / NSLOOKUP
            "dig", "nslookup" -> {
                val domain = args.firstOrNull { !it.startsWith("-") } ?: "example.com"
                results.add(TerminalLine("; <<>> DiG 9.18.19-1~deb12u1-Kali <<>> $domain", LineType.SYSTEM))
                results.add(TerminalLine(";; Got answer:", LineType.OUTPUT))
                results.add(TerminalLine(";; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 48210", LineType.OUTPUT))
                results.add(TerminalLine(";; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1", LineType.OUTPUT))
                results.add(TerminalLine("", LineType.OUTPUT))
                results.add(TerminalLine(";; QUESTION SECTION:", LineType.SYSTEM))
                results.add(TerminalLine(";$domain.			IN	A", LineType.OUTPUT))
                results.add(TerminalLine("", LineType.OUTPUT))
                results.add(TerminalLine(";; ANSWER SECTION:", LineType.SYSTEM))
                results.add(TerminalLine("$domain.		3600	IN	A	93.184.216.34", LineType.SUCCESS))
                results.add(TerminalLine("", LineType.OUTPUT))
                results.add(TerminalLine(";; Query time: 14 msec", LineType.OUTPUT))
                results.add(TerminalLine(";; SERVER: 8.8.8.8#53(8.8.8.8) (UDP)", LineType.OUTPUT))
            }

            // TRACEROUTE
            "traceroute" -> {
                val host = args.firstOrNull() ?: "1.1.1.1"
                results.add(TerminalLine("traceroute to $host ($host), 30 hops max, 60 byte packets", LineType.SYSTEM))
                results.add(TerminalLine(" 1  rootbox-vnet-gateway (192.168.100.1)  0.742 ms  0.612 ms  0.518 ms", LineType.OUTPUT))
                results.add(TerminalLine(" 2  10.0.2.2 (10.0.2.2)  1.420 ms  1.310 ms  1.215 ms", LineType.OUTPUT))
                results.add(TerminalLine(" 3  172.16.1.1 (172.16.1.1)  6.410 ms  6.310 ms  6.120 ms", LineType.OUTPUT))
                results.add(TerminalLine(" 4  $host ($host)  14.210 ms  13.820 ms  14.050 ms", LineType.SUCCESS))
            }

            // NC / NETCAT
            "nc", "netcat" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("usage: nc [-zv] <host> <port>", LineType.WARNING))
                } else {
                    val port = args.lastOrNull() ?: "80"
                    val host = args.getOrNull(args.size - 2) ?: "192.168.100.1"
                    results.add(TerminalLine("Connection to $host $port port [tcp/http] succeeded!", LineType.SUCCESS))
                }
            }

            // TCPDUMP
            "tcpdump" -> {
                results.add(TerminalLine("tcpdump: verbose output suppressed, use -v[v]... for full protocol decode", LineType.SYSTEM))
                results.add(TerminalLine("listening on vnet0, link-type EN10MB (Ethernet), snapshot length 262144 bytes", LineType.SYSTEM))
                results.add(TerminalLine("19:42:01.120 IP 192.168.100.2.48102 > 8.8.8.8.53: 48210+ A? example.com. (29)", LineType.OUTPUT))
                results.add(TerminalLine("19:42:01.134 IP 8.8.8.8.53 > 192.168.100.2.48102: 48210 1/0/0 A 93.184.216.34 (45)", LineType.OUTPUT))
                results.add(TerminalLine("19:42:01.140 IP 192.168.100.2.51240 > 93.184.216.34.443: Flags [S], seq 128491024, win 64240", LineType.OUTPUT))
                results.add(TerminalLine("19:42:01.155 IP 93.184.216.34.443 > 192.168.100.2.51240: Flags [S.], seq 48102834, ack 128491025", LineType.OUTPUT))
                results.add(TerminalLine("4 packets captured, 4 packets received by filter, 0 packets dropped by kernel", LineType.SUCCESS))
            }

            // ARP
            "arp" -> {
                results.add(TerminalLine("Address                  HWtype  HWaddress           Flags Mask            Iface", LineType.SYSTEM))
                results.add(TerminalLine("192.168.100.1            ether   52:54:00:12:34:56   C                     vnet0", LineType.OUTPUT))
            }

            // MSFCONSOLE
            "msfconsole" -> {
                results.add(TerminalLine("      .:okOOOkdc'           'cdkOOOko:.", LineType.ERROR))
                results.add(TerminalLine("    .xOOOOOOOOOOOOc       cOOOOOOOOOOOOx.", LineType.ERROR))
                results.add(TerminalLine("   :OOOOOOOOOOOOOOOk,   ,kOOOOOOOOOOOOOOO:", LineType.ERROR))
                results.add(TerminalLine("  'OOOOOOOOOkkkkOOOOO: :OOOOOOOOOOOOOOOOOO'", LineType.ERROR))
                results.add(TerminalLine("       =[ metasploit v6.3.35-dev-kali                   ]", LineType.SYSTEM))
                results.add(TerminalLine("+ -- --=[ 2345 exploits - 1215 auxiliary - 412 post       ]", LineType.OUTPUT))
                results.add(TerminalLine("+ -- --=[ 965 payloads  - 45 encoders   - 11 nops         ]", LineType.OUTPUT))
                results.add(TerminalLine("Virtual Metasploit console initialized in RootBox container.", LineType.SUCCESS))
                results.add(TerminalLine("msf6 >", LineType.SUCCESS))
            }

            // PYTHON3
            "python", "python3" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("Python 3.11.6 (main, Oct  8 2025, 05:06:43) [GCC 13.2.0] on linux", LineType.SYSTEM))
                    results.add(TerminalLine("Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.", LineType.OUTPUT))
                    results.add(TerminalLine(">>> print('Hello from RootBox Kali environment!')", LineType.OUTPUT))
                    results.add(TerminalLine("Hello from RootBox Kali environment!", LineType.SUCCESS))
                } else if (args[0] == "-c") {
                    val code = args.drop(1).joinToString(" ").trim('"', '\'')
                    results.add(TerminalLine(">>> $code", LineType.OUTPUT))
                    if (code.contains("2+2") || code.contains("2 + 2")) {
                        results.add(TerminalLine("4", LineType.SUCCESS))
                    } else {
                        results.add(TerminalLine("Executed: $code", LineType.SUCCESS))
                    }
                } else {
                    results.add(TerminalLine("Python 3.11: executed script ${args[0]}", LineType.SUCCESS))
                }
            }

            // GIT
            "git" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("usage: git [--version] [--help] [-C <path>] [-c <name>=<value>] <command> [<args>]", LineType.OUTPUT))
                } else if (args[0] == "clone") {
                    val repo = args.getOrNull(1) ?: "https://github.com/rootbox/tools.git"
                    val folderName = repo.substringAfterLast("/").removeSuffix(".git")
                    results.add(TerminalLine("Cloning into '$folderName'...", LineType.OUTPUT))
                    results.add(TerminalLine("remote: Enumerating objects: 42, done.", LineType.OUTPUT))
                    results.add(TerminalLine("remote: Total 42 (delta 0), reused 0 (delta 0), pack-reused 42", LineType.OUTPUT))
                    results.add(TerminalLine("Receiving objects: 100% (42/42), 24.18 KiB | 4.84 MiB/s, done.", LineType.SUCCESS))
                    fs.createDirectory(resolveVirtualPath(folderName))
                    fs.writeFile(resolveVirtualPath("$folderName/README.md"), "# $folderName\nCloned into RootBox virtual filesystem.")
                } else {
                    results.add(TerminalLine("git version 2.43.0", LineType.OUTPUT))
                }
            }

            "id" -> {
                if (isRoot) {
                    results.add(TerminalLine("uid=0(root) gid=0(root) groups=0(root),1004(input),1007(log),1015(sdcard_rw),3003(inet) context=u:r:su:s0", LineType.OUTPUT))
                } else {
                    results.add(TerminalLine("uid=1000(kali) gid=1000(kali) groups=1000(kali),4(adm),24(cdrom),27(sudo),30(dip),46(plugdev),100(users)", LineType.OUTPUT))
                }
            }

            "whoami" -> {
                results.add(TerminalLine(if (isRoot) "root" else "kali", LineType.OUTPUT))
            }

            "uname" -> {
                if (args.contains("-a")) {
                    results.add(TerminalLine("Linux kali 6.1.0-rootbox-vAOSP #1 SMP PREEMPT RootBox Virtual Android Kali GNU/Linux aarch64", LineType.OUTPUT))
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
                    envVars["USER"] = "kali"
                    envVars["UID"] = "1000"
                    envVars["GID"] = "1000"
                    envVars["HOME"] = "/home/kali"
                    results.add(TerminalLine("Dropped virtual root. Now in user session (kali:1000).", LineType.WARNING))
                } else {
                    results.add(TerminalLine("Terminal session active in RootBox container.", LineType.OUTPUT))
                }
            }

            "pwd" -> {
                results.add(TerminalLine(cwd, LineType.OUTPUT))
            }

            "cd" -> {
                val target = if (args.isEmpty() || args[0] == "~") {
                    if (isRoot) "/root" else "/home/kali"
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

            "ps" -> {
                results.add(TerminalLine("USER       PID   PPID  VSIZE  RSS   WCHAN            PC  NAME", LineType.SYSTEM))
                results.add(TerminalLine("root         1      0  12416  4128  ep_poll    00000000 S init (vAOSP)", LineType.OUTPUT))
                results.add(TerminalLine("root         2      1   8192  2216  kthreadd   00000000 S vaospd", LineType.OUTPUT))
                results.add(TerminalLine("root        10      1 145216 42100  poll       00000000 S zygote64", LineType.OUTPUT))
                results.add(TerminalLine("system      12     10 412080 120400 ep_poll    00000000 S system_server", LineType.OUTPUT))
                results.add(TerminalLine("root        18      1  18340  6240  ep_poll    00000000 S vnetd", LineType.OUTPUT))
                results.add(TerminalLine("root       105      1  24150  8210  ep_poll    00000000 S nmap-service", LineType.OUTPUT))
                results.add(TerminalLine(if (isRoot) "root       210      1  14200  5120  sys_pause  00000000 S bash (kali)" else "kali       210      1  14200  5120  sys_pause  00000000 S bash (kali)", LineType.OUTPUT))
            }

            "top" -> {
                results.add(TerminalLine("Tasks: 8 total, 1 running, 7 sleeping, 0 stopped, 0 zombie", LineType.SYSTEM))
                results.add(TerminalLine("%Cpu(s):  2.4 us,  1.1 sy,  0.0 ni, 96.2 id,  0.3 wa,  0.0 hi", LineType.SYSTEM))
                results.add(TerminalLine("MiB Mem :   3072.0 total,   1420.5 free,   1184.2 used,    467.3 buff/cache", LineType.SYSTEM))
                results.add(TerminalLine("PID  USER     PR  NI    VIRT    RES  S %CPU  %MEM     TIME+ COMMAND", LineType.SYSTEM))
                results.add(TerminalLine(" 12  system   20   0  412.1M 120.4M  S  3.8   3.9   3:45.80 system_server", LineType.OUTPUT))
                results.add(TerminalLine(" 10  root     20   0  145.2M  42.1M  S  1.4   1.4   1:20.15 zygote64", LineType.OUTPUT))
                results.add(TerminalLine("105  root     20   0   24.1M   8.2M  S  0.8   0.3   0:12.40 nmap", LineType.OUTPUT))
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

            "pm" -> {
                if (args.isEmpty()) {
                    results.add(TerminalLine("Package manager usage: pm [list packages|install <path>|uninstall <pkg>]", LineType.WARNING))
                } else if (args[0] == "list" && args.getOrNull(1) == "packages") {
                    val apps = appDao?.getAllApps()?.firstOrNull() ?: emptyList()
                    apps.forEach {
                        results.add(TerminalLine("package:${it.packageName}", LineType.OUTPUT))
                    }
                } else if (args[0] == "install") {
                    val apk = args.getOrNull(1) ?: "app.apk"
                    results.add(TerminalLine("Success: Package $apk installed into /data/app", LineType.SUCCESS))
                } else if (args[0] == "uninstall") {
                    val pkg = args.getOrNull(1)
                    if (pkg != null) {
                        appDao?.deleteByPackageName(pkg)
                        results.add(TerminalLine("Success: Package $pkg uninstalled from virtual container", LineType.SUCCESS))
                    }
                }
            }

            "dmesg" -> {
                results.add(TerminalLine("[    0.000000] Booting Linux kernel 6.1.0-rootbox-vAOSP-kali", LineType.SYSTEM))
                results.add(TerminalLine("[    0.001420] Virtual CPU: 4 Cores allocated [ARM_v8.2_NEON]", LineType.OUTPUT))
                results.add(TerminalLine("[    0.003512] Memory: 3145728K/3145728K available", LineType.OUTPUT))
                results.add(TerminalLine("[    0.015240] RootBox vFS mounted on / [rw,noatime,relatime]", LineType.OUTPUT))
                results.add(TerminalLine("[    0.021045] Virtual network interface vnet0 initialized (192.168.100.2)", LineType.SUCCESS))
                results.add(TerminalLine("[    0.048910] Virtual root subsystem: UID 0 granted to rootbox", LineType.SUCCESS))
                results.add(TerminalLine("[    0.061002] Security Isolation: Host physical kernel boundary enforced", LineType.WARNING))
                results.add(TerminalLine("[    0.089100] Kali tool repository ready (/usr/bin, /usr/sbin)", LineType.OUTPUT))
            }

            "uptime" -> {
                results.add(TerminalLine(" 19:42:10 up 3:15,  1 user,  load average: 0.12, 0.08, 0.05", LineType.OUTPUT))
            }

            "date" -> {
                val now = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
                results.add(TerminalLine(now, LineType.OUTPUT))
            }

            else -> {
                // If it's a known available tool not yet installed, suggest installing it
                if (availableAptRepo.containsKey(cmd) && !installedTools.contains(cmd)) {
                    results.add(TerminalLine("Command '$cmd' not found, but can be installed with:", LineType.ERROR))
                    results.add(TerminalLine("  apt install $cmd", LineType.SYSTEM))
                } else {
                    results.add(TerminalLine("$cmd: command not found. Type 'help' or 'apt list' for available tools.", LineType.ERROR))
                }
            }
        }

        return results
    }

    private fun handleAptCommand(args: List<String>, results: MutableList<TerminalLine>) {
        if (args.isEmpty()) {
            results.add(TerminalLine("apt 2.6.1 (arm64)", LineType.SYSTEM))
            results.add(TerminalLine("Usage: apt [update | install <pkg> | remove <pkg> | list]", LineType.WARNING))
            return
        }

        val subCmd = args[0]
        when (subCmd) {
            "update" -> {
                results.add(TerminalLine("Get:1 http://http.kali.org/kali kali-rolling InRelease [41.5 kB]", LineType.OUTPUT))
                results.add(TerminalLine("Get:2 http://http.kali.org/kali kali-rolling/main Sources [16.2 MB]", LineType.OUTPUT))
                results.add(TerminalLine("Get:3 http://http.kali.org/kali kali-rolling/main arm64 Packages [19.8 MB]", LineType.OUTPUT))
                results.add(TerminalLine("Fetched 36.0 MB in 1.4s (25.7 MB/s)", LineType.SUCCESS))
                results.add(TerminalLine("Reading package lists... Done", LineType.OUTPUT))
                results.add(TerminalLine("Building dependency tree... Done", LineType.OUTPUT))
                results.add(TerminalLine("All 20 Kali virtual tools are up to date.", LineType.SUCCESS))
            }

            "install" -> {
                val pkgs = args.drop(1)
                if (pkgs.isEmpty()) {
                    results.add(TerminalLine("apt install: missing package name(s).", LineType.ERROR))
                    results.add(TerminalLine("Example: apt install nmap netcat curl", LineType.OUTPUT))
                    return
                }

                results.add(TerminalLine("Reading package lists... Done", LineType.OUTPUT))
                results.add(TerminalLine("Building dependency tree... Done", LineType.OUTPUT))

                for (pkg in pkgs) {
                    val cleanPkg = pkg.lowercase().trim()
                    if (availableAptRepo.containsKey(cleanPkg)) {
                        installedTools.add(cleanPkg)
                        results.add(TerminalLine("The following NEW package will be installed: $cleanPkg", LineType.SYSTEM))
                        results.add(TerminalLine("Get:1 http://http.kali.org/kali kali-rolling/main arm64 $cleanPkg (2.4.0) [2,481 kB]", LineType.OUTPUT))
                        results.add(TerminalLine("Unpacking $cleanPkg into /usr/bin/$cleanPkg...", LineType.OUTPUT))
                        results.add(TerminalLine("Setting up $cleanPkg (2.4.0-vaosp)...", LineType.OUTPUT))
                        results.add(TerminalLine("✓ Package '$cleanPkg' installed successfully. Run '$cleanPkg' to use it.", LineType.SUCCESS))
                        fs.writeFile(resolveVirtualPath("/system/xbin/$cleanPkg"), "#!/bin/sh\n# Kali $cleanPkg binary")
                    } else {
                        results.add(TerminalLine("E: Unable to locate package $pkg in Kali repositories.", LineType.ERROR))
                    }
                }
            }

            "remove", "purge" -> {
                val pkg = args.getOrNull(1)?.lowercase()
                if (pkg != null && installedTools.contains(pkg)) {
                    installedTools.remove(pkg)
                    results.add(TerminalLine("Removing $pkg (2.4.0-vaosp)...", LineType.OUTPUT))
                    results.add(TerminalLine("✓ Package '$pkg' removed.", LineType.SUCCESS))
                } else {
                    results.add(TerminalLine("Package '${args.getOrNull(1)}' is not installed.", LineType.WARNING))
                }
            }

            "list" -> {
                val onlyInstalled = args.contains("--installed")
                results.add(TerminalLine(if (onlyInstalled) "Installed Kali Packages:" else "Kali Rolling Package Repository:", LineType.SYSTEM))
                availableAptRepo.forEach { (pkg, desc) ->
                    val isInst = installedTools.contains(pkg)
                    if (!onlyInstalled || isInst) {
                        val status = if (isInst) "[installed]" else "[available]"
                        val color = if (isInst) LineType.SUCCESS else LineType.OUTPUT
                        results.add(TerminalLine("  $pkg - $desc $status", color))
                    }
                }
            }

            "search" -> {
                val query = args.getOrNull(1)?.lowercase() ?: ""
                results.add(TerminalLine("Searching Kali repository for '$query'...", LineType.SYSTEM))
                val matches = availableAptRepo.filter { it.key.contains(query) || it.value.contains(query, ignoreCase = true) }
                if (matches.isEmpty()) {
                    results.add(TerminalLine("No matching packages found.", LineType.WARNING))
                } else {
                    matches.forEach { (pkg, desc) ->
                        results.add(TerminalLine("$pkg/kali-rolling - $desc", LineType.OUTPUT))
                    }
                }
            }

            else -> {
                results.add(TerminalLine("E: Invalid operation $subCmd. Use update, install, remove, or list.", LineType.ERROR))
            }
        }
    }

    private fun printKaliBanner(results: MutableList<TerminalLine>) {
        results.add(TerminalLine("..............", LineType.SYSTEM))
        results.add(TerminalLine("            ..,;:ccc,.", LineType.SYSTEM))
        results.add(TerminalLine("          ......''';lxO.", LineType.SYSTEM))
        results.add(TerminalLine(" .....''''..........,:ld;", LineType.SYSTEM))
        results.add(TerminalLine("          .';;;:::;,,.x,", LineType.SYSTEM))
        results.add(TerminalLine("     ..'''.            0Xxoc:,.  ...", LineType.SYSTEM))
        results.add(TerminalLine(" ....                ,ONkc;,;cokOdc',.", LineType.SYSTEM))
        results.add(TerminalLine(" .                   OMo           ':ddo.", LineType.SYSTEM))
        results.add(TerminalLine("                    dMc               :OO;", LineType.SYSTEM))
        results.add(TerminalLine("                    0M.                 :NO.", LineType.SYSTEM))
        results.add(TerminalLine("                    Ol                   .Nx", LineType.SYSTEM))
        results.add(TerminalLine("   KALI LINUX ROLLING CONTAINER", LineType.SUCCESS))
        results.add(TerminalLine("   \"The Quieter You Become, The More You Are Able To Hear\"", LineType.OUTPUT))
        results.add(TerminalLine("   RootBox Virtual Android • 64-bit ARM64", LineType.OUTPUT))
    }

    private fun printNeofetch(results: MutableList<TerminalLine>) {
        results.add(TerminalLine("       _,met\$\$\$\$gg.          root@kali", LineType.SYSTEM))
        results.add(TerminalLine("    ,g\$\$\$\$\$\$\$\$\$\$\$\$\$\$P.       ---------", LineType.SYSTEM))
        results.add(TerminalLine("  ,g\$\$P\"\"       \"\"\"Y\$\$.     OS: Kali GNU/Linux Rolling arm64", LineType.OUTPUT))
        results.add(TerminalLine(" ,\$\$P'              `\$\$\$.   Host: RootBox Virtual Android Container", LineType.OUTPUT))
        results.add(TerminalLine("'\$\$P       ,ggs.     `\$\$b:  Kernel: 6.1.0-rootbox-vAOSP-kali", LineType.OUTPUT))
        results.add(TerminalLine("d\$\$'     ,\$P\"'   .    \$\$\$   Uptime: 3 hours, 15 mins", LineType.OUTPUT))
        results.add(TerminalLine("\$\$P      d\$\$'     ,   \$\$\$P  Packages: ${installedTools.size} (apt), 5 (pm)", LineType.OUTPUT))
        results.add(TerminalLine("\$\$:      \$\$.   -    ,d\$\$'   Shell: bash 5.2.15", LineType.OUTPUT))
        results.add(TerminalLine("\$\$;      Y\$b._   _,d\$P'     Terminal: rootbox-term (xterm-256color)", LineType.OUTPUT))
        results.add(TerminalLine("Y\$\$.     `.`\"Y\$\$\$P\"'        CPU: Virtual ARM64 4-Core @ 2.40GHz", LineType.OUTPUT))
        results.add(TerminalLine(" `\$\$b      \"-.__            Memory: 1184MiB / 3072MiB", LineType.OUTPUT))
        results.add(TerminalLine("  `Y\$\$.                     Root Context: UID 0 (root)", LineType.SUCCESS))
        results.add(TerminalLine("    `\$\$b.                   Network: vnet0 (192.168.100.2)", LineType.SUCCESS))
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
            path == "~" -> if (isRoot) "/root" else "/home/kali"
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
