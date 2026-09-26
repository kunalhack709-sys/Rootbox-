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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

data class ApkValidationResult(
    val isValid: Boolean,
    val packageName: String = "",
    val appName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val architecture: String = "arm64-v8a",
    val permissions: List<String> = emptyList(),
    val fileSizeBytes: Long = 0L,
    val iconBase64: String? = null,
    val failureReason: String? = null
)

data class InstallStepState(
    val currentAppName: String = "",
    val currentPackage: String = "",
    val progressPercent: Int = 0,
    val currentStatus: String = "",
    val totalApks: Int = 1,
    val completedApks: Int = 0,
    val batchList: List<BatchItemProgress> = emptyList(),
    val isComplete: Boolean = false,
    val completedApp: VirtualAppEntity? = null,
    val errorMessage: String? = null
)

data class BatchItemProgress(
    val name: String,
    val status: String // PENDING, INSTALLING, SUCCESS, FAILED
)

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
                    appName = "Termux",
                    versionName = "0.118.0",
                    versionCode = 118,
                    sizeBytes = 42 * 1024 * 1024L,
                    dataSizeBytes = 12 * 1024 * 1024L,
                    cacheSizeBytes = 2 * 1024 * 1024L,
                    permissions = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE, android.permission.VIBRATE, android.permission.ACCESS_SUPERUSER",
                    isSystemApp = true,
                    isRunning = false,
                    rootAccessGranted = true,
                    targetSdk = 34,
                    minSdk = 24,
                    architecture = "arm64-v8a"
                ),
                VirtualAppEntity(
                    packageName = "stericson.busybox.virtual",
                    appName = "BusyBox Installer",
                    versionName = "1.36.1",
                    versionCode = 361,
                    sizeBytes = 18 * 1024 * 1024L,
                    dataSizeBytes = 4 * 1024 * 1024L,
                    cacheSizeBytes = 512 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = true,
                    isRunning = false,
                    rootAccessGranted = true,
                    targetSdk = 34,
                    minSdk = 24,
                    architecture = "arm64-v8a"
                ),
                VirtualAppEntity(
                    packageName = "com.speedsoftware.rootexplorer.virtual",
                    appName = "Root Explorer",
                    versionName = "4.12.0",
                    versionCode = 412,
                    sizeBytes = 24 * 1024 * 1024L,
                    dataSizeBytes = 6 * 1024 * 1024L,
                    cacheSizeBytes = 1024 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = false,
                    isRunning = false,
                    rootAccessGranted = true,
                    targetSdk = 34,
                    minSdk = 24,
                    architecture = "arm64-v8a"
                ),
                VirtualAppEntity(
                    packageName = "eu.chainfire.supersu.virtual",
                    appName = "RootBox SuperSU",
                    versionName = "2.82-SR5",
                    versionCode = 282,
                    sizeBytes = 14 * 1024 * 1024L,
                    dataSizeBytes = 2 * 1024 * 1024L,
                    cacheSizeBytes = 256 * 1024L,
                    permissions = "android.permission.ACCESS_SUPERUSER, android.permission.RECEIVE_BOOT_COMPLETED",
                    isSystemApp = true,
                    isRunning = false,
                    rootAccessGranted = true,
                    targetSdk = 33,
                    minSdk = 24,
                    architecture = "arm64-v8a"
                ),
                VirtualAppEntity(
                    packageName = "com.rootbox.sqlitebrowser",
                    appName = "SQLite Inspector",
                    versionName = "3.42.0",
                    versionCode = 342,
                    sizeBytes = 16 * 1024 * 1024L,
                    dataSizeBytes = 3 * 1024 * 1024L,
                    cacheSizeBytes = 512 * 1024L,
                    permissions = "android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE",
                    isSystemApp = false,
                    isRunning = false,
                    rootAccessGranted = false,
                    targetSdk = 34,
                    minSdk = 24,
                    architecture = "arm64-v8a"
                )
            )
            appDao.insertApps(defaultApps)

            for (app in defaultApps) {
                fs.createDirectory("/data/app/${app.packageName}")
                fs.createDirectory("/data/data/${app.packageName}/files")
                fs.createDirectory("/data/data/${app.packageName}/cache")
                fs.createDirectory("/data/data/${app.packageName}/shared_prefs")
            }
        }
    }

    suspend fun validateApk(uri: Uri): ApkValidationResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "validate_${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext ApkValidationResult(
                isValid = false,
                failureReason = "Unable to read APK from provided Android storage URI."
            )

            if (tempFile.length() < 1024L) {
                return@withContext ApkValidationResult(
                    isValid = false,
                    failureReason = "File is corrupt or empty (Size: ${tempFile.length()} bytes). Not a valid Android package archive."
                )
            }

            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
            val pkgInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
                ?: return@withContext ApkValidationResult(
                    isValid = false,
                    failureReason = "Invalid APK structure: Android framework cannot parse the package manifest. The APK file may be corrupted, truncated, or incompatible."
                )

            pkgInfo.applicationInfo?.sourceDir = tempFile.absolutePath
            pkgInfo.applicationInfo?.publicSourceDir = tempFile.absolutePath

            val packageName = pkgInfo.packageName ?: ""
            if (packageName.isBlank()) {
                return@withContext ApkValidationResult(
                    isValid = false,
                    failureReason = "APK manifest is missing a valid Android package name."
                )
            }

            val minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                pkgInfo.applicationInfo?.minSdkVersion ?: 21
            } else {
                21
            }

            val virtualOsMaxApi = 34
            if (minSdk > virtualOsMaxApi) {
                return@withContext ApkValidationResult(
                    isValid = false,
                    packageName = packageName,
                    failureReason = "Minimum SDK version ($minSdk) exceeds the RootBox virtual Android image runtime (API $virtualOsMaxApi - Android 14)."
                )
            }

            val appName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
            val versionName = pkgInfo.versionName ?: "1.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val perms = pkgInfo.requestedPermissions?.toList() ?: emptyList()

            // Icon extraction
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
                // Ignore icon error
            }

            ApkValidationResult(
                isValid = true,
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = pkgInfo.applicationInfo?.targetSdkVersion ?: 34,
                architecture = "arm64-v8a",
                permissions = perms,
                fileSizeBytes = tempFile.length(),
                iconBase64 = iconBase64
            )
        } catch (e: Exception) {
            ApkValidationResult(
                isValid = false,
                failureReason = "Validation error: ${e.localizedMessage ?: "Unknown parsing failure"}"
            )
        } finally {
            tempFile.delete()
        }
    }

    suspend fun installApkFromUriWithProgress(
        uri: Uri,
        onProgress: (InstallStepState) -> Unit
    ): Result<VirtualAppEntity> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "install_flow_${System.currentTimeMillis()}.apk")
        try {
            onProgress(InstallStepState(progressPercent = 10, currentStatus = "Reading APK from storage..."))
            delay(150)

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not read APK from source URI"))

            onProgress(InstallStepState(progressPercent = 25, currentStatus = "Validating package manifest & architecture..."))
            delay(200)

            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
            val pkgInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
                ?: return@withContext Result.failure(Exception("Invalid APK: Unable to parse package manifest"))

            pkgInfo.applicationInfo?.sourceDir = tempFile.absolutePath
            pkgInfo.applicationInfo?.publicSourceDir = tempFile.absolutePath

            val packageName = pkgInfo.packageName ?: "unknown.package"
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

            onProgress(
                InstallStepState(
                    currentAppName = appName,
                    currentPackage = packageName,
                    progressPercent = 50,
                    currentStatus = "Allocating virtual storage in /data/app/$packageName..."
                )
            )
            delay(200)

            // Extract icon
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

            onProgress(
                InstallStepState(
                    currentAppName = appName,
                    currentPackage = packageName,
                    progressPercent = 75,
                    currentStatus = "Installing into RootBox..."
                )
            )
            delay(250)

            // Copy to virtual filesystem
            val virtualApkDir = "/data/app/$packageName"
            fs.createDirectory(virtualApkDir)
            fs.createDirectory("/data/data/$packageName/files")
            fs.createDirectory("/data/data/$packageName/cache")
            fs.createDirectory("/data/data/$packageName/shared_prefs")

            val vApkPath = "$virtualApkDir/base.apk"
            tempFile.inputStream().use { input ->
                fs.importFromInputStream(virtualApkDir, "base.apk", input)
            }

            onProgress(
                InstallStepState(
                    currentAppName = appName,
                    currentPackage = packageName,
                    progressPercent = 90,
                    currentStatus = "Registering package into RootBox package manager..."
                )
            )
            delay(150)

            val entity = VirtualAppEntity(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                installDate = System.currentTimeMillis(),
                apkPath = vApkPath,
                sizeBytes = fs.resolveVirtualPath(vApkPath).length(),
                dataSizeBytes = 1048576L,
                cacheSizeBytes = 524288L,
                permissions = perms,
                isSystemApp = false,
                isRunning = false,
                rootAccessGranted = perms.contains("SUPERUSER"),
                iconBase64 = iconBase64,
                targetSdk = targetSdk,
                minSdk = minSdk,
                architecture = "arm64-v8a"
            )

            appDao.insertApp(entity)

            onProgress(
                InstallStepState(
                    currentAppName = appName,
                    currentPackage = packageName,
                    progressPercent = 100,
                    currentStatus = "Installation complete",
                    isComplete = true,
                    completedApp = entity
                )
            )

            Result.success(entity)
        } catch (e: Exception) {
            onProgress(
                InstallStepState(
                    progressPercent = 0,
                    currentStatus = "Failed",
                    errorMessage = e.localizedMessage
                )
            )
            Result.failure(e)
        } finally {
            tempFile.delete()
        }
    }

    suspend fun installMultipleApks(
        uris: List<Uri>,
        onProgress: (InstallStepState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val batch = uris.mapIndexed { index, _ ->
            BatchItemProgress("APK #${index + 1}", "PENDING")
        }.toMutableList()

        uris.forEachIndexed { index, uri ->
            batch[index] = batch[index].copy(status = "INSTALLING")
            onProgress(
                InstallStepState(
                    totalApks = uris.size,
                    completedApks = index,
                    batchList = batch.toList(),
                    currentStatus = "Installing package ${index + 1} of ${uris.size}..."
                )
            )

            val result = installApkFromUriWithProgress(uri) { stepState ->
                onProgress(
                    stepState.copy(
                        totalApks = uris.size,
                        completedApks = index,
                        batchList = batch.toList()
                    )
                )
            }

            if (result.isSuccess) {
                val app = result.getOrNull()
                batch[index] = BatchItemProgress(app?.appName ?: "APK #${index + 1}", "SUCCESS")
            } else {
                batch[index] = BatchItemProgress("APK #${index + 1}", "FAILED")
            }
        }

        onProgress(
            InstallStepState(
                totalApks = uris.size,
                completedApks = uris.size,
                batchList = batch.toList(),
                progressPercent = 100,
                isComplete = true,
                currentStatus = "Batch installation finished"
            )
        )
    }

    suspend fun installSampleApp(
        sampleKey: String,
        onProgress: (InstallStepState) -> Unit
    ): Result<VirtualAppEntity> = withContext(Dispatchers.IO) {
        val sample = when (sampleKey) {
            "root_checker" -> VirtualAppEntity(
                packageName = "com.rootbox.rootchecker",
                appName = "Root Checker Virtual",
                versionName = "2.1.0",
                versionCode = 21,
                sizeBytes = 8 * 1024 * 1024L,
                dataSizeBytes = 512 * 1024L,
                cacheSizeBytes = 256 * 1024L,
                permissions = "android.permission.ACCESS_SUPERUSER, android.permission.INTERNET",
                isSystemApp = false,
                isRunning = false,
                rootAccessGranted = true,
                targetSdk = 34,
                minSdk = 24
            )
            "demo_sandbox" -> VirtualAppEntity(
                packageName = "com.example.sandboxdemo",
                appName = "Test Application",
                versionName = "1.0.0",
                versionCode = 1,
                sizeBytes = 5 * 1024 * 1024L,
                dataSizeBytes = 1024 * 1024L,
                cacheSizeBytes = 256 * 1024L,
                permissions = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE",
                isSystemApp = false,
                isRunning = false,
                rootAccessGranted = false,
                targetSdk = 34,
                minSdk = 24
            )
            else -> VirtualAppEntity(
                packageName = "org.rootbox.devsample",
                appName = "Developer Sample Tool",
                versionName = "1.4.0",
                versionCode = 14,
                sizeBytes = 12 * 1024 * 1024L,
                dataSizeBytes = 2 * 1024 * 1024L,
                cacheSizeBytes = 512 * 1024L,
                permissions = "android.permission.ACCESS_SUPERUSER, android.permission.READ_EXTERNAL_STORAGE",
                isSystemApp = false,
                isRunning = false,
                rootAccessGranted = true,
                targetSdk = 34,
                minSdk = 24
            )
        }

        onProgress(InstallStepState(currentAppName = sample.appName, currentPackage = sample.packageName, progressPercent = 30, currentStatus = "Allocating virtual storage..."))
        delay(200)
        fs.createDirectory("/data/app/${sample.packageName}")
        fs.createDirectory("/data/data/${sample.packageName}/files")
        fs.createDirectory("/data/data/${sample.packageName}/cache")

        onProgress(InstallStepState(currentAppName = sample.appName, currentPackage = sample.packageName, progressPercent = 75, currentStatus = "Installing into RootBox..."))
        delay(250)
        appDao.insertApp(sample)

        onProgress(InstallStepState(currentAppName = sample.appName, currentPackage = sample.packageName, progressPercent = 100, isComplete = true, completedApp = sample, currentStatus = "Installation complete"))
        Result.success(sample)
    }

    suspend fun clearAppData(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val appDataDir = fs.resolveVirtualPath("/data/data/$packageName")
            if (appDataDir.exists()) {
                appDataDir.deleteRecursively()
                appDataDir.mkdirs()
                File(appDataDir, "files").mkdirs()
                File(appDataDir, "cache").mkdirs()
            }
            appDao.clearAppData(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAppCache(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = fs.resolveVirtualPath("/data/data/$packageName/cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            appDao.clearAppCache(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportApk(packageName: String, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val apkFile = fs.resolveVirtualPath("/data/app/$packageName/base.apk")
            if (apkFile.exists()) {
                apkFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportAppData(packageName: String, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val summary = buildString {
                appendLine("RootBox Virtual Application Data Backup")
                appendLine("Package: $packageName")
                appendLine("Timestamp: ${System.currentTimeMillis()}")
                appendLine("Filesystem Root: /data/data/$packageName")
                val dir = fs.resolveVirtualPath("/data/data/$packageName")
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        appendLine("${file.relativeTo(dir).path} (${file.length()} bytes)")
                    }
                }
            }
            outputStream.write(summary.toByteArray(Charsets.UTF_8))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setRootAccess(packageName: String, granted: Boolean) = withContext(Dispatchers.IO) {
        appDao.setRootAccess(packageName, granted)
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
        appDao.updateLastLaunched(packageName, System.currentTimeMillis())
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
