package com.arkade.cel

sealed class Program(
    val expression: String,
) {
    class OnChainInputProgram(
        expression: String,
    ) : Program(expression)

    class OnChainOutputProgram(
        expression: String,
    ) : Program(expression)

    class OffChainInputProgram(
        expression: String,
    ) : Program(expression)

    class OffChainOutputProgram(
        expression: String,
    ) : Program(expression)
}

expect fun parseAndInvoke(
    program: Program,
    args: Map<String, Any>,
): Any

expect fun validate(program: Program)
