package com.terista.environment.view.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.util.InjectionUtil
import com.terista.environment.view.list.ListViewModel

class WelcomeActivity : AppCompatActivity() {

    override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewInstalledAppList()
        jump()
    }

    private fun jump() {
        MainActivity.start(this)
        finish()
    }

    private fun previewInstalledAppList(){
        val viewModel = ViewModelProvider(this,InjectionUtil.getListFactory()).get(ListViewModel::class.java)
        viewModel.previewInstalledList()
    }
}
