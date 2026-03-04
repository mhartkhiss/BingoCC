package com.mkz.bingocard.vision

import android.app.Application
import android.content.Context

object AndroidContextProvider {
    @Volatile
    lateinit var appContext: Context
        private set

    fun init(app: Application) {
        appContext = app.applicationContext
    }
}
