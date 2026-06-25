package com.arkade.core.intents

import kotlinx.serialization.Serializable

@Serializable
data class IntentVtxo(
    val intentTxId: String,
    val vtxoTxId: String,
    val vtxoTxOutIndex: Long,
    val linkedAt: Long,
)
