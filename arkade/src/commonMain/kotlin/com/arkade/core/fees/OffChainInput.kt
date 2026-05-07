package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import kotlin.time.Duration

data class OffChainInput(
    val coin: Coin,
    val expiry: Duration,
    val birth: Duration,
    val type: Type,
    val weight: Int,
) {
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount,
            "expiry" to expiry.inWholeSeconds,
            "birth" to birth.inWholeSeconds,
            "type" to type.name.lowercase(),
            "weight" to weight,
        )

    companion object {
        enum class Type {
            RECOVERABLE,
            NOTE,
            VTXO,
        }
    }
}
