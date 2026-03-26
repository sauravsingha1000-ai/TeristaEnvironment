package com.terista.environment.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001

        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            // BlackBox init (keep this)
            BlackBoxCore.get().onBeforeMainActivityOnCreate(this)

            setContentView(viewBinding.root)

            // Handle system bars (status + nav)
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

            // ✅ SINGLE SCREEN (NO VIEWPAGER)
            loadSingleFragment()

            initFab()
            initToolbarSubTitle()

            checkStoragePermission()

            BlackBoxCore.get().onAfterMainActivityOnCreate(this)

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    // ✅ Load ONLY ONE fragment
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
        try {
            viewBinding.fab.setOnClickListener {
                val intent = Intent(this, ListActivity::class.java)
                intent.putExtra("userID", currentUser)
                apkPathResult.launch(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initFab: ${e.message}")
        }
    }

    fun showFloatButton(show: Boolean) {
        try {
            val tranY = Resolution.convertDpToPixel(120F, App.getContext())
            val time = 200L

            if (show) {
                viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time).start()
            } else {
                viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time).start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in showFloatButton: ${e.message}")
        }
    }

    private fun initToolbarSubTitle() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in initToolbarSubTitle: ${e.message}")
        }
    }

    private fun updateUserRemark(userId: Int) {
        try {
            var remark = AppManager.mRemarkSharedPreferences
                .getString("Remark$userId", "User $userId")

            if (remark.isNullOrEmpty()) {
                remark = "User $userId"
            }

            viewBinding.toolbarLayout.toolbar.subtitle = remark
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user remark: ${e.message}")
            viewBinding.toolbarLayout.toolbar.subtitle = "User $userId"
        }
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error handling APK result: ${e.message}")
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating menu: ${e.message}")
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in menu click: ${e.message}")
            return false
        }
    }

    private fun checkStoragePermission() {
        // ✅ Keep your original permission logic here (unchanged)
    }

    private fun showErrorDialog(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "Error")
                message(text = message)
                positiveButton(text = "OK") { finish() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}")
            finish()
        }
    }
}
