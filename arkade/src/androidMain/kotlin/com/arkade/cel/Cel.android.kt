package com.arkade.cel

import dev.cel.bundle.Cel
import dev.cel.bundle.CelBuilder
import dev.cel.bundle.CelFactory
import dev.cel.common.CelFunctionDecl
import dev.cel.common.CelOverloadDecl
import dev.cel.common.types.SimpleType
import dev.cel.runtime.CelFunctionBinding
import kotlin.time.Clock

/**
 * Creates a base [CelBuilder] pre-configured with common declarations shared by all fee
 * estimation program types on Android.
 *
 * The builder declares:
 * - `amount: Double` — the coin amount variable available in all program environments
 * - `now()` — a global CEL function bound to [Clock.System.now] that returns the current
 *   epoch time in seconds as a [Double]
 *
 * Use this as a starting point and extend it with additional variables for specific program types.
 *
 * @return A [CelBuilder] pre-configured with the `amount` variable and `now()` function.
 */
fun celEnvironmentBuilder(): CelBuilder {
    val nowSignature: CelFunctionDecl =
        CelFunctionDecl.newFunctionDeclaration(
            "now",
            CelOverloadDecl.newGlobalOverload(
                "nowTimestamp",
                SimpleType.DOUBLE,
            ),
        )

    val nowFunction: CelFunctionBinding =
        CelFunctionBinding.from(
            "nowTimestamp",
            emptyList(),
        ) { _ ->
            Clock.System
                .now()
                .epochSeconds
                .toDouble()
        }

    return CelFactory
        .standardCelBuilder()
        .addVar("amount", SimpleType.DOUBLE)
        .addFunctionDeclarations(nowSignature)
        .addFunctionBindings(nowFunction)
}

/**
 * Returns the appropriate [Cel] environment for the given [program] type on Android.
 *
 * Each program type gets a CEL environment with the variables it is allowed to reference:
 *
 * | Program type              | Available variables                                      |
 * |---------------------------|----------------------------------------------------------|
 * | [Program.OnChainInputProgram]  | `amount`                                            |
 * | [Program.OffChainInputProgram] | `amount`, `expiry`, `birth`, `inputType`, `weight`  |
 * | [Program.OnChainOutputProgram] | `amount`, `script`                                  |
 * | [Program.OffChainOutputProgram]| `amount`, `script`                                  |
 *
 * All environments also expose the `now()` built-in function.
 *
 * @param program The [Program] for which to select a CEL environment.
 * @return A fully built [Cel] environment matching the program's variable scope.
 */
fun getCelEnvironment(program: Program): Cel {
    val intentOnChainInputCelEnvironment = celEnvironmentBuilder().build()

    val intentOffChainInputCelEnvironment =
        celEnvironmentBuilder()
            .addVar("expiry", SimpleType.DOUBLE)
            .addVar("birth", SimpleType.DOUBLE)
            .addVar("inputType", SimpleType.STRING)
            .addVar("weight", SimpleType.DOUBLE)
            .build()

    val intentOutputCelEnvironment =
        celEnvironmentBuilder()
            .addVar("script", SimpleType.STRING)
            .build()

    return when (program) {
        is Program.OnChainInputProgram -> intentOnChainInputCelEnvironment
        is Program.OnChainOutputProgram -> intentOutputCelEnvironment
        is Program.OffChainInputProgram -> intentOffChainInputCelEnvironment
        is Program.OffChainOutputProgram -> intentOutputCelEnvironment
    }
}

/**
 * Android `actual` implementation of [parseAndInvoke].
 *
 * Selects the appropriate CEL environment for [program], compiles [program]'s expression,
 * creates an executable CEL program from the resulting AST, and evaluates it with [args].
 *
 * @param program The [Program] whose expression will be compiled and evaluated.
 * @param args A map of variable names to their runtime values.
 * @return The result of evaluating the CEL expression.
 * @throws Exception if compilation or evaluation fails.
 */
actual fun parseAndInvoke(
    program: Program,
    args: Map<String, Any>,
): Any {
    val cel = getCelEnvironment(program)
    val celProgram = cel.createProgram(cel.compile(program.expression).ast)
    return celProgram.eval(args)
}

/**
 * Android `actual` implementation of [validate].
 *
 * Selects the appropriate CEL environment for [program] and compiles [program]'s expression
 * without evaluating it, to verify that the expression is syntactically and semantically valid.
 *
 * @param program The [Program] whose expression will be validated.
 * @throws Exception if the expression fails to compile.
 */
actual fun validate(program: Program) {
    val cel = getCelEnvironment(program)
    cel.compile(program.expression)
}
