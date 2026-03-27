package com.terista.environment.view.setting

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.terista.environment.R
import com.terista.environment.app.AppManager
import com.terista.environment.util.toast
import com.terista.environment.view.base.BaseActivity
import com.terista.environment.view.gms.GmsManagerActivity
import top.niunaijun.blackbox.BlackBoxCore

class SettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_modern)

        // SWITCHES
        val hideRoot = findViewById<SwitchMaterial>(R.id.switch_hide_root)
        val daemon = findViewById<SwitchMaterial>(R.id.switch_daemon)
        val vpn = findViewById<SwitchMaterial>(R.id.switch_vpn)
        val flagSecure = findViewById<SwitchMaterial>(R.id.switch_flag_secure)

        // BUTTONS
        val gms = findViewById<TextView>(R.id.btn_gms)
        val logs = findViewById<TextView>(R.id.btn_logs)

        // LOAD STATES
        hideRoot.isChecked = AppManager.mBlackBoxLoader.hideRoot()
        daemon.isChecked = AppManager.mBlackBoxLoader.daemonEnable()
        vpn.isChecked = AppManager.mBlackBoxLoader.useVpnNetwork()
        flagSecure.isChecked = AppManager.mBlackBoxLoader.disableFlagSecure()

        // EVENTS
        hideRoot.setOnCheckedChangeListener { _, b ->
            AppManager.mBlackBoxLoader.invalidHideRoot(b)
            toast(R.string.restart_module)
        }

        daemon.setOnCheckedChangeListener { _, b ->
            AppManager.mBlackBoxLoader.invalidDaemonEnable(b)
            toast(R.string.restart_module)
        }

        vpn.setOnCheckedChangeListener { _, b ->
            if (b) {
                val intent = VpnService.prepare(this)
                if (intent != null) startActivity(intent)
            }
            AppManager.mBlackBoxLoader.invalidUseVpnNetwork(b)
            toast(R.string.restart_module)
        }

        flagSecure.setOnCheckedChangeListener { _, b ->
            AppManager.mBlackBoxLoader.invalidDisableFlagSecure(b)
            toast(R.string.restart_module)
        }

        // GMS
        if (BlackBoxCore.get().isSupportGms) {
            gms.setOnClickListener {
                GmsManagerActivity.start(this)
            }
        } else {
            gms.alpha = 0.5f
        }

        // LOGS
        logs.setOnClickListener {
            logs.isEnabled = false

            BlackBoxCore.get().sendLogs(
                "Manual Log Upload",
                true,
                object : BlackBoxCore.LogSendListener {
                    override fun onSuccess() {
                        runOnUiThread { logs.isEnabled = true }
                    }

                    override fun onFailure(error: String?) {
                        runOnUiThread { logs.isEnabled = true }
                    }
                }
            )

            toast("Sending logs...")
        }
    }

    companion object{
        fun start(context: Context){
            val intent = Intent(context,SettingActivity::class.java)
            context.startActivity(intent)
        }
    }
}
