package com.arkade.core.wallet

import com.arkade.core.ArkServerInfo
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Hrp
import com.arkade.core.bitcoin.Network
import com.arkade.core.bitcoin.WitnessVersion
import com.arkade.core.toXOnlyPubKey
import com.arkade.core.wallet.Wallet.Companion.masterKeyFromSecret
import com.arkade.di.ArkadeDI
import com.arkade.repositories.WalletRepo
import com.arkade.storage.db.Database
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

expect abstract class WalletTest() : com.arkade.Test {
    val testDb: Database

    @Test
    abstract fun should_create_wallet_successfully()

    @Test
    abstract fun should_load_more_wallets_successfully()
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
        deprecatedSigners = listOf(),
        serviceStatus = mapOf(),
        digest = "50da3e81cba4844be3559638cf7104a64e30c616bd5862e86b3903222ece0994",
        maxTxWeight = 40000,
        maxOpReturnOutputs = 3,
    )

class SingleKeyWalletTest : WalletTest() {
    private val serverInfo = getArkServerInfo()

    @Test
    override fun should_create_wallet_successfully() {
        runTest {
            val nsec = "nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k"
            val wallet =
                Wallet.create(
                    nsec,
                    serverInfo = serverInfo,
                    testDb = testDb,
                )
            assertEquals(nsec, wallet.secret)
            assertEquals(Wallet.Type.SINGLE_KEY, wallet.type)

            wallet.save()

            val loadedWallet = assertNotNull(Wallet.loadById(wallet.id, testDb))

            assertEquals(wallet.id, loadedWallet.id)
            assertEquals(wallet.secret, loadedWallet.secret)
            assertEquals(wallet.destination, loadedWallet.destination)
            assertEquals(wallet.type, loadedWallet.type)
            assertEquals(wallet.accountDescriptor, loadedWallet.accountDescriptor)

            loadedWallet.delete()

            assertEquals(null, Wallet.loadById(wallet.id, testDb))
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
                val wallet = Wallet.create(nsec, serverInfo = serverInfo, testDb = testDb)
                wallets.add(wallet)
                wallet.save()
            }

            val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }

            val loadedWallets = repo.loadWallets().filter { w -> w.type == Wallet.Type.SINGLE_KEY }

            assertEquals(wallets.size, loadedWallets.size)

            for (loadedWallet in loadedWallets) {
                val wallet = wallets.find { w -> w.id == loadedWallet.id }!!
                assertEquals(wallet.secret, loadedWallet.secret)
            }
        }
    }
}

class HDWalletTest : WalletTest() {
    private val serverInfo = getArkServerInfo()

    @Test
    override fun should_create_wallet_successfully() {
        runTest {
            val secret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            val wallet =
                Wallet.create(
                    secret,
                    serverInfo = serverInfo,
                    testDb = testDb,
                )
            assertEquals(secret, wallet.secret)
            assertEquals(Wallet.Type.HD, wallet.type)
            assertEquals(0, wallet.lastUsedIndex)

            wallet.save()

            val loadedWallet = assertNotNull(Wallet.loadById(wallet.id, testDb))

            assertEquals(wallet.id, loadedWallet.id)
            assertEquals(wallet.secret, loadedWallet.secret)
            assertEquals(wallet.destination, loadedWallet.destination)
            assertEquals(wallet.type, loadedWallet.type)
            assertEquals(wallet.accountDescriptor, loadedWallet.accountDescriptor)
            assertEquals(wallet.lastUsedIndex, loadedWallet.lastUsedIndex)

            val (_, fingerprint) = masterKeyFromSecret(secret)

            val loadedWallet2 = assertNotNull(Wallet.loadByFingerprint(fingerprint, testDb))
            assertEquals(fingerprint, loadedWallet2.fingerprint())
            assertEquals(wallet.id, loadedWallet2.id)
            assertEquals(wallet.secret, loadedWallet2.secret)
            assertEquals(wallet.destination, loadedWallet2.destination)
            assertEquals(wallet.type, loadedWallet2.type)
            assertEquals(wallet.accountDescriptor, loadedWallet2.accountDescriptor)
            assertEquals(wallet.lastUsedIndex, loadedWallet2.lastUsedIndex)

            loadedWallet.updateLastUsedIndex(1)

            val loadedWallet3 = assertNotNull(Wallet.loadById(loadedWallet.id, testDb))

            assertEquals(1, loadedWallet3.lastUsedIndex)

            loadedWallet.delete()

            assertEquals(null, Wallet.loadById(wallet.id, testDb))
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
                val wallet = Wallet.create(secret, serverInfo = serverInfo, testDb = testDb)
                wallets.add(wallet)
                wallet.save()
            }

            val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }

            val loadedWallets = repo.loadWallets().filter { w -> w.type == Wallet.Type.HD }

            assertEquals(wallets.size, loadedWallets.size)

            for (loadedWallet in loadedWallets) {
                val wallet = wallets.find { w -> w.id == loadedWallet.id }!!
                assertEquals(wallet.secret, loadedWallet.secret)
            }
        }
    }

    @Test
    fun should_load_null_wallet_for_nonexistent_fingerprint() {
        runTest {
            val wallet = Wallet.loadByFingerprint("00000000", testDb)
            assertEquals(null, wallet)
        }
    }
}
