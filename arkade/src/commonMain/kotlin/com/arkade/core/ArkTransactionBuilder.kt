package com.arkade.core

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

class ArkTransactionBuilder {
    suspend fun constructForfeitTx(
        arkServerInfo: ArkServerInfo,
        coin: ArkCoin,
        connector: TxOut?,
        connectorTxId: TxId,
        forfeitDestination: String,
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
                Script.parse(forfeitDestination),
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

        val forfeitTx = Psbt(tx)

        // Sign the PSBT

        return forfeitTx
    }
}
