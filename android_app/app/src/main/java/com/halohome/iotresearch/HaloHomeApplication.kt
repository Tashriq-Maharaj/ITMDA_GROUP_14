package com.halohome.iotresearch

import android.app.Application

class HaloHomeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HaloHomeApplication
            private set
    }
}
