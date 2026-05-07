package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin

data class Fee(
    val coin: Coin,
) {
    fun add(other: Fee): Fee {
        val currentAmount = coin.amount
        val sum = currentAmount + other.coin.amount
        return Fee(Coin(Coin.Unit.SATOSHI, sum))
    }
}
