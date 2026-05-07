package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin

data class FeeOutput(
    val coin: Coin,
    val script: String,
) {
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount,
            "script" to script,
        )
}
