package com.arkade.core.fees

data class IntentFeeInfo(
    val onChainInput: String?,
    val onChainOutput: String?,
    val offChainInput: String?,
    val offChainOutput: String?,
) {
    companion object {
        fun fromProtoBuf(intentFeeInfo: ark.v1.IntentFeeInfo): IntentFeeInfo =
            IntentFeeInfo(
                intentFeeInfo.onchain_input.ifEmpty { null },
                intentFeeInfo.onchain_output.ifEmpty { null },
                intentFeeInfo.offchain_input.ifEmpty { null },
                intentFeeInfo.offchain_output.ifEmpty { null },
            )
    }
}
