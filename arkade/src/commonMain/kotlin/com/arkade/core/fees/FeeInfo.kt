package com.arkade.core.fees

data class FeeInfo(
    val intent: IntentFeeInfo?,
    val rate: Float,
) {
    companion object {
        fun fromProtBuf(feeInfo: ark.v1.FeeInfo?): FeeInfo? {
            if (feeInfo == null) return null
            return FeeInfo(
                feeInfo.intent_fee?.let(IntentFeeInfo::fromProtoBuf),
                feeInfo.tx_fee_rate.toFloat(),
            )
        }
    }
}
