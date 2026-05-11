package com.arkade.core.bitcoin

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.require

/**
 * The `Coin` class represents the value and [Unit] of a [Utxo],
 * the [Unit] can be [Unit.BTC] or [Unit.SATOSHI]
 */
data class Coin(
    val unit: Unit,
    val amount: BigDecimal,
) {
    init {
        require(amount >= 0) { "Amount cannot be negative" }

        when (unit) {
            Unit.BTC -> require(amount.scale <= 8) { "BTC amount must have at most 8 decimal places" }
            Unit.SATOSHI -> require(amount.scale <= 0) { "Satoshi amount must have no decimal places" }
        }
    }

    /**
     * Convert this [Coin] from [Unit.BTC] to [Unit.SATOSHI]
     */
    fun toSatoshi(): Coin {
        if (unit == Unit.SATOSHI) return this
        val sats = amount.multiply(Unit.BASE).scale(0)
        return Coin(Unit.SATOSHI, sats)
    }

    /**
     * Convert this [Coin] from [Unit.SATOSHI] to [Unit.BTC]
     */
    fun toBTC(): Coin {
        if (unit == Unit.BTC) return this
        val btc = amount.divide(Unit.BASE).scale(8)
        return Coin(Unit.BTC, btc)
    }

    /**
     * The `Unit` enum class represents the units in which a [Utxo] value can be presented in.
     */
    enum class Unit {
        BTC,
        SATOSHI,
        ;

        companion object {
            internal val BASE = BigDecimal.fromLong(100_000_000L)
        }
    }

    companion object {
        /**
         * @param satoshi is the amount of money in [Unit.SATOSHI]
         * @return [Coin] in [Unit.SATOSHI]
         */
        fun fromSatoshi(satoshi: Long): Coin = Coin(Unit.SATOSHI, satoshi.toBigDecimal())

        /**
         * @param btc is the amount of money in [Unit.BTC]
         * @return [Coin] in [Unit.BTC]
         */
        fun fromBTC(btc: BigDecimal): Coin = Coin(Unit.BTC, btc)
    }
}
