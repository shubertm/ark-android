package com.arkade.cel

import dev.cel.bundle.Cel
import dev.cel.bundle.CelFactory
import dev.cel.common.CelFunctionDecl
import dev.cel.common.CelOverloadDecl
import dev.cel.common.types.SimpleType
import dev.cel.runtime.CelFunctionBinding
import kotlin.time.Clock

/**
 * Returns the appropriate [Cel] environment for the given [program] type on JVM.
 *
 * Each program type gets a CEL environment with the variables it is allowed to reference.
 * All environments include the `now()` built-in function, which is a global overload (named
 * `nowTimestamp`) that returns the current epoch time in seconds as a [Double].
 *
 * | Program type                    | Available variables                                      |
 * |---------------------------------|----------------------------------------------------------|
 * | [Program.OnChainInputProgram]   | `amount`                                                 |
 * | [Program.OffChainInputProgram]  | `amount`, `expiry`, `birth`, `inputType`, `weight`       |
 * | [Program.OnChainOutputProgram]  | `amount`, `script`                                       |
 * | [Program.OffChainOutputProgram] | `amount`, `script`                                       |
 *
 * @param program The [Program] for which to select a CEL environment.
 * @return A fully built [Cel] environment matching the program's variable scope.
 */
fun getCelEnvironment(program: Program): Cel {
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

    val intentOnChainInputCelEnvironment =
        CelFactory
            .standardCelBuilder()
            .addVar("amount", SimpleType.DOUBLE)
            .addFunctionDeclarations(nowSignature)
            .addFunctionBindings(nowFunction)
            .build()

    val intentOffChainInputCelEnvironment =
        CelFactory
            .standardCelBuilder()
            .addVar("amount", SimpleType.DOUBLE)
            .addVar("expiry", SimpleType.DOUBLE)
            .addVar("birth", SimpleType.DOUBLE)
            .addVar("inputType", SimpleType.STRING)
            .addVar("weight", SimpleType.DOUBLE)
            .addFunctionDeclarations(nowSignature)
            .addFunctionBindings(nowFunction)
            .build()

    val intentOutputCelEnvironment =
        CelFactory
            .standardCelBuilder()
            .addVar("amount", SimpleType.DOUBLE)
            .addVar("script", SimpleType.STRING)
            .addFunctionDeclarations(nowSignature)
            .addFunctionBindings(nowFunction)
            .build()

    return when (program) {
        is Program.OnChainInputProgram -> intentOnChainInputCelEnvironment
        is Program.OnChainOutputProgram -> intentOutputCelEnvironment
        is Program.OffChainInputProgram -> intentOffChainInputCelEnvironment
        is Program.OffChainOutputProgram -> intentOutputCelEnvironment
    }
}

/**
 * JVM `actual` implementation of [parseAndInvoke].
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
 * JVM `actual` implementation of [validate].
 *
 * Selects the appropriate CEL environment for [program] and compiles [program]'s expression
 * without evaluating it, to verify that the expression is syntactically and semantically valid.
 *
 * @param program The [Program] whose expression will be validated.
 * @throws Exception if the expression fails to compile.
 */
actual fun validate(program: Program) {
    val cel = getCelEnvironment(program)
    cel.compile(program.expression).ast
}
