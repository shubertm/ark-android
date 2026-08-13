package com.arkade.core.batches

import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.psbt.Psbt

/**
 * A single node of a transaction tree, as received from the server before being assembled
 * into a [TxTree].
 *
 * @property tx The node's transaction, wrapped as a [Psbt].
 * @property children Maps each output index of [tx] that is spent within the tree to the
 * [TxId] of the child transaction spending it.
 */
data class TxTreeNode(
    val tx: Psbt,
    val children: Map<Long, TxId>,
)
