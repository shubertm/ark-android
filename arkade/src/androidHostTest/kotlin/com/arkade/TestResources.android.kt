package com.arkade

actual fun readJsonFile(name: String) =
    Thread
        .currentThread()
        .contextClassLoader!!
        .getResourceAsStream(name)!!
        .bufferedReader()
        .readText()
