package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin

/**
 * Represents an on-chain input (UTXO) for fee estimation.
 *
 * Provides [toCelArgs] to convert the input's coin value into the variable map expected by an
 * on-chain input CEL fee program.
 *
 * @property coin The coin value of this UTXO.
 */
data class OnChainInput(
    val coin: Coin,
) {
    /**
     * Converts this input into a CEL argument map for use with an on-chain input fee program.
     *
     * The returned map contains:
     * - `"amount"` → [Double] — the coin amount from [coin]
     *
     * @return A [Map] of variable names to values for CEL evaluation.
     */
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount.doubleValue(),
        )
}
