package com.arkade.core.fees

/**
 * Fee configuration received from the Ark server.
 *
 * Contains an optional intent-based CEL fee configuration and a transaction fee rate. This is the
 * domain-layer representation of the `ark.v1.FeeInfo` protobuf message.
 *
 * @property intent Optional CEL-based fee programs for intent-type transactions, or `null` if the
 *   server does not support intent fee estimation.
 * @property rate The transaction fee rate (e.g., satoshis per vbyte).
 */
data class FeeInfo(
    val intent: IntentFeeInfo?,
    val rate: Float,
) {
    companion object {
        /**
         * Creates a [FeeInfo] from a protobuf `ark.v1.FeeInfo` message.
         *
         * @param feeInfo The protobuf message to convert, or `null`.
         * @return A [FeeInfo] populated from [feeInfo], or `null` if [feeInfo] is `null`.
         */
        fun fromProtBuf(feeInfo: ark.v1.FeeInfo?): FeeInfo? {
            if (feeInfo == null) return null
            return FeeInfo(
                feeInfo.intent_fee?.let(IntentFeeInfo::fromProtoBuf),
                feeInfo.tx_fee_rate.toFloat(),
            )
        }
    }
}
