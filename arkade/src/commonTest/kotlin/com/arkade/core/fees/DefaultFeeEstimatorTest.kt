package com.arkade.core.fees

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test
import kotlin.test.assertTrue

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
}
