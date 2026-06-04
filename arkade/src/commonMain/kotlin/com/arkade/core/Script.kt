package com.arkade.core

import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OP_CHECKSEQUENCEVERIFY
import fr.acinq.bitcoin.OP_CHECKSIG
import fr.acinq.bitcoin.OP_CHECKSIGVERIFY
import fr.acinq.bitcoin.OP_DROP
import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptTree
import fr.acinq.bitcoin.XonlyPublicKey

/**
 * @param serverPubKey is the Arkade operator's x-only public key
 * @param ownerPubKey is the `VTXO` owner's x-only public key
 * @return a multisig script for collaborative exit
 */
fun multisigScript(
    serverPubKey: XonlyPublicKey,
    ownerPubKey: XonlyPublicKey,
): ByteArray {
    val asm =
        listOf(
            OP_PUSHDATA(serverPubKey),
            OP_CHECKSIGVERIFY,
            OP_PUSHDATA(ownerPubKey),
            OP_CHECKSIG,
        )
    return Script.write(asm)
}

/**
 * @param lockTime is the wait time for the exit after on-chain confirmation
 * @param ownerPubKey is the x-only public key for the `VTXO` owner
 * @return a `CSV` script for unilateral exit
 * @throws IllegalArgumentException if the [lockTime] is not with the range [[0, 65535]]
 */
fun csvSigScript(
    lockTime: Long,
    ownerPubKey: XonlyPublicKey,
): ByteArray {
    require(lockTime in 0..0xFFFFL) { "Invalid lock time" }
    val asm =
        listOf(
            OP_PUSHDATA(Script.encodeNumber(lockTime)),
            OP_CHECKSEQUENCEVERIFY,
            OP_DROP,
            OP_PUSHDATA(ownerPubKey),
            OP_CHECKSIG,
        )
    return Script.write(asm)
}

fun buildScriptTree(leaves: List<ByteArray>): ScriptTree {
    require(leaves.isNotEmpty()) { "Leaves have 0 length" }
    if (leaves.size == 1) {
        return ScriptTree.Leaf(ByteVector(leaves.single()), 0)
    }

    val leaves = leaves.map { ScriptTree.Leaf(ByteVector(it), 0) }
    val branches = mutableListOf<ScriptTree.Branch>()

    for (leafIndex in 0 until leaves.size step 2) {
        if (leafIndex == leaves.size - 1) {
            val lastBranch = branches.removeLastOrNull()
            if (lastBranch != null) {
                branches.add(ScriptTree.Branch(lastBranch, leaves[leafIndex]))
                continue
            } else {
                throw IllegalStateException("This should not happen")
            }
        }

        val branch = ScriptTree.Branch(leaves[leafIndex], leaves[leafIndex + 1])
        branches.add(branch)
    }

    while (branches.isNotEmpty()) {
        if (branches.size == 1) {
            return branches.single()
        }

        val right = branches.removeAt(0)
        val left = branches.removeAt(0)
        val branch = ScriptTree.Branch(left, right)
        branches.add(branch)
    }
    throw IllegalStateException("This should never happen")
}
