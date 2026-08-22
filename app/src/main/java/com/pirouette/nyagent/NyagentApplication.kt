package com.pirouette.nyagent

import android.app.Application
import com.pirouette.nyagent.presentation.ServiceLocator

/** Application entry point that owns the [ServiceLocator] for dependency wiring. */
class NyagentApplication : Application() {

    val serviceLocator: ServiceLocator by lazy {
        ServiceLocator(this)
    }
}
