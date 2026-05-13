package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * Represents a fee amount expressed as a [Coin].
 *
 * This is a simple value type used throughout the fee estimation system. Arithmetic is performed
 * in satoshi units to avoid precision issues when summing fees across inputs and outputs.
 *
 * @property coin The underlying [Coin] value representing the fee amount.
 */
data class Fee(
    val coin: Coin,
) {
    /**
     * Returns a new [Fee] that is the sum of this fee and [other].
     *
     * Both fees are converted to satoshi units before summing, and the result is expressed
     * in satoshis.
     *
     * @param other The fee to add to this fee.
     * @return A new [Fee] whose satoshi amount equals the sum of both fees.
     */
    fun add(other: Fee): Fee {
        val currentAmount = coin.toSatoshi().amount
        val sum = currentAmount + other.coin.toSatoshi().amount
        return Fee(Coin(Coin.Unit.SATOSHI, sum))
    }

    companion object {
        /**
         * A [Fee] representing zero satoshis. Used as the initial accumulator value when
         * aggregating fees across a transaction's inputs and outputs.
         */
        val ZERO = Fee(Coin(Coin.Unit.SATOSHI, BigDecimal.ZERO))
    }
}
