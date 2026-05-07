package com.arkade.cel

import dev.cel.bundle.CelFactory

actual fun parseAndInvoke(
    program: String,
    args: Map<String, Any>,
): Any {
    val cel = CelFactory.standardCelBuilder().build()
    val program = cel.createProgram(cel.compile(program).ast)
    return program.eval(args)
}
