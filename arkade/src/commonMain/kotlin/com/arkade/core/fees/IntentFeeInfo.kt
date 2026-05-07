package com.arkade.core.fees

data class IntentFeeInfo(
    val onChainInput: String,
    val onChainOutput: String,
    val offChainInput: String,
    val offChainOutput: String,
) {
    companion object {
        fun fromProtoBuf(intentFeeInfo: ark.v1.IntentFeeInfo): IntentFeeInfo =
            IntentFeeInfo(
                intentFeeInfo.onchain_input,
                intentFeeInfo.onchain_output,
                intentFeeInfo.offchain_input,
                intentFeeInfo.offchain_output,
            )
    }
}
