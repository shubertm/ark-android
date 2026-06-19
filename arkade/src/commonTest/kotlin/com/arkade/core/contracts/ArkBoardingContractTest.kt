package com.arkade.core.contracts

import com.arkade.core.bitcoin.Hrp
import com.arkade.core.bitcoin.Network
import com.arkade.core.taproot.parseTaprootDescriptor
import com.arkade.storage.db.entities.ContractEntity
import fr.acinq.bitcoin.OP_CHECKSEQUENCEVERIFY
import fr.acinq.bitcoin.OP_CHECKSIG
import fr.acinq.bitcoin.OP_CHECKSIGVERIFY
import fr.acinq.bitcoin.Script
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArkBoardingContractTest {
    val serverPubKeyDescriptor = parseTaprootDescriptor("03a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c")
    val ownerPubKeyDescriptor = parseTaprootDescriptor("0315fbe13a8cf7e4d0c81b0caf4040f37666933d97080abb04f908964bb14588a8")
    val diffOwnerPubKeyDescriptor = parseTaprootDescriptor("2f9c3daffa6d41e380e036433e9eea09b82bbbc5d9c772286655aa1c5b9ab3f0")

    val exitDelay = 144L

    @Test
    fun should_generate_tap_script_tree_correctly() {
        val contract =
            ArkBoardingContract(
                serverPubKeyDescriptor,
                ownerPubKeyDescriptor,
                exitDelay,
            )

        val leaves = contract.getTapLeafScripts()

        assertEquals(2, leaves.size)

        val collaborativeScript = Script.parse(leaves[0])
        assertTrue(collaborativeScript.contains(OP_CHECKSIGVERIFY))
        assertTrue(collaborativeScript.contains(OP_CHECKSIG))

        val unilateralExit = Script.parse(leaves[1])
        assertTrue(unilateralExit.contains(OP_CHECKSEQUENCEVERIFY))
    }

    @Test
    fun should_produce_deterministic_address() {
        val contract =
            ArkBoardingContract(
                serverPubKeyDescriptor,
                ownerPubKeyDescriptor,
                exitDelay,
            )
        val contract1 =
            ArkBoardingContract(
                serverPubKeyDescriptor,
                ownerPubKeyDescriptor,
                exitDelay,
            )

        assertEquals(
            contract.getScriptPubKey(Network.TESTNET),
            contract1.getScriptPubKey(Network.TESTNET),
        )
        assertEquals(
            contract.getOnChainAddress(Network.TESTNET).encode(),
            contract1.getOnChainAddress(Network.TESTNET).encode(),
        )
    }

    @Test
    fun should_produce_different_addresses_for_different_keys() {
        val contract =
            ArkBoardingContract(
                serverPubKeyDescriptor,
                ownerPubKeyDescriptor,
                exitDelay,
            )
        val contract1 =
            ArkBoardingContract(
                serverPubKeyDescriptor,
                diffOwnerPubKeyDescriptor,
                exitDelay,
            )

        assertNotEquals(
            contract.getScriptPubKey(Network.TESTNET),
            contract1.getScriptPubKey(Network.TESTNET),
        )
        assertNotEquals(
            contract.getOnChainAddress(Network.MAINNET).encode(),
            contract1.getOnChainAddress(Network.MAINNET).encode(),
        )
    }

    @Test
    fun parse_round_trip() {
        val contract = ArkBoardingContract(serverPubKeyDescriptor, ownerPubKeyDescriptor, exitDelay)
        val contractEntity = ContractEntity.fromContract(contract, walletId = "test-wallet", network = Network.MAINNET)
        val decoded = ArkBoardingContract.parse(contractEntity.additionalData) as ArkBoardingContract

        assertEquals(contract.type, decoded.type)
        assertEquals(contract.getScriptPubKey(Network.MAINNET), decoded.getScriptPubKey(Network.MAINNET))
        assertEquals(contract.getOnChainAddress(Network.MAINNET), decoded.getOnChainAddress(Network.MAINNET))
        assertEquals(contract.getTapLeafScripts().size, decoded.getTapLeafScripts().size)
        assertEquals(contract.getAdditionalData().size, decoded.getAdditionalData().size)
    }

    @Test
    fun should_throw_on_get_ark_address() {
        val contract = ArkBoardingContract(serverPubKeyDescriptor, ownerPubKeyDescriptor, exitDelay)
        assertFailsWith<UnsupportedOperationException>("") {
            contract.getArkAddress(Network.MAINNET)
        }
    }

    @Test
    fun should_return_bech32m_encoded_on_chain_address() {
        val contract = ArkBoardingContract(serverPubKeyDescriptor, ownerPubKeyDescriptor, exitDelay)
        val testnetAddress = contract.getOnChainAddress(Network.TESTNET)
        val regtestAddress = contract.getOnChainAddress(Network.REGTEST)
        val signetAddress = contract.getOnChainAddress(Network.SIGNET)
        val mainnetAddress = contract.getOnChainAddress(Network.MAINNET)

        assertEquals(Hrp.TESTNETS.prefix, testnetAddress.hrp.prefix)
        assertEquals(Hrp.REGTEST.prefix, regtestAddress.hrp.prefix)
        assertEquals(Hrp.TESTNETS.prefix, signetAddress.hrp.prefix)
        assertEquals(Hrp.MAINNET.prefix, mainnetAddress.hrp.prefix)
    }

    @Test
    fun should_always_create_a_contract_of_type_boarding() {
        val contract = ArkBoardingContract(serverPubKeyDescriptor, ownerPubKeyDescriptor, exitDelay)
        assertEquals(ArkBoardingContract.TYPE, contract.type)
    }
}
