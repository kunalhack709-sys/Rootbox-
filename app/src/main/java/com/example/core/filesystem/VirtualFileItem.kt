package com.example.core.filesystem

data class VirtualFileItem(
    val name: String,
    val virtualPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val permissions: String = if (isDirectory) "drwxr-xr-x" else "-rw-r--r--",
    val owner: String = "root:root",
    val lastModified: Long = System.currentTimeMillis(),
    val isExecutable: Boolean = false
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "<DIR>"
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format("%.1f MB", mb)
        }
}
