package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class Fee(
    val coin: Coin,
) {
    fun add(other: Fee): Fee {
        val currentAmount = coin.amount
        val sum = currentAmount + other.coin.amount
        return Fee(Coin(Coin.Unit.SATOSHI, sum))
    }

    companion object {
        val ZERO = Fee(Coin(Coin.Unit.SATOSHI, BigDecimal.ZERO))
    }
}
