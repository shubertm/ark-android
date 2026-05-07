package com.arkade.cel

import dev.cel.bundle.Cel
import dev.cel.bundle.CelFactory
import dev.cel.common.CelFunctionDecl
import dev.cel.common.CelOverloadDecl
import dev.cel.common.types.SimpleType
import dev.cel.runtime.CelFunctionBinding
import kotlin.time.Clock

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
            listOf(Unit::class.java),
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
            .addVar("type", SimpleType.STRING)
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

actual fun parseAndInvoke(
    program: Program,
    args: Map<String, Any>,
): Any {
    val cel = getCelEnvironment(program)
    val program = cel.createProgram(cel.compile(program.expression).ast)
    return program.eval(args)
}

actual fun validate(program: Program) {
    val cel = getCelEnvironment(program)
    cel.compile(program.expression)
}
