package com.arkade.core.wallet

import androidx.room.RoomDatabase
import com.arkade.core.ArkServerInfo
import com.arkade.core.Vtxo
import com.arkade.core.assets.Asset
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Hrp
import com.arkade.core.bitcoin.Network
import com.arkade.core.bitcoin.WitnessVersion
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ArkContractParserImpl
import com.arkade.core.contracts.ContractState
import com.arkade.core.intents.ArkIntent
import com.arkade.core.intents.IntentState
import com.arkade.core.toXOnlyPubKey
import com.arkade.core.wallet.Wallet.Companion.masterKeyFromSecret
import com.arkade.di.ArkadeDI
import com.arkade.readJsonFile
import com.arkade.repositories.WalletRepo
import com.arkade.storage.db.Database
import com.arkade.utils.Log
import com.arkade.utils.success
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.TxId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.koin.core.parameter.parametersOf
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

expect abstract class WalletTest() : com.arkade.Test {
    val dbBuilder: RoomDatabase.Builder<Database>

    @Test
    abstract fun should_create_wallet_successfully()

    @Test
    abstract fun should_load_more_wallets_successfully()

    @Test
    abstract fun should_store_and_retrieve_valid_vtxo_data_successfully()

    @Test
    abstract fun should_store_and_retrieve_valid_ark_contracts_successfully()

    @Test
    abstract fun should_store_and_retrieve_valid_ark_intents_successfully()
}

fun getArkServerInfo(): ArkServerInfo =
    ArkServerInfo(
        version = "",
        signerPubKey = "fa73c6e4876ffb2dfc961d763cca9abc73d4b88efcb8f5e7ff92dc55e9aa553d".toXOnlyPubKey(),
        forfeitPubKey = "dfcaec558c7e78cf3e38b898ba8a43cfb5727266bae32c5c5b3aeb32c558aa0b".toXOnlyPubKey(),
        forfeitAddress =
            Address(
                hrp = Hrp.TESTNETS,
                witnessVersion = WitnessVersion.SEGWIT,
                witnessProgram = "15048e41633084bfcae91d03b3c2bb7f6ac78440".hexToByteArray(),
            ),
        checkpointTapScript = "03a80040b27520dfcaec558c7e78cf3e38b898ba8a43cfb5727266bae32c5c5b3aeb32c558aa0bac",
        network = Network.SIGNET,
        sessionDuration = 1.minutes,
        unilateralExitDelay = 2.days,
        boardingExitDelay = 180.days,
        utxoMinAmount = 330,
        utxoMaxAmount = -1,
        vtxoMinAmount = 1,
        vtxoMaxAmount = -1,
        dust = 330,
        fees = null,
        scheduledSession = null,
        deprecatedSigners = emptyList(),
        serviceStatus = emptyMap(),
        digest = "50da3e81cba4844be3559638cf7104a64e30c616bd5862e86b3903222ece0994",
        maxTxWeight = 40000,
        maxOpReturnOutputs = 3,
    )

class SingleKeyWalletTest : WalletTest() {
    private val serverInfo = getArkServerInfo()

    val testVtxoData = Json.parseToJsonElement(readJsonFile("fixtures/vtxo-data.json"))
    val validTestVtxosJsonArray = testVtxoData.jsonObject["valid"]?.jsonObject["vtxos"]?.jsonArray
    val invalidTestVtxosJsonArray = testVtxoData.jsonObject["invalid"]?.jsonObject["vtxos"]?.jsonArray

    val testContractsData = Json.parseToJsonElement(readJsonFile("fixtures/contracts-data.json"))
    val validContractsJsonArray = testContractsData.jsonObject["valid"]?.jsonObject["contracts"]?.jsonArray
    val invalidContractsJsonArray = testContractsData.jsonObject["invalid"]?.jsonObject["contracts"]?.jsonArray

    val testIntentsData = Json.parseToJsonElement(readJsonFile("fixtures/intents-data.json"))
    val validIntentsJsonArray = testIntentsData.jsonObject["valid"]?.jsonObject["intents"]?.jsonArray
    val invalidIntentsJsonArray = testIntentsData.jsonObject["invalid"]?.jsonObject["intents"]?.jsonArray

    @Test
    override fun should_create_wallet_successfully() {
        runTest {
            val nsec = "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k"
            val wallet =
                Wallet.create(
                    nsec,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )
            assertEquals(nsec, wallet.secret)
            assertEquals(Wallet.Type.SINGLE_KEY, wallet.type)

            wallet.save()

            val loadedWallet = assertNotNull(Wallet.loadById(wallet.id, dbBuilder))

            assertEquals(wallet.id, loadedWallet.id)
            assertEquals(wallet.secret, loadedWallet.secret)
            assertEquals(wallet.destination, loadedWallet.destination)
            assertEquals(wallet.type, loadedWallet.type)
            assertEquals(wallet.accountDescriptor, loadedWallet.accountDescriptor)

            loadedWallet.delete()

            assertEquals(null, Wallet.loadById(wallet.id, dbBuilder))
        }
    }

    @Test
    override fun should_load_more_wallets_successfully() {
        runTest {
            val nsecs =
                listOf(
                    "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k",
                    "nsec1msazr4ymx26cl83rl0wjulet9atuvlnlwyag9y6zz9rakvvh47rq99tupf",
                    "nsec1q390qprt2rl8urlfd2advh5s4rc3n4l8m0hpyrp8nd9f9tl3fkdqq9anc9",
                    "nsec1wzt73wjccrw4hm7wjpazp8vgcypvhu4egx3syzu6dgqz69kvewzs72kpx9",
                    "nsec1smd696h88hn2qje5ygzgx29n3u6dycvx2yh2lvgm2ey4q635manqnys59p",
                )
            val wallets = mutableListOf<Wallet>()
            for (nsec in nsecs) {
                val wallet = Wallet.create(nsec, serverInfo = serverInfo, dbBuilder = dbBuilder)
                wallets.add(wallet)
                wallet.save()
            }

            val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(dbBuilder) }

            val loadedWallets = repo.loadWallets().filter { w -> w.type == Wallet.Type.SINGLE_KEY }

            assertEquals(wallets.size, loadedWallets.size)

            for (loadedWallet in loadedWallets) {
                val wallet = wallets.find { w -> w.id == loadedWallet.id }!!
                assertEquals(wallet.secret, loadedWallet.secret)
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_vtxo_data_successfully() {
        runTest {
            val nsec = "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k"
            val wallet =
                Wallet.create(
                    nsec,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val vtxosJson = assertNotNull(validTestVtxosJsonArray, "Missing valid test VTXOs")

            wallet.deleteVtxos()

            vtxosJson.forEachIndexed { index, vtxoJson ->
                val (vtxo, comment) = vtxoFromJson(vtxoJson)
                wallet.saveVtxo(vtxo)
                val vtxos = wallet.getVtxos()
                assertEquals(index + 1, vtxos.size)
                assertEquals(vtxo, vtxos[index])
                Log.success(LOG_TAG, comment)
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_ark_contracts_successfully() {
        runTest {
            val nsec = "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k"
            val wallet =
                Wallet.create(
                    nsec,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val contractsJson = assertNotNull(validContractsJsonArray, "Missing valid test contracts")

            wallet.deleteContracts()

            contractsJson.forEachIndexed { index, contractJson ->
                val (contract, state) = contractFromJson(contractJson)
                wallet.saveContract(contract, state, Network.TESTNET)
                val contracts = wallet.getContracts()
                assertEquals(index + 1, contracts.size)

                assertEquals(contract.toString(), contracts[index].toString())
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_ark_intents_successfully() {
        runTest {
            val nsec = "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k"
            val wallet =
                Wallet.create(
                    nsec,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val intentsJson = assertNotNull(validIntentsJsonArray, "Missing valid test intents")

            wallet.deleteIntents()

            intentsJson.forEachIndexed { index, intentJson ->
                val intent = intentFromJson(intentJson, wallet.id)
                wallet.saveIntent(intent)
                val intents = wallet.getIntents()
                assertEquals(index + 1, intents.size)
                assertEquals(intent, intents[index])
            }
        }
    }

    @Test
    fun should_fail_constructing_invalid_vtxo_data() {
        runTest {
            val vtxosJson = assertNotNull(invalidTestVtxosJsonArray, "Missing invalid test VTXOs")
            vtxosJson.forEach { vtxoJson ->
                val comment =
                    vtxoJson.jsonObject["comment"]
                        ?.jsonPrimitive
                        .toString()
                        .removeSurrounding("\"")
                assertFailsWith<IllegalArgumentException> {
                    val (_, _) = vtxoFromJson(vtxoJson)
                }
                Log.success(LOG_TAG, comment)
            }
        }
    }

    @Test
    fun should_fail_constructing_invalid_contracts() {
        runTest {
            val contractsJson =
                assertNotNull(invalidContractsJsonArray, "Missing valid test contracts")

            contractsJson.forEachIndexed { index, contractJson ->
                val comment =
                    contractJson.jsonObject["comment"]
                        ?.jsonPrimitive
                        .toString()
                        .removeSurrounding("\"")
                assertFailsWith<IllegalArgumentException> {
                    contractFromJson(contractJson)
                    println(contractJson)
                }
                Log.success(LOG_TAG, comment)
            }
        }
    }

    @Test
    fun should_fail_constructing_invalid_intents() {
        runTest {
            val intentsJson =
                assertNotNull(invalidIntentsJsonArray, "Missing valid test contracts")

            intentsJson.forEachIndexed { index, intentJson ->
                val exception =
                    assertFailsWith<IllegalArgumentException> {
                        intentFromJson(intentJson, "test-wallet")
                    }
                Log.success(LOG_TAG, exception.message.toString())
            }
        }
    }

    companion object {
        private const val LOG_TAG = "SingleKeyWalletTest"
    }
}

class HDWalletTest : WalletTest() {
    private val serverInfo = getArkServerInfo()

    val testVtxoData = Json.parseToJsonElement(readJsonFile("fixtures/vtxo-data.json"))
    val validTestVtxosJsonArray = testVtxoData.jsonObject["valid"]?.jsonObject["vtxos"]?.jsonArray

    val testContractsData = Json.parseToJsonElement(readJsonFile("fixtures/contracts-data.json"))
    val validContractsJsonArray = testContractsData.jsonObject["valid"]?.jsonObject["contracts"]?.jsonArray
    val testIntentsData = Json.parseToJsonElement(readJsonFile("fixtures/intents-data.json"))
    val validIntentsJsonArray = testIntentsData.jsonObject["valid"]?.jsonObject["intents"]?.jsonArray

    @Test
    override fun should_create_wallet_successfully() {
        runTest {
            val secret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            val wallet =
                Wallet.create(
                    secret,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )
            assertEquals(secret, wallet.secret)
            assertEquals(Wallet.Type.HD, wallet.type)
            assertEquals(0, wallet.lastUsedIndex)

            wallet.save()

            val loadedWallet = assertNotNull(Wallet.loadById(wallet.id, dbBuilder))

            assertEquals(wallet.id, loadedWallet.id)
            assertEquals(wallet.secret, loadedWallet.secret)
            assertEquals(wallet.destination, loadedWallet.destination)
            assertEquals(wallet.type, loadedWallet.type)
            assertEquals(wallet.accountDescriptor, loadedWallet.accountDescriptor)
            assertEquals(wallet.lastUsedIndex, loadedWallet.lastUsedIndex)

            val (_, fingerprint) = masterKeyFromSecret(secret)

            val loadedWallet2 = assertNotNull(Wallet.loadByFingerprint(fingerprint, dbBuilder))
            assertEquals(fingerprint, loadedWallet2.fingerprint())
            assertEquals(wallet.id, loadedWallet2.id)
            assertEquals(wallet.secret, loadedWallet2.secret)
            assertEquals(wallet.destination, loadedWallet2.destination)
            assertEquals(wallet.type, loadedWallet2.type)
            assertEquals(wallet.accountDescriptor, loadedWallet2.accountDescriptor)
            assertEquals(wallet.lastUsedIndex, loadedWallet2.lastUsedIndex)

            loadedWallet.updateLastUsedIndex(1)

            val loadedWallet3 = assertNotNull(Wallet.loadById(loadedWallet.id, dbBuilder))

            assertEquals(1, loadedWallet3.lastUsedIndex)

            loadedWallet.delete()

            assertEquals(null, Wallet.loadById(wallet.id, dbBuilder))
        }
    }

    @Test
    override fun should_load_more_wallets_successfully() {
        runTest {
            val secrets =
                listOf(
                    "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
                    "legal winner thank year wave sausage worth useful legal winner thank yellow",
                    "letter advice cage absurd amount doctor acoustic avoid letter advice cage above",
                    "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
                    "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon agent",
                )
            val wallets = mutableListOf<Wallet>()
            for (secret in secrets) {
                val wallet = Wallet.create(secret, serverInfo = serverInfo, dbBuilder = dbBuilder)
                wallets.add(wallet)
                wallet.save()
            }

            val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(dbBuilder) }

            val loadedWallets = repo.loadWallets().filter { w -> w.type == Wallet.Type.HD }

            assertEquals(wallets.size, loadedWallets.size)

            for (loadedWallet in loadedWallets) {
                val wallet = wallets.find { w -> w.id == loadedWallet.id }!!
                assertEquals(wallet.secret, loadedWallet.secret)
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_vtxo_data_successfully() {
        runTest {
            val secret =
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            val wallet =
                Wallet.create(
                    secret,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val vtxosJson = assertNotNull(validTestVtxosJsonArray, "Missing valid test VTXOs")

            wallet.deleteVtxos()

            vtxosJson.forEachIndexed { index, vtxoJson ->
                val (vtxo, comment) = vtxoFromJson(vtxoJson)
                wallet.saveVtxo(vtxo)
                val vtxos = wallet.getVtxos()
                assertEquals(index + 1, vtxos.size)
                assertEquals(vtxo, vtxos[index])
                Log.success(LOG_TAG, comment)
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_ark_contracts_successfully() {
        runTest {
            val secret =
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            val wallet =
                Wallet.create(
                    secret,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val contractsJson =
                assertNotNull(validContractsJsonArray, "Missing valid test contracts")

            wallet.deleteContracts()

            contractsJson.forEachIndexed { index, contractJson ->
                val (contract, state) = contractFromJson(contractJson)
                wallet.saveContract(contract, state, Network.TESTNET)
                val contracts = wallet.getContracts()
                assertEquals(index + 1, contracts.size)

                assertEquals(contract.toString(), contracts[index].toString())
            }
        }
    }

    @Test
    override fun should_store_and_retrieve_valid_ark_intents_successfully() {
        runTest {
            val secret =
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            val wallet =
                Wallet.create(
                    secret,
                    serverInfo = serverInfo,
                    dbBuilder = dbBuilder,
                )

            val intentsJson = assertNotNull(validIntentsJsonArray, "Missing valid test intents")

            wallet.deleteIntents()

            intentsJson.forEachIndexed { index, intentJson ->
                val intent = intentFromJson(intentJson, wallet.id)
                wallet.saveIntent(intent)
                val intents = wallet.getIntents()
                assertEquals(index + 1, intents.size)
                assertEquals(intent, intents[index])
            }
        }
    }

    @Test
    fun should_load_null_wallet_for_nonexistent_fingerprint() {
        runTest {
            val wallet = Wallet.loadByFingerprint("00000000", dbBuilder)
            assertEquals(null, wallet)
        }
    }

    companion object {
        private const val LOG_TAG = "HDWalletTest"
    }
}

private fun vtxoFromJson(json: JsonElement): Pair<Vtxo.Data, String> {
    val comment =
        json.jsonObject["comment"]
            ?.jsonPrimitive
            .toString()
            .removeSurrounding("\"")
    val (txId, index) =
        json.jsonObject["outpoint"]
            ?.jsonPrimitive
            .toString()
            .removeSurrounding("\"")
            .split(":")
    val outpoint = OutPoint(TxId(txId), index.toLong())
    val script = json.jsonObject["script"]?.jsonPrimitive?.content!!
    val amount = json.jsonObject["amount"]?.jsonPrimitive?.long!!
    val createdAt = json.jsonObject["created_at"]?.jsonPrimitive?.long!!
    val expiresAt = json.jsonObject["expires_at"]?.jsonPrimitive?.long!!
    val isPreConfirmed =
        json.jsonObject["is_preconfirmed"]?.jsonPrimitive?.boolean!!
    val isSwept = json.jsonObject["is_swept"]?.jsonPrimitive?.boolean!!
    val isUnrolled = json.jsonObject["is_unrolled"]?.jsonPrimitive?.boolean!!
    val isSpent = json.jsonObject["is_spent"]?.jsonPrimitive?.boolean!!
    val spentBy =
        json.jsonObject["spent_by"]
            ?.jsonPrimitive
            ?.content
            ?.ifEmpty { null }
    val settledBy =
        json.jsonObject["settled_by"]
            ?.jsonPrimitive
            ?.content
            ?.ifEmpty { null }
    val arkTxId =
        json.jsonObject["ark_txid"]
            ?.jsonPrimitive
            ?.content
            ?.ifEmpty { null }
    val commitmentTxIds =
        json.jsonObject["commitment_txids"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: emptyList()
    val assets =
        json.jsonObject["assets"]?.jsonArray?.map { assetJson ->
            Json.decodeFromJsonElement<Asset>(assetJson)
        } ?: emptyList()

    return Vtxo.Data.normalized(
        outpoint,
        amount.toBigDecimal(),
        script,
        createdAt,
        expiresAt,
        isPreConfirmed,
        isSwept,
        isUnrolled,
        isSpent,
        spentBy,
        settledBy,
        arkTxId,
        commitmentTxIds,
        assets,
    ) to comment
}

private fun contractFromJson(json: JsonElement): Pair<ArkContract, ContractState> {
    val type = json.jsonObject["type"]?.jsonPrimitive?.content!!
    val state =
        when (json.jsonObject["state"]?.jsonPrimitive?.content!!) {
            "Active" -> ContractState.ACTIVE
            "InActive" -> ContractState.INACTIVE
            "AwaitingFundsBeforeDeactivate" -> ContractState.AWAITING_FUNDS_BEFORE_DEACTIVATE
            else -> throw IllegalArgumentException("Invalid contract state")
        }
    val data =
        json.jsonObject["data"]?.jsonObject!!.entries.associate { entry ->
            val key = entry.key
            val value = entry.value
            key to value.jsonPrimitive.content
        }
    return ArkContractParserImpl().parse(data, type) to state
}

private fun intentFromJson(
    json: JsonElement,
    walletId: String,
): ArkIntent {
    val txId = json.jsonObject["tx_id"]?.jsonPrimitive?.content!!
    val id = json.jsonObject["id"]?.jsonPrimitive?.content!!
    val state = IntentState.valueOf(json.jsonObject["state"]?.jsonPrimitive?.content!!)
    val validFrom = json.jsonObject["valid_from"]?.jsonPrimitive?.long!!
    val validUntil = json.jsonObject["valid_until"]?.jsonPrimitive?.long!!
    val createdAt = json.jsonObject["created_at"]?.jsonPrimitive?.long!!
    val updatedAt = json.jsonObject["updated_at"]?.jsonPrimitive?.long!!
    val registerProof = json.jsonObject["register_proof"]?.jsonPrimitive?.content!!
    val registerProofMessage = json.jsonObject["register_proof_message"]?.jsonPrimitive?.content!!
    val deleteProof = json.jsonObject["delete_proof"]?.jsonPrimitive?.content!!
    val deleteProofMessage = json.jsonObject["delete_proof_message"]?.jsonPrimitive?.content!!
    val batchId = json.jsonObject["batch_id"]?.jsonPrimitive?.content!!
    val commitmentTxId =
        json.jsonObject["commitment_txid"]
            ?.jsonPrimitive
            ?.content
            ?.ifEmpty { null }
    val cancellationReason = json.jsonObject["cancellation_reason"]?.jsonPrimitive?.content!!
    val vtxos =
        json.jsonObject["vtxos"]?.jsonArray!!.map {
            val (txId, index) = it.jsonPrimitive.content.split(":")
            OutPoint(TxId(txId), index.toLong())
        }
    val signerDescriptor = json.jsonObject["signer_descriptor"]?.jsonPrimitive?.content!!
    return ArkIntent(
        txId,
        id,
        walletId,
        state,
        validFrom,
        validUntil,
        createdAt,
        updatedAt,
        registerProof,
        registerProofMessage,
        deleteProof,
        deleteProofMessage,
        batchId,
        commitmentTxId,
        cancellationReason,
        vtxos,
        signerDescriptor,
    )
}
