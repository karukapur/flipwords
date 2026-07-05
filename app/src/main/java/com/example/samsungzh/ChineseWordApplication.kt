package com.example.samsungzh

import android.app.Application

class ChineseWordApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WordUpdateScheduler.schedule(this)
    }
}
