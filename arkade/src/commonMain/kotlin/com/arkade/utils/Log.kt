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

fun Log.success(
    tag: String,
    message: String,
    indent: Int = 0,
) {
    var indentString = ""
    for (i in 0..indent) {
        indentString = indentString.plus("  ")
    }
    info(tag, "\u001B[32m$indentString✓ PASSED: $message")
}
