package com.arkade.cel

/**
 * A sealed class representing a CEL (Common Expression Language) program scoped to a specific
 * transaction role within the Ark fee estimation system.
 *
 * Each subclass determines the set of CEL variables available during evaluation. All program types
 * have access to the built-in `now()` function, which returns the current epoch time in seconds
 * as a [Double].
 *
 * @property expression The CEL expression string to be compiled and evaluated.
 */
sealed class Program(
    val expression: String,
) {
    /**
     * A CEL program for estimating fees on on-chain inputs (UTXOs).
     *
     * Available variables:
     * - `amount: Double` — the coin amount in satoshis
     *
     * @param expression The CEL expression string.
     */
    class OnChainInputProgram(
        expression: String,
    ) : Program(expression)

    /**
     * A CEL program for estimating fees on on-chain outputs.
     *
     * Available variables:
     * - `amount: Double` — the coin amount in satoshis
     * - `script: String` — the output script
     *
     * @param expression The CEL expression string.
     */
    class OnChainOutputProgram(
        expression: String,
    ) : Program(expression)

    /**
     * A CEL program for estimating fees on off-chain inputs (Ark VTXOs/Notes).
     *
     * Available variables:
     * - `amount: Double` — the coin amount in satoshis
     * - `expiry: Double` — the expiry time in whole seconds
     * - `birth: Double` — the birth time in whole seconds
     * - `inputType: String` — the input type as a lowercase string (e.g., "vtxo", "note", "recoverable")
     * - `weight: Double` — the weight factor for the input
     *
     * @param expression The CEL expression string.
     */
    class OffChainInputProgram(
        expression: String,
    ) : Program(expression)

    /**
     * A CEL program for estimating fees on off-chain outputs.
     *
     * Available variables:
     * - `amount: Double` — the coin amount in satoshis
     * - `script: String` — the output script
     *
     * @param expression The CEL expression string.
     */
    class OffChainOutputProgram(
        expression: String,
    ) : Program(expression)
}

/**
 * Compiles and evaluates the given [program]'s CEL expression with the provided [args].
 *
 * This is an `expect` function with platform-specific `actual` implementations for Android,
 * JVM, and Native targets.
 *
 * @param program The [Program] whose expression will be compiled and evaluated.
 * @param args A map of variable names to their runtime values, matching the variables declared
 *   for the program type.
 * @return The result of evaluating the CEL expression. For fee estimation, this is expected to be
 *   a [Double] representing the fee in satoshis.
 * @throws Exception if the expression fails to compile or evaluate.
 */
expect fun parseAndInvoke(
    program: Program,
    args: Map<String, Any>,
): Any

/**
 * Compiles the given [program]'s CEL expression without evaluating it, to confirm the expression
 * is syntactically and semantically valid.
 *
 * This is an `expect` function with platform-specific `actual` implementations for Android,
 * JVM, and Native targets.
 *
 * @param program The [Program] whose expression will be validated.
 * @throws Exception if the expression fails to compile (e.g., syntax error, undeclared reference,
 *   type mismatch).
 */
expect fun validate(program: Program)
