package com.example.core.apk

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import com.example.core.filesystem.VirtualFilesystem
import com.example.data.local.VirtualAppDao
import com.example.data.local.entities.VirtualAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class ApkManager(
    private val context: Context,
    private val fs: VirtualFilesystem,
    private val appDao: VirtualAppDao
) {
    suspend fun initializeDefaultAppsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = appDao.getAllApps().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val defaultApps = listOf(
                VirtualAppEntity(
                    packageName = "com.termux.virtual",
                    appName = "Termux Virtual CLI",
                    versionName = "0.118.0",
                    versionCode = 118,
                    sizeBytes = 42 * 1024 * 1024L,
                    permissions = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE, android.permission.VIBRATE, android.permission.ACCESS_SUPERUSER",
                    isSystemApp = true,
                    isRunning = false,
                    targetSdk = 34,
                    minSdk = 24
                ),
                VirtualAppEntity(
                    packageName = "stericson.busybox.virtual",
                    appName = "BusyBox Utilities",
                    versionName = "1.36.1",
                    versionCode = 361,
                    sizeBytes = 18 * 1024 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = true,
                    isRunning = false,
                    targetSdk = 34,
                    minSdk = 24
                ),
                VirtualAppEntity(
                    packageName = "com.speedsoftware.rootexplorer.virtual",
                    appName = "Root Explorer Virtual",
                    versionName = "4.12.0",
                    versionCode = 412,
                    sizeBytes = 24 * 1024 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = false,
                    isRunning = false,
                    targetSdk = 34,
                    minSdk = 24
                ),
                VirtualAppEntity(
                    packageName = "eu.chainfire.supersu.virtual",
                    appName = "SuperSU Virtual Manager",
                    versionName = "2.82-SR5",
                    versionCode = 282,
                    sizeBytes = 14 * 1024 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.RECEIVE_BOOT_COMPLETED",
                    isSystemApp = true,
                    isRunning = false,
                    targetSdk = 33,
                    minSdk = 24
                ),
                VirtualAppEntity(
                    packageName = "com.rootbox.sqlitebrowser",
                    appName = "SQLite3 DB Browser",
                    versionName = "3.42.0",
                    versionCode = 342,
                    sizeBytes = 16 * 1024 * 1024L,
                    permissions = "android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = false,
                    isRunning = false,
                    targetSdk = 34,
                    minSdk = 24
                ),
                VirtualAppEntity(
                    packageName = "org.rootbox.microhttpd",
                    appName = "MicroWebServer HTTPD",
                    versionName = "1.5.2",
                    versionCode = 152,
                    sizeBytes = 8 * 1024 * 1024L,
                    permissions = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE",
                    isSystemApp = false,
                    isRunning = false,
                    targetSdk = 34,
                    minSdk = 24
                )
            )
            appDao.insertApps(defaultApps)

            // Also create folders in virtual filesystem
            for (app in defaultApps) {
                fs.createDirectory("/data/app/${app.packageName}")
                fs.createDirectory("/data/data/${app.packageName}")
            }
        }
    }

    suspend fun installApkFromUri(uri: Uri): Result<VirtualAppEntity> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_install_${System.currentTimeMillis()}.apk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not read APK from source"))

            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
            val pkgInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
                ?: return@withContext Result.failure(Exception("Invalid APK: Unable to parse package manifest"))

            pkgInfo.applicationInfo?.sourceDir = tempFile.absolutePath
            pkgInfo.applicationInfo?.publicSourceDir = tempFile.absolutePath

            val packageName = pkgInfo.packageName
            val appName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
            val versionName = pkgInfo.versionName ?: "1.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val perms = pkgInfo.requestedPermissions?.joinToString(", ") ?: "None"
            val targetSdk = pkgInfo.applicationInfo?.targetSdkVersion ?: 34
            val minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                pkgInfo.applicationInfo?.minSdkVersion ?: 24
            } else {
                24
            }

            // Extract icon if present
            var iconBase64: String? = null
            try {
                val drawable = pkgInfo.applicationInfo?.loadIcon(pm)
                if (drawable != null) {
                    val bitmap = drawableToBitmap(drawable)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, stream)
                    iconBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                // Ignore icon failure
            }

            // Save into virtual filesystem
            val virtualApkDir = "/data/app/$packageName"
            fs.createDirectory(virtualApkDir)
            fs.createDirectory("/data/data/$packageName")
            val vApkPath = "$virtualApkDir/base.apk"
            tempFile.inputStream().use { input ->
                fs.importFromInputStream(virtualApkDir, "base.apk", input)
            }
            tempFile.delete()

            val entity = VirtualAppEntity(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                installDate = System.currentTimeMillis(),
                apkPath = vApkPath,
                sizeBytes = fs.resolveVirtualPath(vApkPath).length(),
                permissions = perms,
                isSystemApp = false,
                isRunning = false,
                iconBase64 = iconBase64,
                targetSdk = targetSdk,
                minSdk = minSdk
            )

            appDao.insertApp(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fs.delete("/data/app/$packageName")
            fs.delete("/data/data/$packageName")
            appDao.deleteByPackageName(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun launchApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        appDao.setAppRunningState(packageName, true)
        true
    }

    suspend fun stopApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        appDao.setAppRunningState(packageName, false)
        true
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
