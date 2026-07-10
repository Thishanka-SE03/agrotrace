package com.agrotrace.scanner.di

import android.content.Context
import com.agrotrace.scanner.data.preferences.ScannerPreferences
import com.agrotrace.scanner.data.remote.ApiFactory
import com.agrotrace.scanner.data.repository.OcrScanRepository
import com.agrotrace.scanner.data.repository.OcrScanRepositoryImpl

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences = ScannerPreferences(appContext)
    private val api = ApiFactory.create()

    val repository: OcrScanRepository = OcrScanRepositoryImpl(
        context = appContext,
        api = api,
        preferences = preferences
    )
}
