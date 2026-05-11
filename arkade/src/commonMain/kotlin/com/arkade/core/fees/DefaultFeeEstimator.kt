package com.arkade.core.fees

import com.arkade.cel.Program
import com.arkade.cel.parseAndInvoke
import com.arkade.cel.validate
import com.arkade.core.bitcoin.Coin

class DefaultFeeEstimator(
    private val intentFeeInfo: IntentFeeInfo,
) {
    init {
        if (intentFeeInfo.onChainInput != null) validate(Program.OnChainInputProgram(intentFeeInfo.onChainInput))
        if (intentFeeInfo.onChainOutput != null) validate(Program.OnChainOutputProgram(intentFeeInfo.onChainOutput))
        if (intentFeeInfo.offChainInput != null) validate(Program.OffChainInputProgram(intentFeeInfo.offChainInput))
        if (intentFeeInfo.offChainOutput != null) validate(Program.OffChainOutputProgram(intentFeeInfo.offChainOutput))
    }

    fun estimateOnChainInputFee(input: OnChainInput): Fee {
        if (intentFeeInfo.onChainInput == null) return Fee.ZERO
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OnChainInputProgram(intentFeeInfo.onChainInput), args)
    }

    fun estimateOffChainInputFee(input: OffChainInput): Fee {
        if (intentFeeInfo.offChainInput == null) return Fee.ZERO
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OffChainInputProgram(intentFeeInfo.offChainInput), args)
    }

    fun estimateOnChainOutputFee(output: FeeOutput): Fee {
        if (intentFeeInfo.onChainOutput == null) return Fee.ZERO
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OnChainOutputProgram(intentFeeInfo.onChainOutput), args)
    }

    fun estimateOffChainOutputFee(output: FeeOutput): Fee {
        if (intentFeeInfo.offChainOutput == null) return Fee.ZERO
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OffChainOutputProgram(intentFeeInfo.offChainOutput), args)
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
        program: Program,
        args: Map<String, Any>,
    ): Fee {
        val fee = parseAndInvoke(program, args)
        if (fee !is Double) throw Error("Expected return type to be double, got ${fee::class.simpleName}")
        require(fee >= 0) { "Fee cannot be negative: $fee" }
        return Fee(Coin.fromSatoshi(fee.toLong()))
    }
}
