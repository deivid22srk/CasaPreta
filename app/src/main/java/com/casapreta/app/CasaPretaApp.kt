package com.casapreta.app

import android.app.Application
import rikka.shizuku.Shizuku

class CasaPretaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sui (Magisk module) auto-initialization is handled by ShizukuProvider
        // since Shizuku v12.1.0. Nothing extra to call here.
    }
}
