package com.arkade.core.batches

import kotlin.time.Duration

/**
 * A single event emitted by the server's batch event stream, describing one step of a batch's
 * lifecycle from creation through finalization and VTXO tree signing or the event-stream lifecycle.
 *
 * Consumers typically process these in order: [BatchStartedEvent] selects the intents included
 * in a batch, [BatchFinalizationEvent] requests forfeit/commitment signing,
 * [TreeSigningStartedEvent] begins cooperative VTXO tree signing (see
 * [com.arkade.core.batches.TreeSignerSession]), [TreeTxEvent] streams the tree's transactions,
 * [TreeNoncesEvent]/[TreeNoncesAggregatedEvent] carry the nonce exchange, [TreeSignatureEvent]
 * carries partial signatures, and [BatchFinalizedEvent] or [BatchFailedEvent] end the batch.
 */
sealed interface BatchEvent {
    /**
     * Emitted when a new batch starts.
     *
     * @property id The id of the batch.
     * @property batchExpiry The expiration time applied to VTXOs produced by this batch.
     * @property intentIdHashes The SHA-256 hashes (hex-encoded) of the ids of the intents
     * included in this batch.
     */
    class BatchStartedEvent(
        val id: String,
        val batchExpiry: Duration,
        val intentIdHashes: List<String>,
    ) : BatchEvent

    /**
     * Emitted when the server requests finalization of the batch.
     *
     * @property id The id of the batch.
     * @property commitmentTx The unsigned commitment transaction, as a base64-encoded PSBT.
     */
    class BatchFinalizationEvent(
        val id: String,
        val commitmentTx: String,
    ) : BatchEvent

    /**
     * Emitted when the batch has been finalized on-chain.
     *
     * @property id The id of the batch.
     * @property commitmentTxId The id of the confirmed commitment transaction.
     */
    class BatchFinalizedEvent(
        val id: String,
        val commitmentTxId: String,
    ) : BatchEvent

    /**
     * Emitted when the batch has failed.
     *
     * @property id The id of the batch.
     * @property reason A human-readable description of the failure.
     */
    class BatchFailedEvent(
        val id: String,
        val reason: String,
    ) : BatchEvent

    /**
     * Emitted when cooperative signing of the VTXO tree starts.
     *
     * @property id The id of the batch.
     * @property coSigners The public keys of the cosigners participating in the tree signing.
     * @property unsignedCommitmentTx The unsigned commitment transaction, as a base64-encoded
     * PSBT, whose shared output funds the VTXO tree.
     */
    class TreeSigningStartedEvent(
        val id: String,
        val coSigners: List<String>,
        val unsignedCommitmentTx: String,
    ) : BatchEvent

    /**
     * Emitted when the server has aggregated the cosigners' public nonces for every VTXO tree
     * transaction.
     *
     * @property id The id of the batch.
     * @property treeNonces Maps each tree transaction's id (hex) to its aggregated public nonce
     * (hex).
     */
    class TreeNoncesAggregatedEvent(
        val id: String,
        val treeNonces: Map<String, String>,
    ) : BatchEvent

    /**
     * Emitted once per transaction of the connector or VTXO tree being built for the batch.
     *
     * @property id The id of the batch.
     * @property batchIndex Identifies which tree this transaction belongs to (`0` for the VTXO
     * tree, `1` for the connector tree).
     * @property tx The transaction, as a base64-encoded PSBT.
     * @property txId The id of [tx].
     * @property children Maps each spent output index of [tx] to the id of the child
     * transaction spending it.
     * @property topic The stream topic(s) this event was delivered on.
     */
    class TreeTxEvent(
        val id: String,
        val batchIndex: Int,
        val tx: String,
        val txId: String,
        val children: Map<Int, String>,
        val topic: List<String>,
    ) : BatchEvent

    /**
     * Emitted when a cosigner's partial signature for a VTXO tree transaction becomes available.
     *
     * @property id The id of the batch.
     * @property batchIndex Identifies which tree this signature belongs to.
     * @property txId The id of the transaction being signed.
     * @property topic The stream topic(s) this event was delivered on.
     */
    class TreeSignatureEvent(
        val id: String,
        val signature: String,
        val batchIndex: Int,
        val txId: String,
        val topic: List<String>,
    ) : BatchEvent

    /**
     * Emitted when cosigners' public nonces for a VTXO tree transaction become available.
     *
     * @property id The id of the batch.
     * @property txId The id of the transaction the nonces are for.
     * @property topic The stream topic(s) this event was delivered on.
     * @property treeNonces The public nonces (hex-encoded) contributed for [txId].
     */
    class TreeNoncesEvent(
        val id: String,
        val txId: String,
        val topic: List<String>,
        val treeNonces: Map<String, String>,
    ) : BatchEvent

    /**
     * Emitted periodically to keep the batch event stream connection alive.
     */
    object HeartbeatEvent : BatchEvent

    /**
     * Emitted once, when the batch event stream connection is (re-)established.
     *
     * @property id The id of the stream.
     */
    class StreamStartedEvent(
        val id: String,
    ) : BatchEvent
}
