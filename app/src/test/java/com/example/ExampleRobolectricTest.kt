package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.apk.ApkManager
import com.example.core.filesystem.VirtualFilesystem
import com.example.core.terminal.LineType
import com.example.core.terminal.VirtualTerminalEngine
import com.example.data.local.RootBoxDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource is RootBox`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RootBox", appName)
    }

    @Test
    fun `verify virtual filesystem initializes rootfs correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fs = VirtualFilesystem(context)

        assertTrue(fs.resolveVirtualPath("/system").exists())
        assertTrue(fs.resolveVirtualPath("/data").exists())
        assertTrue(fs.resolveVirtualPath("/sdcard").exists())
        assertTrue(fs.resolveVirtualPath("/system/xbin/su").exists())
        assertTrue(fs.resolveVirtualPath("/system/build.prop").exists())

        val buildProp = fs.readFile("/system/build.prop")
        assertTrue(buildProp.contains("RootBox Virtual Android"))
        assertTrue(buildProp.contains("ro.virtual.environment=1"))
    }

    @Test
    fun `verify virtual terminal reports uid 0 and virtual android uname`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fs = VirtualFilesystem(context)
        val terminal = VirtualTerminalEngine(fs)

        val idResult = terminal.executeCommand("id")
        assertTrue(idResult.any { it.text.contains("uid=0(root)") })

        val unameResult = terminal.executeCommand("uname -a")
        assertTrue(unameResult.any { it.text.contains("RootBox Virtual Android") })

        val lsResult = terminal.executeCommand("ls /")
        assertTrue(lsResult.any { it.text.contains("system") && it.text.contains("data") })
    }

    @Test
    fun `verify physical root exploits are rejected with mandatory message`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fs = VirtualFilesystem(context)
        val terminal = VirtualTerminalEngine(fs)

        val exploitResult = terminal.executeCommand("fastboot flash recovery twrp.img")
        val expected = "This feature requires real device root and is unavailable in RootBox's no-root virtual environment."
        assertTrue(exploitResult.any { it.text == expected && it.type == LineType.ERROR })
    }

    @Test
    fun `verify APK install persistence and container management`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = RootBoxDatabase.getInstance(context)
        val fs = VirtualFilesystem(context)
        val apkManager = ApkManager(context, fs, db.virtualAppDao())

        // Install test application into RootBox
        val result = apkManager.installSampleApp("demo_sandbox") { /* progress */ }
        assertTrue(result.isSuccess)
        val installedApp = result.getOrNull()
        assertNotNull(installedApp)
        assertEquals("com.example.sandboxdemo", installedApp?.packageName)

        // Verify stored inside virtual /data/app and database
        assertTrue(fs.resolveVirtualPath("/data/app/com.example.sandboxdemo").exists())
        val apps = db.virtualAppDao().getAllApps().first()
        assertTrue(apps.any { it.packageName == "com.example.sandboxdemo" })

        // Launch app inside RootBox
        apkManager.launchApp("com.example.sandboxdemo")
        val runningApp = db.virtualAppDao().getApp("com.example.sandboxdemo")
        assertTrue(runningApp?.isRunning == true)

        // Stop app
        apkManager.stopApp("com.example.sandboxdemo")
        val stoppedApp = db.virtualAppDao().getApp("com.example.sandboxdemo")
        assertTrue(stoppedApp?.isRunning == false)

        // Clear data
        val cleared = apkManager.clearAppData("com.example.sandboxdemo")
        assertTrue(cleared)
    }

    @Test
    fun `verify kali apt tools installation and execution`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fs = VirtualFilesystem(context)
        val terminal = VirtualTerminalEngine(fs)

        // Verify Kali prompt
        assertTrue(terminal.getPrompt().contains("kali"))

        // Run apt update
        val updateRes = terminal.executeCommand("apt update")
        assertTrue(updateRes.any { it.text.contains("Reading package lists") })

        // Run apt install nmap
        val installRes = terminal.executeCommand("apt install nmap")
        assertTrue(installRes.any { it.text.contains("installed successfully") })
        assertTrue(terminal.installedTools.contains("nmap"))

        // Run nmap tool
        val nmapRes = terminal.executeCommand("nmap 192.168.100.1")
        assertTrue(nmapRes.any { it.text.contains("Nmap scan report") })
        assertTrue(nmapRes.any { it.text.contains("22/tcp") })

        // Run ifconfig
        val ifconfigRes = terminal.executeCommand("ifconfig")
        assertTrue(ifconfigRes.any { it.text.contains("192.168.100.2") })
    }
}
