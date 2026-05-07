package com.arkade.cel

expect fun parseAndInvoke(
    program: String,
    args: Map<String, Any>,
): Any
