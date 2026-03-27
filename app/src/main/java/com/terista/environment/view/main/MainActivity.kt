package com.terista.environment.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.android.material.floatingactionbutton.FloatingActionButton
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.app.App
import com.terista.environment.app.AppManager
import com.terista.environment.util.Resolution
import com.terista.environment.view.base.LoadingActivity
import com.terista.environment.view.fake.FakeManagerActivity
import com.terista.environment.view.list.ListActivity
import com.terista.environment.view.setting.SettingActivity

// ✅ NEW (SAFE IMPORTS)
import android.os.Build
import android.view.WindowManager

class MainActivity : LoadingActivity() {

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

            setContentView(R.layout.activity_main)

            // 🔥 SMOOTH FADE (SAFE)
            window.decorView.alpha = 0f
            window.decorView.animate().alpha(1f).setDuration(250).start()

            // 🔥 REAL BLUR (ANDROID 12+ ONLY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setBackgroundBlurRadius(80)
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }

            // 🔥 INSETS (UNCHANGED)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                val header = findViewById<android.view.View>(R.id.header)
                header.setPadding(
                    header.paddingLeft,
                    systemBars.top,
                    header.paddingRight,
                    header.paddingBottom
                )

                val fab = findViewById<android.view.View>(R.id.fab)
                fab.translationY = -systemBars.bottom.toFloat()

                insets
            }

            BlackBoxCore.get().onAfterMainActivityOnCreate(this)

            // ✅ ORIGINAL PERMISSION FLOW (UNCHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                if (android.os.Environment.isExternalStorageManager()) {
                    loadAppsFragment()
                } else {
                    showStoragePermissionDialog()
                }

            } else {
                loadAppsFragment()
            }

            initFab()
            initUserSubtitle()

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    private fun loadAppsFragment() {
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

    private fun showStoragePermissionDialog() {
        MaterialDialog(this).show {
            title(text = "Storage Permission Required")
            message(text = "This app needs All Files Access permission to show and install apps properly.")

            positiveButton(text = "Grant Permission") {
                openStorageSettings()
            }

            negativeButton(text = "Continue Anyway") {
                loadAppsFragment()
            }
        }
    }

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

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                loadAppsFragment()
            }
        }

    override fun onResume() {
        super.onResume()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {

                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (fragment == null) {
                    loadAppsFragment()
                }
            }
        }
    }

    private fun initFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", currentUser)
            apkPathResult.launch(intent)
        }

        // 🔥 FLOAT ANIMATION (SAFE)
        fab.animate()
            .translationYBy(-20f)
            .setDuration(1000)
            .withEndAction {
                fab.animate().translationYBy(20f).setDuration(1000).start()
            }
            .start()
    }

    private fun initUserSubtitle() {
        val subtitle = findViewById<TextView>(R.id.subtitle_user)

        updateUserRemark(currentUser)

        subtitle.setOnClickListener {
            MaterialDialog(this).show {
                title(res = R.string.userRemark)
                input(
                    hintRes = R.string.userRemark,
                    prefill = subtitle.text
                ) { _, input ->
                    AppManager.mRemarkSharedPreferences.edit {
                        putString("Remark$currentUser", input.toString())
                        subtitle.text = input
                    }
                }
                positiveButton(res = R.string.done)
                negativeButton(res = R.string.cancel)
            }
        }
    }

    private fun updateUserRemark(userId: Int) {
        val subtitle = findViewById<TextView>(R.id.subtitle_user)

        var remark = AppManager.mRemarkSharedPreferences
            .getString("Remark$userId", "User $userId")

        if (remark.isNullOrEmpty()) {
            remark = "User $userId"
        }

        subtitle.text = remark
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
