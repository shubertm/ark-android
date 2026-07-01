package com.arkade.core.batches

interface BatchEventHandler {
    fun onBatchFinalization()

    fun onBatchFailed()

    fun onTreeSigningStarted()

    fun onTreeNoncesAggregated()

    fun onTreeTx()

    fun onTreeSignature()

    fun onTreeNonces()
}
