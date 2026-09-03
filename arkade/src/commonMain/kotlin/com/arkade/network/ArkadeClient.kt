package com.arkade.network

import com.arkade.core.ArkServerInfo
import com.arkade.core.batches.BatchEvent
import com.arkade.core.intents.ArkIntent
import com.arkade.core.txs.ArkTransaction
import com.arkade.core.txs.TxEvent
import kotlinx.coroutines.flow.Flow

interface ArkadeClient {
    /**
     * Fetches Ark server information
     * @return [ArkServerInfo]
     */
    suspend fun getInfo(): ArkServerInfo

    /**
     * Registers an intent with the Ark server
     * @param intent is the intent to register
     * @return the intent id
     */
    suspend fun registerIntent(intent: ArkIntent): String

    /**
     * Confirms the registration of an intent with the Ark server
     * @param intentId is the intent id
     */
    suspend fun confirmIntentRegistration(intentId: String)

    /**
     * Deletes an intent with the Ark server
     * @param intent is the intent to delete
     */
    suspend fun deleteIntent(intent: ArkIntent)

    /**
     * Submits a transaction to the Ark server
     * @param signedArkTx is the signed transaction
     * @param checkpointTxs is the list of checkpoint transactions
     * @return a fully signed [ArkTransaction]
     */
    suspend fun submitTransaction(
        signedArkTx: String,
        checkpointTxs: List<String>,
    ): ArkTransaction

    /**
     * Finalize processing a transaction
     * @param arkTxid is the transaction id
     * @param finalCheckpointTxs is the list of finalized checkpoint transactions
     */
    suspend fun finalizeTransaction(
        arkTxid: String,
        finalCheckpointTxs: List<String>,
    )

    /**
     * Submits tree nonces to the Ark server
     * @param batchId is the batch id
     * @param pubkey is the public key
     * @param treeNonces is the map of tree nonces
     */
    suspend fun submitTreeNonces(
        batchId: String,
        pubkey: String,
        treeNonces: Map<String, String>,
    )

    /**
     * Submits tree signatures to the Ark server
     * @param batchId is the batch id
     * @param pubkey is the public key
     * @param treeSignatures is the map of tree signatures
     */
    suspend fun submitTreeSignatures(
        batchId: String,
        pubkey: String,
        treeSignatures: Map<String, String>,
    )

    /**
     * Submits forfeit transactions to the Ark server
     * @param signedForfeitTxs is the list of signed forfeit transactions
     * @param signedCommitmentTx is the signed commitment transaction
     */
    suspend fun submitForfeitTxs(
        signedForfeitTxs: List<String>,
        signedCommitmentTx: String?,
    )

    /**
     * Fetches pending transactions from the Ark server
     * @param intent is the intent
     * @return [List] of [ArkTransaction]
     */
    suspend fun getPendingTxs(intent: ArkIntent): List<ArkTransaction>

    /**
     * Streams batch events from the Ark server
     */
    fun getBatchEventStream(topics: List<String>): Flow<BatchEvent>

    /**
     * Streams transaction events from the Ark server
     */
    fun getTransactionsStream(): Flow<TxEvent>

    suspend fun updateStreamTopics(
        streamId: String,
        addTopics: List<String> = emptyList(),
        removeTopics: List<String> = emptyList(),
    )
}
