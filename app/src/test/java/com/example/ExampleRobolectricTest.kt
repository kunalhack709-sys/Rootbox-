package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.filesystem.VirtualFilesystem
import com.example.core.terminal.LineType
import com.example.core.terminal.VirtualTerminalEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
