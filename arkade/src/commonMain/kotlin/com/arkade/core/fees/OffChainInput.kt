package com.arkade.core.fees

import com.arkade.core.bitcoin.Coin
import kotlin.time.Duration

/**
 * Represents an off-chain input (Ark VTXO or Note) for fee estimation.
 *
 * Provides [toCelArgs] to convert the input's properties into the variable map expected by an
 * off-chain input CEL fee program.
 *
 * @property coin The coin value of this input.
 * @property expiry The duration until this input expires (used to assess time-sensitivity).
 * @property birth The age of this input since it was created.
 * @property type The type of off-chain input (see [Type]).
 * @property weight A weight factor that can influence the fee calculation for this input.
 */
data class OffChainInput(
    val coin: Coin,
    val expiry: Duration,
    val birth: Duration,
    val type: Type,
    val weight: Double,
) {
    /**
     * Converts this input into a CEL argument map for use with an off-chain input fee program.
     *
     * The returned map contains:
     * - `"amount"` → [Double] — the coin amount from [coin]
     * - `"expiry"` → [Double] — [expiry] expressed as whole seconds
     * - `"birth"` → [Double] — [birth] expressed as whole seconds
     * - `"inputType"` → [String] — the lowercase name of [type] (e.g; `"vtxo"`, `"note"`, `"recoverable"`)
     * - `"weight"` → [Double] — the [weight] factor
     *
     * @return A [Map] of variable names to values for CEL evaluation.
     */
    fun toCelArgs(): Map<String, Any> =
        mapOf(
            "amount" to coin.amount.doubleValue(),
            "expiry" to expiry.inWholeSeconds.toDouble(),
            "birth" to birth.inWholeSeconds.toDouble(),
            "inputType" to type.name.lowercase(),
            "weight" to weight,
        )

    companion object {
        /**
         * Classifies the nature of an off-chain input.
         *
         * This value is exposed to CEL programs as the `inputType` variable (lowercased).
         */
        enum class Type {
            /** A recoverable VTXO that has not yet expired and can still be swept. */
            RECOVERABLE,

            /** A Note-type off-chain asset. */
            NOTE,

            /** A standard Virtual Transaction Output (VTXO). */
            VTXO,
            ;

            companion object {
                /**
                 * Returns the [Type] whose name matches [name] (case-insensitive).
                 *
                 * @param name The type name string to parse.
                 * @return The matching [Type].
                 * @throws IllegalArgumentException if [name] does not match any known type.
                 */
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
