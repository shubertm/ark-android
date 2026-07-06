package com.arkade.core.batches

import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.psbt.Psbt

data class TxTreeNode(
    val tx: Psbt,
    val children: Map<Long, TxId>,
)
