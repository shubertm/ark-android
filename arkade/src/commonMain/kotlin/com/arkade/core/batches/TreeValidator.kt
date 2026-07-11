package com.arkade.core.batches

import com.arkade.core.getArkFieldsCosigners
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.crypto.musig2.Musig2
import fr.acinq.bitcoin.psbt.Psbt

object TreeValidator {
    fun validateConnectorsTxGraph(
        commitmentTxPSBT: Psbt,
        connectorsGraph: TxTree,
    ) {
        connectorsGraph.validate()
        if (connectorsGraph.root.inputs.size != 1) {
            throw Errors.NumberOfInputs()
        }
        val rootInput = connectorsGraph.root.global.tx.txIn[0]
        val commitmentTx = commitmentTxPSBT.global.tx
        if (rootInput.outPoint.txid != commitmentTx.txid) {
            throw Errors.WrongSettlementTxId()
        }
        if (commitmentTx.txOut.size <= rootInput.outPoint.index) {
            throw Errors.InvalidSettlementTxOutputs()
        }
    }

    fun validateVtxoTxGraph(
        graph: TxTree,
        roundTxPSBT: Psbt,
        sweepTapTreeRoot: ByteVector32,
    ) {
        val rootInput = graph.root.global.tx.txIn[0]
        val commitmentTxId = roundTxPSBT.global.tx.txid

        if (rootInput.outPoint.txid != commitmentTxId) {
            throw Errors.WrongCommitmentTxId()
        }

        val batchOutputIndex = rootInput.outPoint.index

        if (graph.root.outputs.size <= batchOutputIndex) {
            throw Errors.InvalidRoundTxOutputs()
        }

        val batchOutputAmount =
            roundTxPSBT.global.tx.txOut[batchOutputIndex.toInt()]
                .amount.sat

        val leaves = graph.leaves()
        if (leaves.isEmpty()) {
            throw Errors.NoLeaves()
        }

        graph.validate()

        for (tree in graph) {
            for ((outputIndex, child) in tree.children) {
                val parentOutput = tree.root.global.tx.txOut[outputIndex.toInt()]
                if (parentOutput.publicKeyScript.isEmpty()) {
                    throw UnsupportedOperationException("Parent output $outputIndex not found")
                }

                val script = parentOutput.publicKeyScript.toByteArray()

                if (script.size < 34 || script[0] != 0x51.toByte() || script[1] != 0x20.toByte()) {
                    throw Errors.InvalidTaprootScript()
                }

                val previousScriptKey = script.copyOfRange(2, 34)
                if (previousScriptKey.size != 32) {
                    throw UnsupportedOperationException("parent out $outputIndex has invalid script")
                }

                val cosigners = child.root.inputs[0].getArkFieldsCosigners()
                if (cosigners.isEmpty()) {
                    throw Errors.MissingCosignersPublicKeys()
                }
                val cosignerKeys =
                    cosigners.sortedBy { it.index }.map {
                        it.pubKey
                    }
                val aggregatedKey = Musig2.aggregateKeys(cosignerKeys)
                val (taprootFinalKey, _) = aggregatedKey.outputKey(sweepTapTreeRoot)

                if (!taprootFinalKey.value.toByteArray().contentEquals(previousScriptKey)) {
                    throw Errors.InvalidTaprootScript()
                }
            }
        }
    }

    object Errors {
        class InvalidSettlementTx(
            tx: String,
        ) : UnsupportedOperationException("Invalid settlement transaction: $tx")

        class InvalidSettlementTxOutputs : UnsupportedOperationException("Invalid settlement transaction outputs")

        class EmptyTree : UnsupportedOperationException("Empty tree")

        class NumberOfInputs : UnsupportedOperationException("Invalid number of inputs")

        class WrongSettlementTxId : UnsupportedOperationException("Wrong settlement tx id")

        class InvalidAmount : UnsupportedOperationException("Invalid amount")

        class NoLeaves : UnsupportedOperationException("No leaves")

        class InvalidTaprootScript : UnsupportedOperationException("Invalid taproot script")

        class InvalidRoundTxOutputs : UnsupportedOperationException("Invalid round transaction outputs")

        class WrongCommitmentTxId : UnsupportedOperationException("Wrong commitment txid")

        class MissingCosignersPublicKeys : UnsupportedOperationException("Missing cosigners public keys")
    }
}
