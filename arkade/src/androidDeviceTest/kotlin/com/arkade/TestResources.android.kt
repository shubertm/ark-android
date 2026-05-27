package com.arkade

import androidx.test.platform.app.InstrumentationRegistry

actual fun readJsonFile(name: String) =
    InstrumentationRegistry
        .getInstrumentation()
        .context
        .assets
        .open(name)
        .bufferedReader()
        .readText()
