package com.arkade.core.fees

/**
 * Holds the CEL expression strings for intent-based fee estimation, one for each transaction role.
 *
 * Each property is a CEL expression string that the Ark server provides to compute fees for the
 * corresponding part of a transaction. A `null` value means the server has not configured a fee
 * program for that role, and [DefaultFeeEstimator] will return [Fee.ZERO] for those roles.
 *
 * Empty protobuf strings are automatically mapped to `null` by [fromProtoBuf].
 *
 * @property onChainInputExpression CEL expression for estimating fees on on-chain inputs (UTXOs), or `null`.
 * @property onChainOutputExpression CEL expression for estimating fees on on-chain outputs, or `null`.
 * @property offChainInputExpression CEL expression for estimating fees on off-chain inputs (VTXOs/Notes), or `null`.
 * @property offChainOutputExpression CEL expression for estimating fees on off-chain outputs, or `null`.
 */
data class IntentFeeInfo(
    val onChainInputExpression: String?,
    val onChainOutputExpression: String?,
    val offChainInputExpression: String?,
    val offChainOutputExpression: String?,
) {
    companion object {
        /**
         * Creates an [IntentFeeInfo] from a protobuf `ark.v1.IntentFeeInfo` message.
         *
         * Empty protobuf string fields are converted to `null`, treating absent and empty
         * configurations equivalently.
         *
         * @param intentFeeInfo The protobuf message to convert.
         * @return An [IntentFeeInfo] with `null` for any fields that were empty in the protobuf.
         */
        fun fromProtoBuf(intentFeeInfo: ark.v1.IntentFeeInfo): IntentFeeInfo =
            IntentFeeInfo(
                intentFeeInfo.onchain_input.ifEmpty { null },
                intentFeeInfo.onchain_output.ifEmpty { null },
                intentFeeInfo.offchain_input.ifEmpty { null },
                intentFeeInfo.offchain_output.ifEmpty { null },
            )
    }
}
