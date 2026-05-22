package com.arkade.utils

object Log

expect fun Log.debug(
    tag: String,
    message: String,
)

expect fun Log.info(
    tag: String,
    message: String,
)

expect fun Log.warning(
    tag: String,
    message: String,
)

expect fun Log.error(
    tag: String,
    message: String,
)

expect fun Log.verbose(
    tag: String,
    message: String,
)

fun Log.drawLine() {
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    skipLine()
}

fun Log.skipLine() {
    println()
}
