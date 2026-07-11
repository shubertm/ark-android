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

/**
 * Builds Ark protocol-specific transactions.
 *
 * Currently only exposes construction of "forfeit" transactions, which spend a VTXO
 * (and, when applicable, a connector output) to a forfeit destination controlled by the
 * Ark server, as required when a client fails to unilaterally exit before a batch is settled.
 */
class ArkTransactionBuilder {
    companion object {
        /**
         * Constructs a forfeit transaction [Psbt] for [coin].
         *
         * The transaction spends [coin]'s outpoint and, when [connector] is provided, a second
         * input from the connector output at index `0` of [connectorTxId]. Its outputs are a
         * zero-value anchor (`p2A`) output and a forfeit output paying the combined input amount
         * to [forfeitDestination]. The sighash type is `ANYONECANPAY|ALL` when no connector is
         * used, or `DEFAULT` otherwise. The main input's witness data is pre-populated using
         * [coin]'s outpoint/output and the unspendable internal key.
         *
         * @param coin The VTXO being forfeited; provides the main input and its expiry-derived locktime.
         * @param connector The connector output to spend as a second input, or `null` if none is used.
         * @param connectorTxId The transaction id containing [connector]'s output (at index `0`).
         * @param forfeitDestination The address that will receive the forfeited funds.
         * @return The constructed [Psbt] with the main input's witness data updated.
         * @throws IllegalStateException if updating the main input's witness data fails.
         */
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
                        coin.outpoint,
                        coin.txOut,
                        sighashType = sighash,
                        taprootInternalKey = UNSPENDABLE_PUBKEY.toXOnlyPubKey(),
                    ).getOrElse { throw IllegalStateException("Failed to update witness input") }
                }

            return forfeitTx
        }
    }
}
