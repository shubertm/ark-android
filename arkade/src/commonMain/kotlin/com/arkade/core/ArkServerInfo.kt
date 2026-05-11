package com.arkade.core

import ark.v1.DeprecatedSigner
import ark.v1.ScheduledSession
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Network
import com.arkade.core.fees.FeeInfo
import fr.acinq.bitcoin.XonlyPublicKey
import kotlin.time.Duration

/**
 * All information about the Ark server
 */
data class ArkServerInfo(
    val version: String,
    val signerPubKey: XonlyPublicKey,
    val forfeitPubKey: XonlyPublicKey,
    val forfeitAddress: Address,
    val checkpointTapScript: String,
    val network: Network,
    val sessionDuration: Duration,
    val unilateralExitDelay: Duration,
    val boardingExitDelay: Duration,
    val utxoMinAmount: Long,
    val utxoMaxAmount: Long,
    val vtxoMinAmount: Long,
    val vtxoMaxAmount: Long,
    val dust: Long,
    val fees: FeeInfo?,
    val scheduledSession: ScheduledSession?,
    val deprecatedSigners: List<DeprecatedSigner>,
    val serviceStatus: Map<String, String>,
    val digest: String,
    val maxTxWeight: Long,
    val maxOpReturnOutputs: Long,
)
