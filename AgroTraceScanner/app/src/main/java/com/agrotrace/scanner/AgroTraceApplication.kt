package com.agrotrace.scanner

import android.app.Application
import com.agrotrace.scanner.di.AppContainer

class AgroTraceApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
