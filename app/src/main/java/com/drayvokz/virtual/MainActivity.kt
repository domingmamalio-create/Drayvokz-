```kotlin
package com.drayvokz.virtual

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var container: ContainerManager
    private lateinit var adapter: AppAdapter
    private val scope = CoroutineScope(Dispatchers.Main)

    private val pickApk = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                container.installApkFromUri(uri) { ok, msg ->
                    toast(if (ok) "Installed: $msg" else "Failed: $msg")
                    refresh()
                }
            }
        }
    }

    private val provisionProfile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            toast("Profile provisioned.")
            refresh()
        } else {
            toast("Cancelled/failed.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)
        container = ContainerManager(this)

        adapter = AppAdapter(mutableListOf()) { app ->
            container.launchInContainer(app.packageName)
        }

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.btnCreate).setOnClickListener { createProfile() }
        findViewById<Button>(R.id.btnRemove).setOnClickListener { removeProfile() }
        findViewById<Button>(R.id.btnInstall).setOnClickListener { pickApkFile() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        scope.launch {
            val status = findViewById<TextView>(R.id.txtStatus)
            val running = container.isProfileRunning()
            status.text = "status: " + (if (running) "RUNNING" else "NOT SET UP")
            val apps = withContext(Dispatchers.IO) { container.listInstalledApps() }
            adapter.replace(apps)
        }
    }

    private fun createProfile() {
        if (container.profileAvailable()) {
            toast("Profile already exists.")
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
        }
        try {
            provisionProfile.launch(intent)
        } catch (e: Exception) {
            toast("Manual: Settings → Accounts → Add work profile")
            startActivity(Intent(Settings.ACTION_ADD_ACCOUNT))
        }
    }

    private fun removeProfile() {
        toast("I-remove sa Settings → Accounts → Work")
    }

    private fun pickApkFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }
        try { pickApk.launch(intent) } catch (e: Exception) { toast("No file picker.") }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
```
