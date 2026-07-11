package com.arkade.core.batches

import com.arkade.core.getArkFieldsCosigners
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.crypto.musig2.Musig2
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Validates the transaction trees involved in a batch, ensuring they are structurally sound
 * and correctly linked to the transactions the server claims they descend from.
 */
object TreeValidator {
    /**
     * Validates that [connectorsGraph] is a well-formed connector tree rooted at
     * [commitmentTxPSBT].
     *
     * Delegates structural validation to [TxTree.validate], then checks that the graph's root
     * has exactly one input, that this input's outpoint references [commitmentTxPSBT]'s txid,
     * and that the referenced output index exists on the commitment transaction.
     *
     * @param commitmentTxPSBT The commitment (settlement) transaction the connectors descend from.
     * @param connectorsGraph The connectors tree to validate.
     * @throws Errors.NumberOfInputs if the root does not have exactly one input.
     * @throws Errors.WrongSettlementTxId if the root input does not reference [commitmentTxPSBT].
     * @throws Errors.InvalidSettlementTxOutputs if the referenced output index is out of bounds.
     */
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

    /**
     * Validates that [graph] is a well-formed VTXO tree rooted at [roundTxPSBT]'s batch output,
     * and that every non-leaf node's Taproot output key matches the aggregated cosigner keys
     * declared on its child.
     *
     * The root input must reference [roundTxPSBT]'s txid, and the referenced batch output index
     * must exist. The tree must have at least one leaf and must satisfy [TxTree.validate]. For
     * every parent/child relationship in [graph], the parent output at the corresponding index
     * must be a Taproot output (`OP_1 <32-byte-key>`); the child's first input's cosigner public
     * keys (see [com.arkade.core.getArkFieldsCosigners]) are aggregated with [Musig2.aggregateKeys]
     * and combined with [sweepTapTreeRoot] to derive the expected Taproot output key, which must
     * match the key embedded in the parent's output script.
     *
     * @param graph The VTXO tree to validate.
     * @param roundTxPSBT The round (commitment) transaction the tree's batch output belongs to.
     * @param sweepTapTreeRoot The Merkle root of the sweep tap script tree, used to derive the
     * expected Taproot output key for each node.
     * @throws Errors.WrongCommitmentTxId if the root input does not reference [roundTxPSBT].
     * @throws Errors.InvalidRoundTxOutputs if the referenced batch output index is out of bounds.
     * @throws Errors.NoLeaves if [graph] has no leaves.
     * @throws Errors.InvalidTaprootScript if a parent output is not a valid Taproot output, or
     * its key does not match the derived key.
     * @throws Errors.MissingCosignersPublicKeys if a child has no cosigner public keys.
     */
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

    /**
     * Exceptions thrown by [TreeValidator] when a transaction tree fails validation.
     */
    object Errors {
        /** Thrown when the settlement transaction referenced by a tree is malformed. */
        class InvalidSettlementTx(
            tx: String,
        ) : UnsupportedOperationException("Invalid settlement transaction: $tx")

        /** Thrown when the settlement transaction does not have the expected output. */
        class InvalidSettlementTxOutputs : UnsupportedOperationException("Invalid settlement transaction outputs")

        /** Thrown when a tree is expected to have nodes but has none. */
        class EmptyTree : UnsupportedOperationException("Empty tree")

        /** Thrown when a tree's root does not have exactly one input. */
        class NumberOfInputs : UnsupportedOperationException("Invalid number of inputs")

        /** Thrown when a tree's root input does not reference the expected settlement transaction. */
        class WrongSettlementTxId : UnsupportedOperationException("Wrong settlement tx id")

        /** Thrown when a transaction amount does not match the expected value. */
        class InvalidAmount : UnsupportedOperationException("Invalid amount")

        /** Thrown when a tree is expected to have at least one leaf but has none. */
        class NoLeaves : UnsupportedOperationException("No leaves")

        /** Thrown when a node's output script is not a valid Taproot script, or its key is wrong. */
        class InvalidTaprootScript : UnsupportedOperationException("Invalid taproot script")

        /** Thrown when the round transaction does not have the expected batch output. */
        class InvalidRoundTxOutputs : UnsupportedOperationException("Invalid round transaction outputs")

        /** Thrown when a tree's root input does not reference the expected commitment transaction. */
        class WrongCommitmentTxId : UnsupportedOperationException("Wrong commitment txid")

        /** Thrown when a node has no cosigner public keys attached. */
        class MissingCosignersPublicKeys : UnsupportedOperationException("Missing cosigners public keys")
    }
}
