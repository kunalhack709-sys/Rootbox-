package com.example.core.filesystem

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class VirtualFilesystem(private val context: Context) {

    val rootDir: File = File(context.filesDir, "rootbox_rootfs").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    init {
        initializeFilesystem()
    }

    fun initializeFilesystem() {
        // Standard virtual directory hierarchy
        val directories = listOf(
            "/system/bin",
            "/system/xbin",
            "/system/etc",
            "/system/lib64",
            "/vendor/bin",
            "/data/app",
            "/data/data",
            "/data/local/tmp",
            "/data/system",
            "/tmp",
            "/sdcard/Download",
            "/sdcard/Documents",
            "/proc",
            "/etc",
            "/dev",
            "/root",
            "/home"
        )

        for (dir in directories) {
            val f = resolveVirtualPath(dir)
            if (!f.exists()) {
                f.mkdirs()
            }
        }

        // Default virtual build.prop
        val buildProp = resolveVirtualPath("/system/build.prop")
        if (!buildProp.exists()) {
            buildProp.writeText(
                """
                # RootBox Virtual Android System Properties
                ro.build.id=UP1A.231005.007
                ro.build.version.incremental=eng.rootbox.20260925
                ro.build.version.sdk=34
                ro.build.version.release=14
                ro.build.date=Fri Sep 25 09:22:33 UTC 2026
                ro.build.type=userdebug
                ro.product.model=RootBox Virtual Android
                ro.product.brand=RootBox
                ro.product.name=rootbox_arm64
                ro.product.device=vcontainer
                ro.product.cpu.abi=arm64-v8a
                ro.virtual.environment=1
                ro.virtual.root=enabled
                ro.secure=0
                ro.debuggable=1
                """.trimIndent()
            )
        }

        // Virtual SU binary script
        val su = resolveVirtualPath("/system/xbin/su")
        if (!su.exists()) {
            su.writeText(
                """
                #!/system/bin/sh
                # RootBox Virtual SU binary (UID 0 emulator)
                export USER=root
                export HOME=/root
                export PATH=/system/xbin:/system/bin:${'$'}PATH
                echo "RootBox Virtual SU: Granted root privileges inside virtual container."
                exec /system/bin/sh "${'$'}@"
                """.trimIndent()
            )
            su.setExecutable(true)
        }

        // Virtual SH binary
        val sh = resolveVirtualPath("/system/bin/sh")
        if (!sh.exists()) {
            sh.writeText("#!/bin/sh\n# RootBox Virtual Shell")
            sh.setExecutable(true)
        }

        // /etc/hosts
        val hosts = resolveVirtualPath("/etc/hosts")
        if (!hosts.exists()) {
            hosts.writeText(
                """
                127.0.0.1   localhost
                ::1         localhost ip6-localhost
                192.168.100.2 rootbox-vm
                """.trimIndent()
            )
        }

        // /etc/resolv.conf
        val resolv = resolveVirtualPath("/etc/resolv.conf")
        if (!resolv.exists()) {
            resolv.writeText(
                """
                nameserver 8.8.8.8
                nameserver 1.1.1.1
                """.trimIndent()
            )
        }

        // /etc/passwd
        val passwd = resolveVirtualPath("/etc/passwd")
        if (!passwd.exists()) {
            passwd.writeText(
                """
                root:x:0:0:Virtual Root:/root:/system/bin/sh
                system:x:1000:1000:Virtual System:/data:/system/bin/sh
                shell:x:2000:2000:Virtual Shell:/data/local/tmp:/system/bin/sh
                app:x:10000:10000:Virtual Apps:/data/data:/system/bin/sh
                """.trimIndent()
            )
        }

        // /proc/version
        val procVersion = resolveVirtualPath("/proc/version")
        if (!procVersion.exists()) {
            procVersion.writeText("Linux version 6.1.0-rootbox-vAOSP (gcc-13) #1 SMP PREEMPT RootBox Virtual Android aarch64\n")
        }

        // /proc/cpuinfo
        val procCpu = resolveVirtualPath("/proc/cpuinfo")
        if (!procCpu.exists()) {
            procCpu.writeText(
                """
                processor	: 0
                BogoMIPS	: 38.40
                Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp
                CPU implementer	: 0x41
                CPU architecture: 8
                Hardware	: RootBox Virtual AOSP ARM64
                """.trimIndent()
            )
        }

        // /sdcard README
        val sdReadme = resolveVirtualPath("/sdcard/README.txt")
        if (!sdReadme.exists()) {
            sdReadme.writeText(
                """
                RootBox Virtual Storage (/sdcard)
                =================================
                This filesystem is completely isolated within RootBox.
                Any file written here remains safely inside the virtual container.
                Host physical storage is protected and unrooted.
                """.trimIndent()
            )
        }
    }

    fun resolveVirtualPath(virtualPath: String): File {
        val sanitized = virtualPath.trim().replace('\\', '/')
        val cleanPath = when {
            sanitized.isEmpty() || sanitized == "/" -> ""
            sanitized.startsWith("/") -> sanitized.substring(1)
            else -> sanitized
        }
        return File(rootDir, cleanPath)
    }

    fun toVirtualPath(file: File): String {
        val rootPath = rootDir.absolutePath
        val filePath = file.absolutePath
        return if (filePath.startsWith(rootPath)) {
            val rel = filePath.substring(rootPath.length)
            if (rel.isEmpty()) "/" else rel
        } else {
            "/"
        }
    }

    fun listFiles(virtualPath: String): List<VirtualFileItem> {
        val target = resolveVirtualPath(virtualPath)
        if (!target.exists() || !target.isDirectory) return emptyList()

        val files = target.listFiles() ?: return emptyList()
        return files.map { file ->
            val vPath = toVirtualPath(file)
            val isDir = file.isDirectory
            val perms = if (isDir) "drwxr-xr-x" else if (file.canExecute()) "-rwxr-xr-x" else "-rw-r--r--"
            val owner = if (vPath.startsWith("/system") || vPath.startsWith("/vendor")) "root:root"
            else if (vPath.startsWith("/data/app")) "system:system"
            else if (vPath.startsWith("/sdcard")) "media_rw:media_rw"
            else "root:root"

            VirtualFileItem(
                name = file.name,
                virtualPath = vPath,
                isDirectory = isDir,
                sizeBytes = if (isDir) 4096L else file.length(),
                permissions = perms,
                owner = owner,
                lastModified = file.lastModified(),
                isExecutable = file.canExecute()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun readFile(virtualPath: String): String {
        val file = resolveVirtualPath(virtualPath)
        return if (file.exists() && file.isFile) {
            file.readText(StandardCharsets.UTF_8)
        } else {
            ""
        }
    }

    fun writeFile(virtualPath: String, content: String): Boolean {
        return try {
            val file = resolveVirtualPath(virtualPath)
            file.parentFile?.mkdirs()
            file.writeText(content, StandardCharsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(virtualPath: String): Boolean {
        val file = resolveVirtualPath(virtualPath)
        return file.mkdirs()
    }

    fun delete(virtualPath: String): Boolean {
        val file = resolveVirtualPath(virtualPath)
        return if (file.exists() && file.absolutePath != rootDir.absolutePath) {
            file.deleteRecursively()
        } else {
            false
        }
    }

    fun rename(virtualPath: String, newName: String): Boolean {
        val file = resolveVirtualPath(virtualPath)
        if (!file.exists()) return false
        val parent = file.parentFile ?: return false
        val dest = File(parent, newName)
        return file.renameTo(dest)
    }

    fun copy(srcVirtualPath: String, dstVirtualPath: String): Boolean {
        return try {
            val src = resolveVirtualPath(srcVirtualPath)
            val dst = resolveVirtualPath(dstVirtualPath)
            if (!src.exists()) return false
            if (src.isDirectory) {
                src.copyRecursively(dst, overwrite = true)
            } else {
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun chmod(virtualPath: String, mode: String): Boolean {
        val file = resolveVirtualPath(virtualPath)
        if (!file.exists()) return false
        // mode like "755", "+x", etc.
        if (mode.contains("x") || mode == "755" || mode == "777") {
            file.setExecutable(true, false)
        }
        return true
    }

    fun calculateTotalStorageUsed(): Long {
        return calculateDirSize(rootDir)
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val list = dir.listFiles() ?: return 0L
        for (f in list) {
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }

    fun importFromInputStream(virtualDir: String, fileName: String, inputStream: InputStream): Boolean {
        return try {
            val targetDir = resolveVirtualPath(virtualDir)
            targetDir.mkdirs()
            val dest = File(targetDir, fileName)
            dest.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportToOutputStream(virtualPath: String, outputStream: OutputStream): Boolean {
        return try {
            val src = resolveVirtualPath(virtualPath)
            if (!src.exists()) return false
            src.inputStream().use { input ->
                input.copyTo(outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
