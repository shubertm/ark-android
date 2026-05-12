package com.arkade.core.fees

import com.arkade.cel.Program
import com.arkade.cel.parseAndInvoke
import com.arkade.cel.validate
import com.arkade.core.bitcoin.Coin

/**
 * Default implementation of the Ark intent fee estimator.
 *
 * Accepts an [IntentFeeInfo] at construction time and immediately validates all non-null CEL
 * expressions by compiling them against their respective environments. Construction will throw
 * if any configured expression is syntactically or semantically invalid.
 *
 * Fee estimation methods return [Fee.ZERO] when the corresponding program is not configured
 * (i.e., the [IntentFeeInfo] field is `null`). When a program is configured, the CEL expression
 * is evaluated with the input/output's argument map, and the numeric result (which must be a
 * non-negative [Double]) is interpreted as a satoshi amount and wrapped in a [Fee].
 *
 * **Error handling:**
 * - Construction throws if any configured CEL program fails to compile.
 * - Estimation methods throw if the CEL result is not a [Double] or is negative.
 *
 * @param intentFeeInfo The CEL fee program configuration received from the Ark server.
 * @throws Exception if any non-null CEL expression in [intentFeeInfo] fails to compile.
 */
class DefaultFeeEstimator(
    private val intentFeeInfo: IntentFeeInfo,
) {
    init {
        if (intentFeeInfo.onChainInput != null) validate(Program.OnChainInputProgram(intentFeeInfo.onChainInput))
        if (intentFeeInfo.onChainOutput != null) validate(Program.OnChainOutputProgram(intentFeeInfo.onChainOutput))
        if (intentFeeInfo.offChainInput != null) validate(Program.OffChainInputProgram(intentFeeInfo.offChainInput))
        if (intentFeeInfo.offChainOutput != null) validate(Program.OffChainOutputProgram(intentFeeInfo.offChainOutput))
    }

    /**
     * Estimates the fee for a single on-chain input (UTXO).
     *
     * Returns [Fee.ZERO] if no on-chain input program is configured.
     *
     * @param input The on-chain input to estimate a fee for.
     * @return The estimated [Fee] for [input].
     * @throws IllegalArgumentException if the CEL result is not a [Double] or is negative.
     */
    fun estimateOnChainInputFee(input: OnChainInput): Fee {
        if (intentFeeInfo.onChainInput == null) return Fee.ZERO
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OnChainInputProgram(intentFeeInfo.onChainInput), args)
    }

    /**
     * Estimates the fee for a single off-chain input (VTXO or Note).
     *
     * Returns [Fee.ZERO] if no off-chain input program is configured.
     *
     * @param input The off-chain input to estimate a fee for.
     * @return The estimated [Fee] for [input].
     * @throws IllegalArgumentException if the CEL result is not a [Double] or is negative.
     */
    fun estimateOffChainInputFee(input: OffChainInput): Fee {
        if (intentFeeInfo.offChainInput == null) return Fee.ZERO
        val args = input.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OffChainInputProgram(intentFeeInfo.offChainInput), args)
    }

    /**
     * Estimates the fee for a single on-chain output.
     *
     * Returns [Fee.ZERO] if no on-chain output program is configured.
     *
     * @param output The on-chain output to estimate a fee for.
     * @return The estimated [Fee] for [output].
     * @throws IllegalArgumentException if the CEL result is not a [Double] or is negative.
     */
    fun estimateOnChainOutputFee(output: FeeOutput): Fee {
        if (intentFeeInfo.onChainOutput == null) return Fee.ZERO
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OnChainOutputProgram(intentFeeInfo.onChainOutput), args)
    }

    /**
     * Estimates the fee for a single off-chain output.
     *
     * Returns [Fee.ZERO] if no off-chain output program is configured.
     *
     * @param output The off-chain output to estimate a fee for.
     * @return The estimated [Fee] for [output].
     * @throws IllegalArgumentException if the CEL result is not a [Double] or is negative.
     */
    fun estimateOffChainOutputFee(output: FeeOutput): Fee {
        if (intentFeeInfo.offChainOutput == null) return Fee.ZERO
        val args = output.toCelArgs()
        return parseAndInvokeIntentFeeProgram(Program.OffChainOutputProgram(intentFeeInfo.offChainOutput), args)
    }

    /**
     * Estimates the aggregate fee for a full transaction by summing fees across all inputs and
     * outputs.
     *
     * Returns [Fee.ZERO] immediately if all four lists are empty. Otherwise, accumulates fees by
     * calling the per-item estimators for each element in each list.
     *
     * @param onChainInputs The on-chain inputs (UTXOs) included in the transaction.
     * @param offChainInputs The off-chain inputs (VTXOs/Notes) included in the transaction.
     * @param onChainOutputs The on-chain outputs included in the transaction.
     * @param offChainOutputs The off-chain outputs included in the transaction.
     * @return The total estimated [Fee] for the transaction.
     * @throws IllegalArgumentException if any CEL result is not a [Double] or is negative.
     */
    fun estimateFee(
        onChainInputs: List<OnChainInput>,
        offChainInputs: List<OffChainInput>,
        onChainOutputs: List<FeeOutput>,
        offChainOutputs: List<FeeOutput>,
    ): Fee {
        if (onChainInputs.isEmpty() && offChainInputs.isEmpty() && onChainOutputs.isEmpty() && offChainOutputs.isEmpty()) {
            return Fee.ZERO
        }

        var fee = Fee.ZERO

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

    /**
     * Evaluates the given [program] with [args] and interprets the result as a satoshi fee amount.
     *
     * @param program The CEL program to evaluate.
     * @param args The variable bindings to pass to the program.
     * @return A [Fee] wrapping the satoshi result.
     * @throws IllegalArgumentException if the result is not a [Double] or is negative.
     */
    private fun parseAndInvokeIntentFeeProgram(
        program: Program,
        args: Map<String, Any>,
    ): Fee {
        val fee = parseAndInvoke(program, args)
        require(fee is Double) { "Expected return type to be double, got ${fee::class.simpleName}" }
        require(fee >= 0) { "Fee cannot be negative: $fee" }
        return Fee(Coin.fromSatoshi(fee.toLong()))
    }
}
