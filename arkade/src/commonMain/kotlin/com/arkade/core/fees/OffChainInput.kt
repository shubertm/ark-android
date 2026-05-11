package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import kotlin.time.Duration

data class OffChainInput(
    val coin: Coin,
    val expiry: Duration,
    val birth: Duration,
    val type: Type,
    val weight: Double,
) {
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount.doubleValue(),
            "expiry" to expiry.inWholeSeconds.toDouble(),
            "birth" to birth.inWholeSeconds.toDouble(),
            "inputType" to type.name.lowercase(),
            "weight" to weight,
        )

    companion object {
        enum class Type {
            RECOVERABLE,
            NOTE,
            VTXO,
            ;

            companion object {
                fun fromString(name: String): Type =
                    when (name.lowercase()) {
                        "recoverable" -> RECOVERABLE
                        "note" -> NOTE
                        "vtxo" -> VTXO
                        else -> throw IllegalArgumentException("Unknown type: $name")
                    }
            }
        }
    }
}
