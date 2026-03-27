package com.terista.environment.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.app.App
import com.terista.environment.app.AppManager
import com.terista.environment.databinding.ActivityMainBinding
import com.terista.environment.util.Resolution
import com.terista.environment.util.inflate
import com.terista.environment.view.apps.AppsFragment
import com.terista.environment.view.base.LoadingActivity
import com.terista.environment.view.fake.FakeManagerActivity
import com.terista.environment.view.list.ListActivity
import com.terista.environment.view.setting.SettingActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : LoadingActivity() {

    private val viewBinding: ActivityMainBinding by inflate()

    private var currentUser = 0

    companion object {
        private const val TAG = "MainActivity"

        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            BlackBoxCore.get().onBeforeMainActivityOnCreate(this)

            setContentView(viewBinding.root)

            ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                viewBinding.toolbarLayout.toolbar.setPadding(
                    viewBinding.toolbarLayout.toolbar.paddingLeft,
                    systemBars.top,
                    viewBinding.toolbarLayout.toolbar.paddingRight,
                    viewBinding.toolbarLayout.toolbar.paddingBottom
                )

                viewBinding.fab.translationY = -systemBars.bottom.toFloat()

                insets
            }

            initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)

            BlackBoxCore.get().onAfterMainActivityOnCreate(this)

            // 🔥 CLEAN PERMISSION FLOW
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                if (android.os.Environment.isExternalStorageManager()) {

                    // ✅ Permission already granted
                    loadSingleFragment()

                } else {
                    // ❌ Show dialog first
                    showStoragePermissionDialog()
                }

            } else {
                // ✅ Older Android → no need special permission
                loadSingleFragment()
            }

            initFab()
            initToolbarSubTitle()

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    // ✅ DIALOG
    private fun showStoragePermissionDialog() {
        MaterialDialog(this).show {
            title(text = "Storage Permission Required")

            message(text = "This app needs All Files Access permission to show and install apps properly.\n\nPlease allow it in the next screen.")

            positiveButton(text = "Grant Permission") {
                openStorageSettings()
            }

            negativeButton(text = "Cancel") {
                loadSingleFragment()
            }
        }
    }

    // ✅ OPEN SETTINGS
    private fun openStorageSettings() {
        val intent = try {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        } catch (e: Exception) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }

        storagePermissionLauncher.launch(intent)
    }

    // ✅ RESULT HANDLER
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                if (android.os.Environment.isExternalStorageManager()) {
                    Log.d(TAG, "Storage permission granted ✅")

                    loadSingleFragment()

                } else {
                    Log.w(TAG, "Storage permission still denied ❌")

                    loadSingleFragment()
                }
            }
        }

    // ✅ FIXED POSITION
    override fun onResume() {
        super.onResume()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {

                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (fragment == null) {
                    loadSingleFragment()
                }
            }
        }
    }

    private fun loadSingleFragment() {
        try {
            val userId = 0
            currentUser = userId

            val fragment = AppsFragment.newInstance(userId)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment: ${e.message}")
        }
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", currentUser)
            apkPathResult.launch(intent)
        }
    }

    fun showFloatButton(show: Boolean) {
        val tranY = Resolution.convertDpToPixel(120F, App.getContext())
        val time = 200L

        if (show) {
            viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time).start()
        } else {
            viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time).start()
        }
    }

    private fun initToolbarSubTitle() {
        updateUserRemark(currentUser)

        viewBinding.toolbarLayout.toolbar.getChildAt(1)?.setOnClickListener {
            MaterialDialog(this).show {
                title(res = R.string.userRemark)
                input(
                    hintRes = R.string.userRemark,
                    prefill = viewBinding.toolbarLayout.toolbar.subtitle
                ) { _, input ->
                    AppManager.mRemarkSharedPreferences.edit {
                        putString("Remark$currentUser", input.toString())
                        viewBinding.toolbarLayout.toolbar.subtitle = input
                    }
                }
                positiveButton(res = R.string.done)
                negativeButton(res = R.string.cancel)
            }
        }
    }

    private fun updateUserRemark(userId: Int) {
        var remark = AppManager.mRemarkSharedPreferences
            .getString("Remark$userId", "User $userId")

        if (remark.isNullOrEmpty()) {
            remark = "User $userId"
        }

        viewBinding.toolbarLayout.toolbar.subtitle = remark
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { data ->
                    val source = data.getStringExtra("source")

                    if (source != null) {
                        val fragment =
                            supportFragmentManager.findFragmentById(R.id.fragment_container)
                                as? AppsFragment

                        fragment?.installApk(source)
                    }
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.main_git -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/")))
            }

            R.id.main_setting -> {
                SettingActivity.start(this)
            }

            R.id.main_tg -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/")))
            }

            R.id.fake_location -> {
                val intent = Intent(this, FakeManagerActivity::class.java)
                intent.putExtra("userID", 0)
                startActivity(intent)
            }
        }
        return true
    }

    private fun showErrorDialog(message: String) {
        MaterialDialog(this).show {
            title(text = "Error")
            message(text = message)
            positiveButton(text = "OK") { finish() }
        }
    }
}
