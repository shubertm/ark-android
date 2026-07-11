package com.arkade.core.batches

interface BatchEventHandler {
    suspend fun onBatchFinalization(
        event: BatchEvent.BatchFinalizationEvent,
        connectors: List<TxTreeNode>,
    )

    suspend fun onBatchFailed()

    suspend fun onTreeSigningStarted()

    suspend fun onTreeNoncesAggregated()

    suspend fun onTreeTx()

    suspend fun onTreeSignature()

    suspend fun onTreeNonces()
}
