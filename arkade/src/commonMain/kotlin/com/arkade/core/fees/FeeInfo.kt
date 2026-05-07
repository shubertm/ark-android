package com.arkade.core.fees

data class FeeInfo(
    val intent: IntentFeeInfo,
    val rate: Float,
) {
    companion object {
        fun fromProtBuf(feeInfo: ark.v1.FeeInfo?): FeeInfo? {
            if (feeInfo == null) return null
            if (feeInfo.intent_fee == null) return null
            return FeeInfo(
                IntentFeeInfo.fromProtoBuf(feeInfo.intent_fee),
                feeInfo.tx_fee_rate.toFloat(),
            )
        }
    }
}
