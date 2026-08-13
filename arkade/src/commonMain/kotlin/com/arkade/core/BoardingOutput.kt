package com.arkade.core

import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Coin
import com.arkade.core.bitcoin.Network
import com.arkade.core.bitcoin.Utxo
import com.arkade.core.taproot.Parity
import com.arkade.core.taproot.TaprootSpendingInfo
import com.arkade.core.vtxos.ScriptSpendingPath
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptTree
import fr.acinq.bitcoin.XonlyPublicKey
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The `BoardingOutput` class represents a `UTXO` that can be used to create a `VTXO`
 * during the boarding process.
 * @property serverPubKey is the Arkade operator's x-only public key
 * @property ownerPubKey is the Arkade user's x-only public key. This is the owner of the `UTXO`
 * @property spendingInfo is the information required to spend this `UTXO`
 * @property address is the address locking this `UTXO`
 * @property exitDelay (intervals) is the amount of time the owner waits for unilateral exit after transaction on chain confirmation
 */
data class BoardingOutput(
    val serverPubKey: XonlyPublicKey,
    val ownerPubKey: XonlyPublicKey,
    val spendingInfo: TaprootSpendingInfo,
    val address: Address,
    val exitDelay: Long,
) {
    private val tapScripts: List<ByteArray>

    init {
        val forfeitScript = multisigScript(serverPubKey, ownerPubKey)
        val exitScript = csvSigScript(exitDelay, ownerPubKey)
        tapScripts = listOf(forfeitScript, exitScript)
    }

    /**
     * @return the scriptpukey locking this `UTXO`
     */
    fun getScriptPubKey() = address.toScriptPubKey()

    /**
     * @param network is the [Network] where this `UTXO` exists and is valid
     * @param serverPubKey is the Arkade operator's x-only public key
     * @return an [ArkAddress] that locks a `VTXO` created from this `UTXO`
     */
    fun getArkAddress(
        network: Network,
        serverPubKey: XonlyPublicKey,
    ) = ArkAddress.create(
        network,
        serverPubKey.value.toByteArray(),
        spendingInfo.outputKey.value.toByteArray(),
    )

    /**
     * @param script is the script to use for spending this `UTXO`
     * @return the control block for the [script], required to spend using the [script]
     * @throws IllegalArgumentException if the provided script is not found in the [ScriptTree]
     */
    fun getControlBlock(script: ByteArray): ByteArray {
        val spendingLeaf =
            spendingInfo.merkleScriptTree.findScript((ByteVector(script)))
                ?: throw IllegalArgumentException("Invalid leaf script")

        return Script.ControlBlock
            .build(
                spendingInfo.internalKey,
                spendingInfo.merkleScriptTree,
                spendingLeaf,
            ).toByteArray()
    }

    /**
     * @return the script and control block for spending this `UTXO` collaboratively encapsulated in [com.arkade.core.vtxos.ScriptSpendingPath]
     */
    fun getForfeitSpendingInfo(): ScriptSpendingPath {
        val script = tapScripts[0].copyOf()
        val controlBlock = getControlBlock(script)
        return ScriptSpendingPath(script, controlBlock.copyOf())
    }

    /**
     * @return the script and control block for spending this `UTXO` unilaterally encapsulated in [ScriptSpendingPath]
     */
    fun getExitSpendingInfo(): ScriptSpendingPath {
        val script = tapScripts[1].copyOf()
        val controlBlock = getControlBlock(script)
        return ScriptSpendingPath(script, controlBlock.copyOf())
    }

    /**
     * @return the list of tap scripts that can be used to spend this `UTXO`
     */
    fun getTapScripts(): List<ByteArray> = tapScripts

    /**
     * @param now is the current system time
     * @param blockConfirmTime is the time at which this `UTXO` was confirmed on-chain
     * @return whether this `UTXO`'s unilateral [exitDelay] has expired or not
     */
    fun canBeClaimedUnilaterally(
        now: Duration,
        blockConfirmTime: Duration,
    ): Boolean {
        val exitDelaySeconds = exitDelay.multiplyExact(512)
        val exitTime = blockConfirmTime + exitDelaySeconds.toDuration(DurationUnit.SECONDS)
        return now >= exitTime
    }

    companion object {
        /**
         * Creates a `BoardingOutput`
         * @param serverPubKey is the Arkade operator's x-only public key
         * @param ownerPubKey is the Arkade user's x-only public key. This is the owner of the `UTXO`
         * @param exitDelay (intervals) is the amount of time the owner waits for unilateral exit
         * after transaction on chain confirmation
         *
         * `intervals = (seconds / 512)`
         *
         * @param network is the [Network] where this `UTXO` exists and is valid
         * @return a `BoardingInput` that can be used to create a `VTXO`
         */
        fun create(
            serverPubKey: XonlyPublicKey,
            ownerPubKey: XonlyPublicKey,
            exitDelay: Long,
            network: Network,
        ): BoardingOutput {
            val multisigScript = multisigScript(serverPubKey, ownerPubKey)
            val csvScript = csvSigScript(exitDelay, ownerPubKey)

            val merkleTree =
                ScriptTree.Branch(
                    ScriptTree.Leaf(ByteVector(multisigScript), 0),
                    ScriptTree.Leaf(ByteVector(csvScript), 0),
                )
            val merkleRoot = merkleTree.hash()

            val internalKey = UNSPENDABLE_PUBKEY.toXOnlyPubKey()
            val (outputKey, isOdd) = internalKey.outputKey(merkleTree)
            val spendingInfo =
                TaprootSpendingInfo(
                    internalKey,
                    outputKey,
                    Parity.fromBooleanIsOdd(isOdd),
                    merkleRoot,
                    merkleTree,
                )

            val scriptPubKey =
                with(Script) {
                    write(pay2tr(internalKey, merkleRoot))
                }
            val address = Address.fromScriptPubKey(scriptPubKey, network)

            return BoardingOutput(
                serverPubKey,
                ownerPubKey,
                spendingInfo,
                address,
                exitDelay,
            )
        }
    }
}

/**
 * The `BoardingOutpoints` class keeps on-chain references of all boarding outputs (UTXOs) in these four
 * states
 *
 * [spendable] references UTXOs that can be spent in collaboration with the server
 *
 * [expired] references UTXOs that can only be spent unilaterally by the owner
 *
 * [pending] references UTXOs awaiting on-chain confirmation
 *
 * [spent] references UTXOs that have been spent already
 */
data class BoardingOutpoints(
    val spendable: List<Triple<OutPoint, Coin, BoardingOutput>>,
    val expired: List<Triple<OutPoint, Coin, BoardingOutput>>,
    val pending: List<Triple<OutPoint, Coin, BoardingOutput>>,
    val spent: List<Pair<OutPoint, Coin>>,
) {
    /**
     * @return the amount of money that the owner can spend on-chain in collaboration with the server
     */
    fun spendableBalance() = spendable.sumOf { it.second.amount }

    /**
     * @return the amount of money that the owner can spend on-chain with unilateral exit
     */
    fun expiredBalance() = expired.sumOf { it.second.amount }

    /**
     * @return the amount of money not yet confirmed on-chain
     */
    fun pendingBalance() = pending.sumOf { it.second.amount }

    companion object {
        /**
         * Creates [BoardingOutpoints] from [BoardingOutput]s and a list of on-chain [Utxo]s
         * @param boardingOutputs is the list of all [BoardingOutput]s available
         * @param getOnChainUtxos returns a list of on-chain [Utxo]s that belong to the [Address]
         * provided
         * @return [BoardingOutpoints]
         */
        fun fromBoardingOutputs(
            boardingOutputs: List<BoardingOutput>,
            getOnChainUtxos: (Address) -> List<Utxo>,
        ): BoardingOutpoints {
            val spendable = mutableListOf<Triple<OutPoint, Coin, BoardingOutput>>()
            val expired = mutableListOf<Triple<OutPoint, Coin, BoardingOutput>>()
            val pending = mutableListOf<Triple<OutPoint, Coin, BoardingOutput>>()
            val spent = mutableListOf<Pair<OutPoint, Coin>>()
            for (boardingOutput in boardingOutputs) {
                val boardingAddress = boardingOutput.address
                val onChainUtxos = getOnChainUtxos(boardingAddress)

                for (utxo in onChainUtxos) {
                    when {
                        utxo.blockConfirmationTime > 0 && !utxo.isSpent -> {
                            val now =
                                Clock.System
                                    .now()
                                    .epochSeconds.seconds
                            val ownerCanExit =
                                boardingOutput.canBeClaimedUnilaterally(
                                    now,
                                    utxo.blockConfirmationTime.toDuration(DurationUnit.SECONDS),
                                )

                            if (ownerCanExit) {
                                expired.add(
                                    Triple(utxo.outpoint, Coin.fromSatoshi(utxo.amount), boardingOutput),
                                )
                            } else {
                                spendable.add(
                                    Triple(utxo.outpoint, Coin.fromSatoshi(utxo.amount), boardingOutput),
                                )
                            }
                        }
                        utxo.isSpent -> {
                            spent.add(Pair(utxo.outpoint, Coin.fromSatoshi(utxo.amount)))
                        }
                        else -> {
                            pending.add(
                                Triple(utxo.outpoint, Coin.fromSatoshi(utxo.amount), boardingOutput),
                            )
                        }
                    }
                }
            }
            return BoardingOutpoints(spendable, expired, pending, spent)
        }
    }
}
