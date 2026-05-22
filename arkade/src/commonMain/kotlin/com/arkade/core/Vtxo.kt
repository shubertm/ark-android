package com.arkade.core

import com.arkade.core.assets.Asset
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Network
import com.arkade.core.bitcoin.Utxo
import com.arkade.core.taproot.Parity
import com.arkade.core.taproot.TaprootSpendingInfo
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptTree
import fr.acinq.bitcoin.XonlyPublicKey
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The `Vtxo` class represents a `VTXO (Virtual Transaction Output)`, an Ark abstraction of the
 * Bitcoin `UTXO (Unspent Transaction Output)`
 *
 * [serverPubKey] is the Arkade operator's x-only public key
 *
 * [ownerPubKey] is the Arkade user's x-only public key. This is the owner of the `VTXO`
 *
 * [spendingInfo] is the information required to spend this `VTXO`
 *
 * [tapScripts] is the list of scripts that can be used to spend this `VTXO`
 *
 * [address] is the address locking this `VTXO`
 *
 * [exitDelay] (intervals) is the amount of time the owner waits for unilateral exit after transaction on chain confirmation
 *
 * `intervals = (seconds / 512)`
 *
 * [exitDelaySeconds] is the [exitDelay] in seconds
 *
 * [network] is the [com.arkade.core.bitcoin.Network] where this `VTXO` exists and is valid
 */
data class Vtxo(
    val serverPubKey: XonlyPublicKey,
    val ownerPubKey: XonlyPublicKey,
    val spendingInfo: TaprootSpendingInfo,
    val tapScripts: List<ByteArray>,
    val address: Address,
    val exitDelay: Long,
    val exitDelaySeconds: Long,
    val network: Network,
) {
    init {
        require(tapScripts.size == 2) { "Expects exactly 2 tap scripts: forfeit and exit" }
    }

    /**
     * @return the scriptpukey locking this `VTXO`
     */
    fun getScriptPubKey(): ByteArray = address.toScriptPubKey()

    /**
     * @return an [ArkAddress] that locks this `VTXO`
     */
    fun getArkAddress(): ArkAddress =
        ArkAddress.create(
            network,
            serverPubKey.value.toByteArray(),
            spendingInfo.outputKey.value.toByteArray(),
        )

    /**
     * @param script is the script to use for spending this `VTXO`
     * @return the control block for the [script], required to spend using the [script]
     * @throws IllegalArgumentException if the provided script is not found in the [ScriptTree]
     */
    fun getControlBlock(script: ByteArray): ByteArray {
        val spendingLeaf =
            spendingInfo.merkleScriptTree.findScript(ByteVector(script))
                ?: throw IllegalArgumentException("Invalid leaf script")

        return Script.ControlBlock
            .build(
                spendingInfo.internalKey,
                spendingInfo.merkleScriptTree,
                spendingLeaf,
            ).toByteArray()
    }

    /**
     * @return the script and control block for spending this `UTXO` collaboratively encapsulated in [ScriptSpendingPath]
     */
    fun getForfeitSpendingPath(): ScriptSpendingPath {
        val forfeitScript = tapScripts[0].copyOf()
        val controlBlock = getControlBlock(forfeitScript)
        return ScriptSpendingPath(forfeitScript, controlBlock.copyOf())
    }

    /**
     * @return the script and control block for spending this `UTXO` unilaterally encapsulated in [ScriptSpendingPath]
     */
    fun getExitSpendingPath(): ScriptSpendingPath {
        val exitScript = tapScripts[1].copyOf()
        val controlBlock = getControlBlock(exitScript)
        return ScriptSpendingPath(exitScript, controlBlock.copyOf())
    }

    /**
     * @param now is the current system time
     * @param blockConfirmTime is the time at which this `VTXO` was confirmed on-chain
     * @return whether this `VTXO`'s unilateral [exitDelay] has expired or not
     */
    fun canBeClaimedUnilaterally(
        now: Duration,
        blockConfirmTime: Duration,
    ): Boolean {
        val exitTime = blockConfirmTime + exitDelaySeconds.toDuration(DurationUnit.SECONDS)
        return now >= exitTime
    }

    /**
     * This is an encapsulation of all `VTXO` data
     * @param outpoint is the outpoint of the `VTXO`
     * @param amount is the amount of the `VTXO`
     * @param script is the script of the `VTXO`
     * @param createdAt is the time at which the `VTXO` was created
     * @param expiresAt is the time at which the `VTXO` expires
     * @param isPreConfirmed is whether the `VTXO` was pre-confirmed
     * @param isSwept is whether the `VTXO` was swept
     * @param isUnrolled is whether the `VTXO` was unrolled
     * @param isSpent is whether the `VTXO` was spent
     * @param spentBy is the transaction id of the transaction that spent this `VTXO`
     * @param settledBy is the transaction id of the transaction that settled this `VTXO`
     * @param arkTxId is the transaction id of the transaction that created this `VTXO`
     * @param commitmentTxIds is the list of transaction ids of the transactions that created this `VTXO`
     * @param assets is the list of assets of the `VTXO`
     */
    data class Data(
        val outpoint: OutPoint,
        @Serializable(Json.BigDecimalSerializer::class)
        val amount: BigDecimal,
        val script: String,
        val createdAt: Long,
        val expiresAt: Long,
        val isPreConfirmed: Boolean = false,
        val isSwept: Boolean = false,
        val isUnrolled: Boolean = false,
        val isSpent: Boolean = false,
        val spentBy: String? = null,
        val settledBy: String? = null,
        val arkTxId: String? = null,
        val commitmentTxIds: List<String> = emptyList(),
        val assets: List<Asset> = emptyList(),
    ) {
        init {
            OutPoint.validate(outpoint)
            require(amount > BigDecimal.ZERO) { "Amount must be positive" }
            if (isSpent && spentBy == null) {
                throw IllegalArgumentException("Spent VTXO must have a spending txId")
            }
        }

        companion object {
            fun normalized(
                outpoint: OutPoint,
                amount: BigDecimal,
                script: String,
                createdAt: Long,
                expiresAt: Long,
                isPreConfirmed: Boolean = false,
                isSwept: Boolean = false,
                isUnrolled: Boolean = false,
                isSpent: Boolean = false,
                spentBy: String? = null,
                settledBy: String? = null,
                arkTxId: String? = null,
                commitmentTxIds: List<String> = emptyList(),
                assets: List<Asset> = emptyList(),
            ): Data {
                val script = script.lowercase()
                val spentBy = spentBy?.lowercase()
                val settledBy = settledBy?.lowercase()
                val arkTxId = arkTxId?.lowercase()
                val commitmentTxIds = commitmentTxIds.map { it.lowercase() }
                val assets = assets.map { Asset(it.id.lowercase(), it.amount) }
                return Data(
                    outpoint = outpoint,
                    amount = amount,
                    script = script,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    isPreConfirmed = isPreConfirmed,
                    isSwept = isSwept,
                    isUnrolled = isUnrolled,
                    isSpent = isSpent,
                    spentBy = spentBy,
                    settledBy = settledBy,
                    arkTxId = arkTxId,
                    commitmentTxIds = commitmentTxIds,
                    assets = assets,
                )
            }
        }
    }

    companion object {
        /**
         * Creates a `VTXO`
         * @param serverPubKey is the Arkade operator's x-only public key
         * @param ownerPubKey is the Arkade user's x-only public key. This is the owner of the `VTXO`
         * @param exitDelay (intervals) is the amount of time the owner waits for unilateral exit
         * after transaction on chain confirmation
         *
         * `intervals = (seconds / 512)`
         *
         * @param network is the [Network] where this `VTXO` exists and is valid
         */
        fun build(
            serverPubKey: XonlyPublicKey,
            ownerPubKey: XonlyPublicKey,
            exitDelay: Long,
            network: Network,
        ): Vtxo {
            val forfeitScript = multisigScript(serverPubKey, ownerPubKey)
            val exitScript = csvSigScript(exitDelay, ownerPubKey)
            val tapScripts = listOf(forfeitScript, exitScript)

            return fromScripts(
                serverPubKey,
                ownerPubKey,
                tapScripts,
                exitDelay,
                network,
            )
        }

        /**
         * Creates a `VTXO` from custom scripts
         * @param serverPubKey is the Arkade operator's x-only public key
         * @param ownerPubKey is the Arkade user's x-only public key. This is the owner of the `VTXO`
         * @param tapScripts is the list of scripts that can be used to spend this `VTXO`
         * @param exitDelay (intervals) is the amount of time the owner waits for unilateral exit
         * after transaction on chain confirmation
         *
         * `intervals = (seconds / 512)`
         *
         * @param network is the [Network] where this `VTXO` exists and is valid
         */
        fun fromScripts(
            serverPubKey: XonlyPublicKey,
            ownerPubKey: XonlyPublicKey,
            tapScripts: List<ByteArray>,
            exitDelay: Long,
            network: Network,
        ): Vtxo {
            require(exitDelay in 0..0xFFFFL) { "Exit delay time is out of range" }
            require(tapScripts.size == 2) { "Expects exactly 2 tap scripts: forfeit and exit" }
            val unSpendablePubKey = UNSPENDABLE_PUBKEY.toXOnlyPubKey()

            val (forfeitScript, exitScript) = tapScripts
            val forfeitLeaf = ScriptTree.Leaf(ByteVector(forfeitScript), 0)
            val exitLeaf = ScriptTree.Leaf(ByteVector(exitScript), 0)
            val scriptTree = ScriptTree.Branch(forfeitLeaf, exitLeaf)
            val merkleRoot = scriptTree.hash()

            val (outputKey, isOdd) = unSpendablePubKey.outputKey(merkleRoot)

            val spendingInfo =
                TaprootSpendingInfo(
                    unSpendablePubKey,
                    outputKey,
                    Parity.fromBooleanIsOdd(isOdd),
                    merkleRoot,
                    scriptTree,
                )

            val scriptPubKey =
                with(Script) {
                    write(pay2tr(unSpendablePubKey, merkleRoot))
                }
            val address = Address.fromScriptPubKey(scriptPubKey, network)

            val exitDelaySeconds = exitDelay.multiplyExact(512)

            return Vtxo(
                serverPubKey,
                ownerPubKey,
                spendingInfo,
                tapScripts.map { it.copyOf() },
                address,
                exitDelay,
                exitDelaySeconds,
                network,
            )
        }
    }
}

/**
 * This is an encapsulation of a script to use for spending a `VTXO` and it's control block
 */
data class ScriptSpendingPath(
    val script: ByteArray,
    val control: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScriptSpendingPath

        if (!script.contentEquals(other.script)) return false
        if (!control.contentEquals(other.control)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = script.contentHashCode()
        result = 31 * result + control.contentHashCode()
        return result
    }
}

data class VirtualTxOutPoint(
    val outpoint: OutPoint,
    val amount: BigDecimal,
)

data class VirtualTxOutPoints(
    val spendable: List<Pair<VirtualTxOutPoint, Vtxo>>,
    val expired: List<Pair<VirtualTxOutPoint, Vtxo>>,
) {
    fun spendableBalance() = spendable.sumOf { it.first.amount }

    fun expiredBalance() = expired.sumOf { it.first.amount }

    companion object {
        fun get(
            spendableVtxos: HashMap<Vtxo, List<VirtualTxOutPoint>>,
            getOnChainVtxos: (Address) -> List<Utxo>,
        ): VirtualTxOutPoints {
            val spendable = mutableListOf<Pair<VirtualTxOutPoint, Vtxo>>()
            val expired = mutableListOf<Pair<VirtualTxOutPoint, Vtxo>>()

            for ((vtxo, vtxoOutPoints) in spendableVtxos) {
                val onChainVtxos = getOnChainVtxos(vtxo.address)
                for (vtxoOutPoint in vtxoOutPoints) {
                    val now =
                        Clock.System
                            .now()
                            .epochSeconds.seconds
                    val onChainVtxo = onChainVtxos.find { it.outpoint == vtxoOutPoint.outpoint }
                    if (onChainVtxo == null) {
                        continue
                    }
                    if (onChainVtxo.isSpent) {
                        continue
                    }
                    if (
                        onChainVtxo.blockConfirmationTime > 0L &&
                        vtxo.canBeClaimedUnilaterally(
                            now,
                            onChainVtxo.blockConfirmationTime.seconds,
                        )
                    ) {
                        expired.add(vtxoOutPoint to vtxo)
                        continue
                    }
                    spendable.add(vtxoOutPoint to vtxo)
                }
            }

            return VirtualTxOutPoints(spendable, expired)
        }
    }
}
