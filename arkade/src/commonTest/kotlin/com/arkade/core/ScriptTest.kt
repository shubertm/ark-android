package com.arkade.core

import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.ScriptTree
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScriptTest {
    val serverPubKey = PublicKey.fromHex("03a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c").xOnly()
    val ownerPubKey = PublicKey.fromHex("0315fbe13a8cf7e4d0c81b0caf4040f37666933d97080abb04f908964bb14588a8").xOnly()

    @Test
    fun scripts_should_be_generated_correctly() {
        val csvScript = "029000b2752015fbe13a8cf7e4d0c81b0caf4040f37666933d97080abb04f908964bb14588a8ac"
        val multisigScript =
            "20a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622" +
                "cad2015fbe13a8cf7e4d0c81b0caf4040f37666933d97080abb04f908964bb14588a8ac"

        val csvScriptGenerated = csvSigScript(0x90, ownerPubKey)
        val multisigScriptGenerated = multisigScript(serverPubKey, ownerPubKey)

        assertEquals(39, csvScriptGenerated.size)
        assertEquals(68, multisigScriptGenerated.size)
        assertEquals(csvScript, csvScriptGenerated.toHexString())
        assertEquals(multisigScript, multisigScriptGenerated.toHexString())
    }

    @Test
    fun csv_script_generation_should_fail_on_invalid_lock_time_above_range() {
        assertFailsWith<IllegalArgumentException> {
            csvSigScript(0xFFFFE, ownerPubKey)
        }
    }

    @Test
    fun csv_script_generation_should_fail_on_invalid_lock_time_below_range() {
        assertFailsWith<IllegalArgumentException> {
            csvSigScript(-0x01, ownerPubKey)
        }
    }

    @Test
    fun should_fail_on_empty_leaves() {
        assertFailsWith<IllegalArgumentException> {
            buildScriptTree(emptyList())
        }
    }

    @Test
    fun should_return_leaf_on_single_leaf() {
        val scripts = listOf(multisigScript(serverPubKey, ownerPubKey))
        val scriptTree = buildScriptTree(scripts)
        assertTrue(scriptTree is ScriptTree.Leaf)
    }

    @Test
    fun should_build_script_tree_from_scripts_successfully() {
        val scripts = mutableListOf<ByteArray>()
        for (seed in 0..10) {
            val secret = Random(seed).nextBytes(32).toHexString()
            val ownerPubKey = PrivateKey.fromHex(secret).xOnlyPublicKey()
            scripts.add(multisigScript(serverPubKey, ownerPubKey))
        }
        for (i in 0..10) {
            scripts.add(csvSigScript(i.toLong(), ownerPubKey))
        }
        val scriptTree = buildScriptTree(scripts)

        scripts.forEach {
            val leaf = ScriptTree.Leaf(ByteVector(it), 0)
            val leafFromTree = scriptTree.findScript(leaf.hash())

            assertNotNull(scriptTree.merkleProof(leaf.hash()))
            assertNotNull(leafFromTree)
            assertEquals(leaf, leafFromTree)
        }
    }

    @Test
    fun should_build_script_tree_from_odd_number_of_leaves() {
        val scripts = mutableListOf<ByteArray>()
        for (seed in 0..10) {
            val secret = Random(seed).nextBytes(32).toHexString()
            val ownerPubKey = PrivateKey.fromHex(secret).xOnlyPublicKey()
            scripts.add(multisigScript(serverPubKey, ownerPubKey))
        }
        for (i in 0..9) {
            scripts.add(csvSigScript(i.toLong(), ownerPubKey))
        }
        val scriptTree = buildScriptTree(scripts)
        scripts.forEach {
            val leaf = ScriptTree.Leaf(ByteVector(it), 0)
            val leafFromTree = scriptTree.findScript(leaf.hash())
            assertNotNull(scriptTree.merkleProof(leaf.hash()))
            assertNotNull(leafFromTree)
            assertEquals(leaf, leafFromTree)
        }
    }
}
