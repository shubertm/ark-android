package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DefaultFeeEstimatorTest {
    val validTestDataPath = "./src/commonTest/kotlin/com/arkade/fixtures/arkfee-valid.json".toPath()
    val invalidTestDataPath = "./src/commonTest/kotlin/com/arkade/fixtures/arkfee-invalid.json".toPath()
    val validTestData =
        FileSystem.SYSTEM.read(validTestDataPath) {
            Json.parseToJsonElement(this.readUtf8())
        }
    val invalidTestData =
        FileSystem.SYSTEM.read(invalidTestDataPath) {
            Json.parseToJsonElement(this.readUtf8())
        }

    @Test
    fun `fail on invalid fee info`() {
        val configs = invalidTestData.jsonObject["invalidConfigs"]?.jsonArray!!

        for (config in configs) {
            val intentFeeInfo =
                IntentFeeInfo(
                    config.jsonObject["config"]
                        ?.jsonObject["onchainInputProgram"]
                        ?.toString()
                        ?.removeSurrounding("\""),
                    config.jsonObject["config"]
                        ?.jsonObject["onchainOutputProgram"]
                        ?.toString()
                        ?.removeSurrounding("\""),
                    config.jsonObject["config"]
                        ?.jsonObject["offchainInputProgram"]
                        ?.toString()
                        ?.removeSurrounding("\""),
                    config.jsonObject["config"]
                        ?.jsonObject["offchainOutputProgram"]
                        ?.toString()
                        ?.removeSurrounding("\""),
                )

            runCatching {
                DefaultFeeEstimator(intentFeeInfo)
            }.onFailure { actualError ->
                val expectedError = config.jsonObject["err"].toString()
                if (expectedError.contains("syntax error", true)) {
                    assertTrue(
                        actualError.message?.contains("syntax", true)!! ||
                            actualError.message?.contains("unexpected", true)!! ||
                            actualError.message?.contains("unterminated", true)!! ||
                            actualError.message?.contains("token", true)!! ||
                            actualError.message?.contains("EOF", true)!!,
                    )
                    return@onFailure
                }
                if (expectedError.contains("undeclared reference", true)) {
                    assertTrue(
                        actualError.message?.contains("undeclared", true)!! ||
                            actualError.message?.contains("unknown variable", true)!! ||
                            actualError.message?.contains("found no matching overload", true)!!,
                    )
                    return@onFailure
                }
                if (expectedError.contains("found no matching overload", true)) {
                    assertTrue(
                        actualError.message?.contains("no such overload", true)!! ||
                            actualError.message?.contains("matching overload", true)!!,
                    )
                    return@onFailure
                }
                assertTrue(expectedError.contains(actualError.message!!))
            }
        }
    }

    @Test
    fun `should return zero on estimate on-chain input program missing`() {
        val intentFeeInfo =
            IntentFeeInfo(
                null,
                null,
                null,
                null,
            )
        val feeEstimator = DefaultFeeEstimator(intentFeeInfo)

        val fee = feeEstimator.estimateOnChainInputFee(OnChainInput(Coin.fromSatoshi(1000)))

        assertEquals(Fee.ZERO, fee)
    }

    @Test
    fun `should estimate on-chain input fee correctly`() {
        val onChainInputProgramData = validTestData.jsonObject["evalOnchainInput"]?.jsonArray!!
        println("On-chain Input Fee Estimation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for (programData in onChainInputProgramData) {
            val name = programData.jsonObject["name"]!!.toString()
            val program = programData.jsonObject["program"]!!.toString().removeSurrounding("\"")
            val cases = programData.jsonObject["cases"]!!.jsonArray
            val intentInfo =
                IntentFeeInfo(
                    program,
                    null,
                    null,
                    null,
                )
            val feeEstimator = DefaultFeeEstimator(intentInfo)

            println()

            for (case in cases) {
                val caseName = case.jsonObject["name"]!!.toString().removeSurrounding("\"")
                val inputAmount =
                    case.jsonObject["input"]!!
                        .jsonObject["amount"]!!
                        .jsonPrimitive.double
                val expectedFeeAmount = case.jsonObject["expected"]!!.jsonPrimitive.double
                runCatching {
                    val fee = feeEstimator.estimateOnChainInputFee(OnChainInput(Coin.fromSatoshi(inputAmount.toLong())))
                    assertEquals(expectedFeeAmount.toBigDecimal(), fee.coin.amount)
                }.onFailure {
                    println("   ❌ FAILED: $caseName")
                    throw it
                }
                println("   ✓ PASSED: $caseName")
            }

            println("✓ PASSED: $name")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @Test
    fun `should return zero on estimate off-chain input program missing`() {
        val intentFeeInfo =
            IntentFeeInfo(
                null,
                null,
                null,
                null,
            )
        val feeEstimator = DefaultFeeEstimator(intentFeeInfo)

        val fee =
            feeEstimator.estimateOffChainInputFee(
                OffChainInput(
                    Coin.fromSatoshi(1000),
                    5.days,
                    Clock.System
                        .now()
                        .epochSeconds
                        .toDuration(DurationUnit.SECONDS),
                    OffChainInput.Companion.Type.VTXO,
                    0.0,
                ),
            )

        assertEquals(Fee.ZERO, fee)
    }

    @Test
    fun `should estimate off-chain input fee correctly`() {
        val onChainInputProgramData = validTestData.jsonObject["evalOffchainInput"]?.jsonArray!!
        println("Off-chain Input Fee Estimation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for (programData in onChainInputProgramData) {
            val name = programData.jsonObject["name"]!!.toString()
            val program = programData.jsonObject["program"]!!.toString().removeSurrounding("\"")
            val cases = programData.jsonObject["cases"]!!.jsonArray
            val intentInfo =
                IntentFeeInfo(
                    null,
                    null,
                    program,
                    null,
                )
            val feeEstimator = DefaultFeeEstimator(intentInfo)

            println()

            for (case in cases) {
                val caseName = case.jsonObject["name"]!!.toString().removeSurrounding("\"")
                val inputAmount =
                    case.jsonObject["input"]
                        ?.jsonObject["amount"]
                        ?.jsonPrimitive
                        ?.long
                val birth =
                    Clock.System.now().epochSeconds +
                        (
                            case.jsonObject["input"]
                                ?.jsonObject["birthOffsetSeconds"]
                                ?.jsonPrimitive
                                ?.long ?: 0
                        )
                val expiry =
                    Clock.System.now().epochSeconds +
                        (
                            case.jsonObject["input"]
                                ?.jsonObject["expiryOffsetSeconds"]
                                ?.jsonPrimitive
                                ?.long ?: 0
                        )
                val type =
                    case.jsonObject["input"]
                        ?.jsonObject["type"]
                        ?.jsonPrimitive
                        ?.toString()
                        ?.removeSurrounding("\"")
                val weight =
                    case.jsonObject["input"]
                        ?.jsonObject["weight"]
                        ?.jsonPrimitive
                        ?.double
                val offChainInput =
                    OffChainInput(
                        Coin.fromSatoshi(inputAmount ?: 0),
                        expiry.toDuration(DurationUnit.SECONDS),
                        birth.toDuration(DurationUnit.SECONDS),
                        OffChainInput.Companion.Type.fromString(type ?: "vtxo"),
                        weight ?: 0.0,
                    )
                val expectedFeeAmount = case.jsonObject["expected"]!!.jsonPrimitive.double
                runCatching {
                    val fee = feeEstimator.estimateOffChainInputFee(offChainInput)
                    assertEquals(expectedFeeAmount.toBigDecimal(), fee.coin.amount)
                }.onFailure {
                    println("   ❌ FAILED: $caseName")
                    throw it
                }
                println("   ✓ PASSED: $caseName")
            }

            println("✓ PASSED: $name")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @Test
    fun `should return zero on estimate on-chain output program missing`() {
        val intentFeeInfo =
            IntentFeeInfo(
                null,
                null,
                null,
                null,
            )
        val feeEstimator = DefaultFeeEstimator(intentFeeInfo)

        val fee =
            feeEstimator.estimateOnChainOutputFee(
                FeeOutput(
                    Coin.fromSatoshi(1000),
                    "",
                ),
            )

        assertEquals(Fee.ZERO, fee)
    }

    @Test
    fun `should estimate on-chain output fee correctly`() {
        val onChainInputProgramData = validTestData.jsonObject["evalOnchainOutput"]?.jsonArray!!
        println("On-chain Output Fee Estimation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for (programData in onChainInputProgramData) {
            val name = programData.jsonObject["name"]!!.toString()
            val program = programData.jsonObject["program"]!!.toString().removeSurrounding("\"")
            val cases = programData.jsonObject["cases"]!!.jsonArray
            val intentInfo =
                IntentFeeInfo(
                    null,
                    program,
                    null,
                    null,
                )
            val feeEstimator = DefaultFeeEstimator(intentInfo)

            println()

            for (case in cases) {
                val caseName = case.jsonObject["name"]!!.toString().removeSurrounding("\"")
                val inputAmount =
                    case.jsonObject["output"]
                        ?.jsonObject["amount"]
                        ?.jsonPrimitive
                        ?.long
                val script =
                    case.jsonObject["output"]
                        ?.jsonObject["script"]
                        ?.jsonPrimitive
                        ?.toString()
                        ?.removeSurrounding("\"")
                val onChainOutput =
                    FeeOutput(
                        Coin.fromSatoshi(inputAmount ?: 0),
                        script ?: "",
                    )
                val expectedFeeAmount = case.jsonObject["expected"]!!.jsonPrimitive.double
                runCatching {
                    val fee = feeEstimator.estimateOnChainOutputFee(onChainOutput)
                    assertEquals(expectedFeeAmount.toBigDecimal(), fee.coin.amount)
                }.onFailure {
                    println("   ❌ FAILED: $caseName")
                    throw it
                }
                println("   ✓ PASSED: $caseName")
            }

            println("✓ PASSED: $name")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @Test
    fun `should return zero on estimate off-chain output program missing`() {
        val intentFeeInfo =
            IntentFeeInfo(
                null,
                null,
                null,
                null,
            )
        val feeEstimator = DefaultFeeEstimator(intentFeeInfo)

        val fee =
            feeEstimator.estimateOffChainOutputFee(
                FeeOutput(
                    Coin.fromSatoshi(1000),
                    "",
                ),
            )

        assertEquals(Fee.ZERO, fee)
    }

    @Test
    fun `should estimate off-chain output fee correctly`() {
        val onChainInputProgramData = validTestData.jsonObject["evalOffchainOutput"]?.jsonArray!!
        println("Off-chain Output Fee Estimation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for (programData in onChainInputProgramData) {
            val name = programData.jsonObject["name"]!!.toString()
            val program = programData.jsonObject["program"]!!.toString().removeSurrounding("\"")
            val cases = programData.jsonObject["cases"]!!.jsonArray
            val intentInfo =
                IntentFeeInfo(
                    null,
                    null,
                    null,
                    program,
                )
            val feeEstimator = DefaultFeeEstimator(intentInfo)

            println()

            for (case in cases) {
                val caseName = case.jsonObject["name"]!!.toString().removeSurrounding("\"")
                val inputAmount =
                    case.jsonObject["output"]
                        ?.jsonObject["amount"]
                        ?.jsonPrimitive
                        ?.long
                val script =
                    case.jsonObject["output"]
                        ?.jsonObject["script"]
                        ?.jsonPrimitive
                        ?.toString()
                        ?.removeSurrounding("\"")
                val onChainOutput =
                    FeeOutput(
                        Coin.fromSatoshi(inputAmount ?: 0),
                        script ?: "",
                    )
                val expectedFeeAmount = case.jsonObject["expected"]!!.jsonPrimitive.double
                runCatching {
                    val fee = feeEstimator.estimateOffChainOutputFee(onChainOutput)
                    assertEquals(expectedFeeAmount.toBigDecimal(), fee.coin.amount)
                }.onFailure {
                    println("   ❌ FAILED: $caseName")
                    throw it
                }
                println("   ✓ PASSED: $caseName")
            }

            println("✓ PASSED: $name")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @Test
    fun `should estimate fees correctly`() {
        val onChainInputProgramData = validTestData.jsonObject["eval"]?.jsonArray!!
        println("Fee Estimation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for (programData in onChainInputProgramData) {
            val name = programData.jsonObject["name"]!!.toString()
            val onChainInputProgram = programData.jsonObject["onchainInputProgram"]?.toString()?.removeSurrounding("\"")
            val offChainInputProgram = programData.jsonObject["offchainInputProgram"]?.toString()?.removeSurrounding("\"")
            val onChainOutputProgram = programData.jsonObject["onchainOutputProgram"]?.toString()?.removeSurrounding("\"")
            val offChainOutputProgram = programData.jsonObject["offchainOutputProgram"]?.toString()?.removeSurrounding("\"")
            val cases = programData.jsonObject["cases"]?.jsonArray!!
            val intentInfo =
                IntentFeeInfo(
                    onChainInputProgram,
                    onChainOutputProgram,
                    offChainInputProgram,
                    offChainOutputProgram,
                )
            val feeEstimator = DefaultFeeEstimator(intentInfo)

            println()

            for (case in cases) {
                val caseName = case.jsonObject["name"]!!.toString().removeSurrounding("\"")
                val onChainInputs =
                    (case.jsonObject["onchainInputs"]?.jsonArray ?: emptyList()).map {
                        OnChainInput(Coin.fromSatoshi(it.jsonObject["amount"]?.jsonPrimitive?.long ?: 0))
                    }
                val offChainInputs =
                    (case.jsonObject["offchainInputs"]?.jsonArray ?: emptyList()).map {
                        OffChainInput(
                            Coin.fromSatoshi(it.jsonObject["amount"]?.jsonPrimitive?.long ?: 0),
                            it.jsonObject["expiry"]
                                ?.jsonPrimitive
                                ?.long
                                ?.toDuration(DurationUnit.SECONDS) ?: Duration.ZERO,
                            it.jsonObject["birth"]
                                ?.jsonPrimitive
                                ?.long
                                ?.toDuration(DurationUnit.SECONDS) ?: Duration.ZERO,
                            OffChainInput.Companion.Type.fromString(
                                it.jsonObject["type"]
                                    ?.jsonPrimitive
                                    ?.toString()
                                    ?.removeSurrounding("\"") ?: "vtxo",
                            ),
                            it.jsonObject["weight"]?.jsonPrimitive?.double ?: 0.0,
                        )
                    }
                val onChainOutputs =
                    (case.jsonObject["onchainOutputs"]?.jsonArray ?: emptyList()).map {
                        FeeOutput(
                            Coin.fromSatoshi(it.jsonObject["amount"]?.jsonPrimitive?.long ?: 0),
                            it.jsonObject["script"]
                                ?.jsonPrimitive
                                ?.toString()
                                ?.removeSurrounding("\"") ?: "",
                        )
                    }
                val offChainOutputs =
                    (case.jsonObject["offchainOutputs"]?.jsonArray ?: emptyList()).map {
                        FeeOutput(
                            Coin.fromSatoshi(it.jsonObject["amount"]?.jsonPrimitive?.long ?: 0),
                            it.jsonObject["script"]
                                ?.jsonPrimitive
                                ?.toString()
                                ?.removeSurrounding("\"") ?: "",
                        )
                    }

                val expectedFeeAmount = case.jsonObject["expected"]!!.jsonPrimitive.double
                runCatching {
                    val fee =
                        feeEstimator.estimateFee(
                            onChainInputs,
                            offChainInputs,
                            onChainOutputs,
                            offChainOutputs,
                        )
                    assertEquals(expectedFeeAmount.toBigDecimal(), fee.coin.amount)
                }.onFailure {
                    println("   ❌ FAILED: $caseName")
                    throw it
                }
                println("   ✓ PASSED: $caseName")
            }

            println("✓ PASSED: $name")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}
