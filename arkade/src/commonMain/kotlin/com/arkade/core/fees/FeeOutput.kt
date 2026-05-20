package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin

/**
 * Represents an output used for fee estimation in the Ark fee system.
 *
 * This model is shared for both on-chain and off-chain output fee estimation. It provides
 * [toCelArgs] to convert the output's data into the variable map expected by the corresponding
 * CEL fee program.
 *
 * @property coin The coin value of the output.
 * @property script The output script (e.g; a hex-encoded locking script).
 */
data class FeeOutput(
    val coin: Coin,
    val script: String,
) {
    /**
     * Converts this output into a CEL argument map for use with an output fee program.
     *
     * The returned map contains:
     * - `"amount"` → [Double] — the coin amount from [coin]
     * - `"script"` → [String] — the raw output script from [script]
     *
     * @return A [Map] of variable names to values for CEL evaluation.
     */
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount.doubleValue(),
            "script" to script,
        )
}
