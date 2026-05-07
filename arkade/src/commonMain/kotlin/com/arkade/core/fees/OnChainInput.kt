package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin

data class OnChainInput(
    val coin: Coin,
) {
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount,
        )
}
