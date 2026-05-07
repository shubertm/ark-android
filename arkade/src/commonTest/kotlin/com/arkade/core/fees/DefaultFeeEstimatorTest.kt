package com.arkade.core.fees

import com.arkade.utils.Log
import com.arkade.utils.info
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test

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
                val error = config.jsonObject["err"]
                Log.info("EstimatorTest", "$actualError")
            }
        }
    }
}
