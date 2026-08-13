package com.arkade.core.txs

import com.arkade.core.vtxos.Vtxo
import fr.acinq.bitcoin.OutPoint

/**
 * A transaction notification sent from Ark server via transaction events
 * @param txId is the transaction id
 * @param tx is the serialized transaction data
 * @param checkpointTxs is the map of checkpoint transactions
 * @param spentVtxos is the list of spent VTXOs
 * @param spendableVtxos is the list of spendable VTXOss
 * @param sweptVtxos is the list of swept VTXOs as [OutPoint]s
 */
data class Notification(
    val txId: String,
    val tx: String,
    val checkpointTxs: Map<String, Transaction>,
    val spentVtxos: List<Vtxo.Data>,
    val spendableVtxos: List<Vtxo.Data>,
    val sweptVtxos: List<OutPoint>,
)
