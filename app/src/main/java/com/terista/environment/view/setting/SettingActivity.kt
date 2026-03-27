package com.terista.environment.view.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.terista.environment.R
import com.terista.environment.view.base.BaseActivity

class SettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 NEW UI
        setContentView(R.layout.settings_modern)

        // (Optional toolbar later)
    }

    companion object{
        fun start(context: Context){
            val intent = Intent(context,SettingActivity::class.java)
            context.startActivity(intent)
        }
    }
}
