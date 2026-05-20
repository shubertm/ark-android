package com.arkade.cel

/**
 * Native `actual` implementation of [parseAndInvoke].
 *
 * **Not yet implemented.** CEL evaluation is not currently supported on Kotlin Native targets.
 * Calling this function will throw [NotImplementedError].
 *
 * @param program The [Program] whose expression would be compiled and evaluated.
 * @param args A map of variable names to their runtime values.
 * @return Nothing — always throws.
 * @throws NotImplementedError always.
 */
actual fun parseAndInvoke(
    program: Program,
    args: Map<String, Any>,
): Any {
    TODO("Not yet implemented")
}

/**
 * Native `actual` implementation of [validate].
 *
 * **Not yet implemented.** CEL validation is not currently supported on Kotlin Native targets.
 * Calling this function will throw [NotImplementedError].
 *
 * @param program The [Program] whose expression would be validated.
 * @throws NotImplementedError always.
 */
actual fun validate(program: Program) {
    TODO("Not yet implemented")
}
