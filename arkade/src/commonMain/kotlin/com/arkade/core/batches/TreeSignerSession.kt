package com.arkade.core.batches

import com.arkade.core.getArkFieldsCosigners
import com.arkade.core.wallet.Wallet
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptTree
import fr.acinq.bitcoin.SigHash
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.crypto.musig2.AggregatedNonce
import fr.acinq.bitcoin.crypto.musig2.IndividualNonce
import fr.acinq.bitcoin.crypto.musig2.Musig2
import fr.acinq.bitcoin.crypto.musig2.SecretNonce
import fr.acinq.bitcoin.sat

class TreeSignerSession(
    private val wallet: Wallet,
    private val graph: TxTree,
    private val descriptor: String,
    private val tapScriptTree: ScriptTree,
    private val rootSharedOutputAmount: Long,
) {
    private val tapScriptTreeRoot = tapScriptTree.hash()
    private var myNonces: Map<ByteVector32, Pair<SecretNonce, IndividualNonce>>? = null
    private val treeNonces: MutableMap<ByteVector32, List<IndividualNonce>> = mutableMapOf()
    private val aggregatedNonces: MutableMap<ByteVector32, AggregatedNonce> = mutableMapOf()

    suspend fun generateNonces(): Map<ByteVector32, Pair<SecretNonce, IndividualNonce>> {
        if (myNonces != null) {
            throw UnsupportedOperationException("Nonces already generated")
        }
        val signer = wallet.signer
        val myPubKey = signer.xOnlyPublicKey(descriptor)

        val treeNonces: MutableMap<ByteVector32, Pair<SecretNonce, IndividualNonce>> = mutableMapOf()

        graph.forEach { node ->
            val tx = node.root.global.tx
            val txId = tx.txid

            val cosignersKeys =
                node.root.inputs[0]
                    .getArkFieldsCosigners()
                    .sortedBy { it.index }
                    .map { it.pubKey }

            if (cosignersKeys.all { it.xOnly() != myPubKey }) {
                return@forEach
            }

            val prevOut = getPrevOutput(node, graph)

            val sigHash =
                tx.hashForSigningTaprootScriptPath(
                    0,
                    listOf(prevOut),
                    SigHash.SIGHASH_DEFAULT,
                    tapScriptTreeRoot,
                )

            val nonce =
                signer.generateNonce(
                    txId.value,
                    descriptor,
                    cosignersKeys,
                    sigHash,
                )
            treeNonces[txId.value] = nonce
        }
        return treeNonces
    }

    suspend fun getNonces(): Map<ByteVector32, IndividualNonce> {
        if (myNonces == null) {
            myNonces = generateNonces()
        }
        return myNonces?.mapValues { it.value.second }!!
    }

    private fun getPrevOutput(
        graph: TxTree,
        rootGraph: TxTree,
    ): TxOut {
        val cosignerKeys = graph.getCosignerPubKeys()

        val aggregatedKey = Musig2.aggregateKeys(cosignerKeys)

        val txId = graph.root.global.tx.txid

        val scriptPubKey = Script.pay2tr(aggregatedKey, tapScriptTreeRoot)

        if (txId == rootGraph.root.global.tx.txid) {
            return TxOut(rootSharedOutputAmount.sat(), scriptPubKey)
        }

        val tx = graph.root.global.tx
        val parentInput = tx.txIn[0]
        val parentTxId = parentInput.outPoint.txid
        val parent =
            rootGraph.find(parentTxId)
                ?: throw UnsupportedOperationException("Parent tx not found: $parentTxId")

        val parentOutput = parent.root.global.tx.txOut[parentInput.outPoint.index.toInt()]

        return TxOut(parentOutput.amount, scriptPubKey)
    }

    fun aggregateNonces(
        treeNonces: List<IndividualNonce>,
        txId: TxId,
    ) {
        val txId = txId.value
        val myNonce = myNonces?.get(txId)
        if (myNonces == null || myNonce == null) {
            throw UnsupportedOperationException("Missing private nonce")
        }
        if (!treeNonces.any { nonce -> nonce.data == myNonce.second.data }) {
            throw UnsupportedOperationException("Missing my nonce")
        }

        val aggregatedNonce =
            IndividualNonce.aggregate(treeNonces).right
                ?: throw UnsupportedOperationException("Failed to aggregate nonces")
        this.treeNonces[txId] = treeNonces
        aggregatedNonces[txId] = aggregatedNonce
    }

    fun verifyAggregatedNonces(expected: Map<ByteVector32, IndividualNonce>) {
        if (myNonces == null) {
            throw UnsupportedOperationException("Nonces not generated")
        }
        val isMatching =
            expected.all { entry ->
                val nonce =
                    aggregatedNonces[entry.key]
                        ?: throw UnsupportedOperationException("Aggregated nonce missing")
                nonce.data == entry.value.data
            }
        if (!isMatching) {
            throw UnsupportedOperationException("Aggregated myNonces do not match")
        }
    }

    suspend fun sign(): Map<ByteVector32, ByteVector32> {
        if (myNonces == null) {
            throw UnsupportedOperationException("Nonces not generated")
        }

        val signatures: MutableMap<ByteVector32, ByteVector32> = mutableMapOf()
        graph.forEach { nodeTxTree ->
            val txId = nodeTxTree.root.global.tx.txid.value
            if (myNonces?.containsKey(txId) == false) {
                return@forEach
            }
            val signature = signPartial(nodeTxTree)
            signatures[txId] = signature
        }
        return signatures
    }

    private suspend fun signPartial(nodeTxTree: TxTree): ByteVector32 {
        if (myNonces == null) {
            throw UnsupportedOperationException("Session not properly initialized")
        }

        val tx = nodeTxTree.root.global.tx
        val txId = tx.txid.value

        val privNonce = myNonces!![txId]?.first ?: throw UnsupportedOperationException("Missing private nonce")

        val prevOut = getPrevOutput(nodeTxTree, graph)

        val cosignerPubKeys = nodeTxTree.getCosignerPubKeys()

        val pubNonces = treeNonces[txId] ?: throw UnsupportedOperationException("Missing tree nonces")

        val partialSig =
            wallet.signer.signMusig(
                descriptor,
                tx,
                listOf(prevOut),
                0,
                privNonce,
                cosignerPubKeys,
                pubNonces,
                tapScriptTree,
            )

        return partialSig
    }

    private fun TxTree.getCosignerPubKeys(): List<PublicKey> =
        root.inputs[0]
            .getArkFieldsCosigners()
            .sortedBy { it.index }
            .map { it.pubKey }
}
