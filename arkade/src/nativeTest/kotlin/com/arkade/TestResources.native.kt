package com.arkade

import okio.FileSystem
import okio.Path.Companion.toPath

actual fun readJsonFile(name: String) = FileSystem.SYSTEM.read(name.toPath()) { readUtf8() }
