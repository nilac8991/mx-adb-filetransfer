package com.zebra.mxadbfiletransfer

import android.app.Application

class DefaultApplication : Application() {

    init {
        INSTANCE = this
    }

    companion object {
        @Volatile
        private var INSTANCE: DefaultApplication? = null

        fun getInstance(): DefaultApplication {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = DefaultApplication()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}