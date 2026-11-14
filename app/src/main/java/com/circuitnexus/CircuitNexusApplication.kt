package com.circuitnexus

import android.app.Application
import com.circuitnexus.di.AppContainer
import com.circuitnexus.util.HapticFeedback

class CircuitNexusApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        HapticFeedback.initialize(this)
    }
}
