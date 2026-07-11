package com.arkade.core

import com.arkade.core.bitcoin.Address
import com.arkade.core.coins.ArkCoin
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigHash
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.getOrElse

class ArkTransactionBuilder {
    companion object {
        fun constructForfeitTx(
            coin: ArkCoin,
            connector: TxOut?,
            connectorTxId: TxId,
            forfeitDestination: Address,
        ): Psbt {
            val p2A = Script.parse("51024e73")

            val sighash =
                if (connector == null) {
                    SigHash.SIGHASH_ANYONECANPAY or SigHash.SIGHASH_ALL
                } else {
                    SigHash.SIGHASH_DEFAULT
                }

            val vtxoInput =
                TxIn(
                    outPoint = coin.outpoint,
                    sequence = coin.sequence ?: 0,
                )
            val connectorInput =
                connector?.publicKeyScript?.let {
                    TxIn(
                        outPoint = OutPoint(connectorTxId, 0),
                        signatureScript = it,
                        sequence = 0,
                    )
                }
            val inputs: MutableList<TxIn> = mutableListOf(vtxoInput)
            if (connectorInput != null) {
                inputs.add(connectorInput)
            }

            val totalInputAmount = coin.txOut.amount.sat + (connector?.amount?.sat ?: 0)

            val forfeitOutput =
                TxOut(
                    Satoshi(totalInputAmount),
                    Script.parse(forfeitDestination.toScriptPubKey().toHexString()),
                )

            val outputs: List<TxOut> =
                listOf(
                    TxOut(Satoshi(0), p2A),
                    forfeitOutput,
                )

            val tx =
                Transaction(
                    version = 3,
                    txIn = inputs,
                    txOut = outputs,
                    lockTime = coin.lockTime ?: 0,
                )

            val forfeitTx =
                with(Psbt(tx)) {
                    updateWitnessInput(
                        vtxoInput.outPoint,
                        forfeitOutput,
                        sighashType = sighash,
                        taprootInternalKey = UNSPENDABLE_PUBKEY.toXOnlyPubKey(),
                    ).getOrElse { throw IllegalStateException("Failed to update witness input") }
                }

            return forfeitTx
        }
    }
}
