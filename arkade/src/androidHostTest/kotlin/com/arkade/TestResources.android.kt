package com.arkade

actual fun readJsonFile(name: String) =
    requireNotNull(
        Thread
            .currentThread()
            .contextClassLoader
            ?.getResourceAsStream(name),
    ) {
        "File $name not found in resources"
    }.bufferedReader().readText()
