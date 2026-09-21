```kotlin
package com.drayvokz.virtual

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ContainerManager(private val ctx: Context) {

    private val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(ctx, AdminReceiver::class.java)
    private val pm = ctx.packageManager
    private val um = ctx.getSystemService(Context.USER_SERVICE) as UserManager

    fun profileAvailable(): Boolean {
        return try {
            dpm.isProfileOwnerApp(ctx.packageName) ||
                dpm.isDeviceOwnerApp(ctx.packageName) ||
                um.userProfiles.size > 1
        } catch (e: Exception) { false }
    }

    fun isProfileRunning(): Boolean = profileAvailable()

    fun launchInContainer(pkg: String) {
        try {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent == null) {
                android.widget.Toast.makeText(ctx, "Cannot launch", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(ctx, "Err: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun installApkFromUri(uri: Uri, onDone: (Boolean, String) -> Unit) {
        Thread {
            try {
                val apksDir = File(ctx.filesDir, "apks").apply { mkdirs() }
                val dest = File(apksDir, "payload_${System.currentTimeMillis()}.apk")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", dest),
                        "application/vnd.android.package-archive"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(installIntent)
                onDone(true, dest.name)
            } catch (e: Exception) {
                onDone(false, e.message ?: "unknown")
            }
        }.start()
    }

    suspend fun listInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val out = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()
        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getInstalledApplications(0)
            }
            for (ai in packages) {
                if (ai.packageName == ctx.packageName) continue
                if (!seen.add(ai.packageName)) continue
                val label = pm.getApplicationLabel(ai).toString()
                val icon = try { pm.getApplicationIcon(ai.packageName) } catch (e: Exception) { null }
                out.add(AppInfo(ai.packageName, label, icon))
            }
        } catch (_: Exception) {}
        out.sortedBy { it.label.lowercase() }
    }
}
```
