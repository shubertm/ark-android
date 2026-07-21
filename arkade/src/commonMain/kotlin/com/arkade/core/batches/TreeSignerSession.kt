package com.arkade.core.batches

import com.arkade.core.getArkFieldsCosigners
import com.arkade.core.wallet.Wallet
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigHash
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.crypto.musig2.IndividualNonce
import fr.acinq.bitcoin.crypto.musig2.Musig2
import fr.acinq.bitcoin.crypto.musig2.SecretNonce

class TreeSignerSession(
    private val wallet: Wallet,
    private val graph: TxTree,
    private val descriptor: String,
    private val tapscriptMerkleRoot: ByteArray?,
    private val rootSharedOutputAmount: Long,
) {
    private var nonces: Map<ByteVector32, Pair<SecretNonce, IndividualNonce>>? = null

    suspend fun generateNonces(): Map<ByteVector32, Pair<SecretNonce, IndividualNonce>> {
        if (nonces != null) {
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

            val (prevOutAmount, scriptPubKey) = getPrevOutput(node, graph)
            val prevOut =
                TxOut(
                    Satoshi(prevOutAmount),
                    scriptPubKey,
                )
            if (tapscriptMerkleRoot != null) {
                val sigHash =
                    tx.hashForSigningTaprootScriptPath(
                        0,
                        listOf(prevOut),
                        SigHash.SIGHASH_DEFAULT,
                        ByteVector32(tapscriptMerkleRoot),
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
        }
        return treeNonces
    }

    suspend fun getNonces(): Map<ByteVector32, IndividualNonce> {
        if (nonces == null) {
            nonces = generateNonces()
        }
        return nonces?.mapValues { it.value.second }!!
    }

    private fun getPrevOutput(
        graph: TxTree,
        rootGraph: TxTree,
    ): Pair<Long, ByteArray> {
        val cosignerKeys =
            graph.root.inputs[0]
                .getArkFieldsCosigners()
                .sortedBy { it.index }
                .map { it.pubKey }

        val aggregatedKey = Musig2.aggregateKeys(cosignerKeys)

        if (tapscriptMerkleRoot == null) {
            throw UnsupportedOperationException("Script root not set")
        }

        val merkleRoot = ByteVector32(tapscriptMerkleRoot)

        val (taprootFinalKey, _) = aggregatedKey.outputKey(merkleRoot)

        val txId = graph.root.global.tx.txid

        val scriptPubKey =
            with(Script) {
                write(pay2tr(taprootFinalKey, merkleRoot))
            }

        if (txId == rootGraph.root.global.tx.txid) {
            return rootSharedOutputAmount to scriptPubKey
        }

        val tx = graph.root.global.tx
        val parentInput = tx.txIn[0]
        val parentTxId = parentInput.outPoint.txid
        val parent =
            rootGraph.find(parentTxId)
                ?: throw UnsupportedOperationException("Parent tx not found: $parentTxId")

        val parentOutput = parent.root.global.tx.txOut[parentInput.outPoint.index.toInt()]

        return parentOutput.amount.sat to scriptPubKey
    }
}
