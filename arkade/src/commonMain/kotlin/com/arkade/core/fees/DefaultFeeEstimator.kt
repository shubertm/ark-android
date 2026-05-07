package com.arkade.core.fees

import com.arkade.cel.parseAndInvoke
import com.arkade.core.bitcoin.Coin

class DefaultFeeEstimator(
    private val intentFeeInfo: IntentFeeInfo,
) {
    fun estimateOnChainInputFee(input: OnChainInput): Fee {
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(intentFeeInfo.onChainInput, args)
    }

    fun estimateOffChainInputFee(input: OffChainInput): Fee {
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(intentFeeInfo.offChainInput, args)
    }

    fun estimateOnChainOutputFee(output: FeeOutput): Fee {
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(intentFeeInfo.onChainOutput, args)
    }

    fun estimateOffChainOutputFee(output: FeeOutput): Fee {
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(intentFeeInfo.offChainOutput, args)
    }

    fun estimateFee(
        onChainInputs: List<OnChainInput>,
        offChainInputs: List<OffChainInput>,
        onChainOutputs: List<FeeOutput>,
        offChainOutputs: List<FeeOutput>,
    ): Fee {
        if (onChainInputs.isEmpty() && offChainInputs.isEmpty() && onChainOutputs.isEmpty() && offChainOutputs.isEmpty()) {
            return Fee(Coin.fromSatoshi(0))
        }

        var fee = Fee(Coin.fromSatoshi(0))

        onChainInputs.forEach {
            fee = fee.add(estimateOnChainInputFee(it))
        }
        offChainInputs.forEach {
            fee = fee.add(estimateOffChainInputFee(it))
        }
        onChainOutputs.forEach {
            fee = fee.add(estimateOnChainOutputFee(it))
        }
        offChainOutputs.forEach {
            fee = fee.add(estimateOffChainOutputFee(it))
        }
        return fee
    }

    private fun parseAndInvokeIntentFeeProgram(
        program: String,
        args: Map<String, Any>,
    ): Fee {
        val fee = parseAndInvoke(program, args)
        if (fee !is Double) throw Error("Expected return type to be double, got ${fee::class.simpleName}")
        return Fee(Coin.fromSatoshi(fee.toLong()))
    }
}
