package com.arkade.core.wallet

import com.arkade.core.ArkAddress
import com.arkade.core.ArkServerInfo
import com.arkade.core.bitcoin.Network
import com.arkade.di.ArkadeDI
import com.arkade.repositories.WalletRepo
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.WalletEntity
import fr.acinq.bitcoin.Bech32
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.DeterministicWallet
import fr.acinq.bitcoin.KeyPath
import fr.acinq.bitcoin.MnemonicCode
import fr.acinq.bitcoin.PrivateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf

interface Wallet {
    val id: String
    val secret: String
    val destination: String?
    val type: Type
    val accountDescriptor: String
    val lastUsedIndex: Int

    val repo: WalletRepo

    /**
     * Persist the wallet's current state to its configured repository.
     *
     * This operation ensures the wallet's properties (id, secret, destination, type,
     * accountDescriptor, lastUsedIndex) are stored or updated in persistent storage.
     */
    suspend fun save()

    /**
     * Delete this wallet from persistent storage.
     *
     * Performs the repository-level removal so the wallet is no longer stored.
     */
    suspend fun delete()

    /**
     * Update the persisted wallet record to match the wallet's current state.
     *
     * Persists the wallet's properties (for example: `secret`, `destination`, `type`,
     * `accountDescriptor`, and `lastUsedIndex`) to the configured repository.
     */
    suspend fun update()

    /**
     * Set the wallet's last used address index and persist the change.
     *
     * @param index The new last-used address index (must be greater than or equal to 0).
     */
    suspend fun updateLastUsedIndex(index: Int)

    /**
     * Converts this wallet into a Room persistence entity.
     *
     * @return A `WalletEntity` containing this wallet's `id`, `secret`, optional `destination`,
     * `type`, `accountDescriptor`, and `lastUsedIndex`.
     */
    fun toRoomEntity(): WalletEntity =
        WalletEntity(
            id,
            secret,
            destination,
            type,
            fingerprint(),
            accountDescriptor,
            lastUsedIndex,
        )

    /**
     * Retrieves the wallet's fingerprint.
     *
     * @return The wallet's fingerprint if it exists (`type` is `HD`), `null` otherwise.
     */
    fun fingerprint(): String? {
        return if (type == Type.HD) {
            val startIndex = accountDescriptor.indexOf('[')
            if (startIndex == -1) return null
            val endIndex = accountDescriptor.indexOf('/', startIndex + 1)
            if (endIndex == -1) return null
            val fingerprint = accountDescriptor.substring(startIndex + 1, endIndex)
            require(fingerprint.length == 8) { "Invalid fingerprint length: expected 8 but is ${fingerprint.length}" }
            fingerprint
        } else {
            null
        }
    }

    enum class Type {
        HD,
        SINGLE_KEY,
    }

    companion object {
        private const val NSEC_HRP = "nsec"

        /**
         * Create a Wallet from a secret (mnemonic phrase or an nsec-encoded private key) and an
         * optional destination tied to the provided server information.
         *
         * @param secret Either an HD mnemonic phrase or an nsec-encoded private key
         * (prefix "nsec"); when `secret` starts with `nsec`, a single-key wallet is created,
         * otherwise an HD wallet is created.
         * @param destination Optional [ArkAddress] for the wallet; when provided, the destination
         * is validated against `serverInfo`.
         * @param serverInfo Server information used to validate the destination and to derive
         * network-specific values for HD wallet creation.
         * @param testDb Optional in-memory or test database passed to the repository for
         * initialization.
         * @return The created [Wallet] instance.
         */
        suspend fun create(
            secret: String,
            destination: String? = null,
            serverInfo: ArkServerInfo,
            testDb: Database? = null,
        ): Wallet =
            withContext(Dispatchers.IO) {
                if (destination != null) {
                    validateDestination(destination, serverInfo)
                }

                val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }

                if (secret.startsWith(NSEC_HRP)) {
                    createNSecWallet(secret, destination, repo)
                } else {
                    createHDWallet(secret, destination, serverInfo, repo)
                }
            }

        /**
         * Load a wallet by its identifier from persistent storage.
         *
         * @param id The wallet identifier to look up.
         * @param testDb Optional database instance used for repository initialization
         * (primarily for tests).
         * @return The wallet with the given `id`, or `null` if no matching wallet is found.
         */
        suspend fun loadById(
            id: String,
            testDb: Database? = null,
        ): Wallet? =
            withContext(Dispatchers.IO) {
                val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }
                repo.loadWalletById(id)
            }

        /**
         * Load a wallet by its fingerprint from persistent storage.
         *
         * @param fingerprint The wallet fingerprint to look up.
         * @param testDb Optional database instance used for repository initialization
         * (primarily for tests).
         * @return The wallet with the given `fingerprint`, or `null` if no matching wallet is found.
         */
        suspend fun loadByFingerprint(
            fingerprint: String,
            testDb: Database? = null,
        ): Wallet? =
            withContext(Dispatchers.IO) {
                val repo: WalletRepo = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }
                repo.loadWalletByFingerprint(fingerprint)
            }

        /**
         * Builds a Taproot output descriptor from an nsec-encoded private key.
         *
         * @param nsec The nsec (Bech32) encoded private key string.
         * @return A Taproot output descriptor in the form `tr(<xOnlyPublicKeyHex>)`.
         */
        fun getOutputDescriptorFromNSec(nsec: String): String {
            val privateKey = getPrivateKeyFromNSec(nsec)
            return "tr(${privateKey.publicKey().xOnly().value.toHex()})"
        }

        /**
         * Derives a master key from the provided mnemonic phrase.
         *
         * @param mnemonics is the mnemonic phrase to use for key derivation.
         * @return A pair containing the derived master key and its fingerprint.
         */
        fun masterKeyFromSecret(mnemonics: String): Pair<DeterministicWallet.ExtendedPrivateKey, String> {
            val seed = MnemonicCode.toSeed(mnemonics, "")
            val masterKey = DeterministicWallet.generate(seed)
            val fingerprint = masterKey.extendedPublicKey.keyFingerprint()
            return masterKey to fingerprint
        }

        /**
         * Extracts the first 32 bits of RIPEMD160 of SHA256 of a serialized extended public key
         *
         * @return A 4 byte hex string fingerprint
         */
        fun DeterministicWallet.ExtendedPublicKey.keyFingerprint(): String =
            Crypto
                .hash160(publickeybytes)
                .take(4)
                .toByteArray()
                .toHexString()

        /**
         * Creates a single-key wallet backed by the provided nsec-encoded private key.
         *
         * @param nsec The nsec-encoded private key string.
         * @param destination Optional destination address associated with the wallet.
         * @param repo Repository instance used by the returned wallet for persistence.
         * @return A [Wallet] initialized with a Taproot output descriptor derived from `nsec`,
         * [Type.SINGLE_KEY], and `lastUsedIndex` set to 0.
         */
        private fun createNSecWallet(
            nsec: String,
            destination: String?,
            repo: WalletRepo,
        ): Wallet {
            val outputDescriptor = getOutputDescriptorFromNSec(nsec)
            return WalletImpl(
                repo,
                outputDescriptor,
                nsec,
                destination,
                Type.SINGLE_KEY,
                outputDescriptor,
                0,
            )
        }

        /**
         * Creates an HD wallet from the provided mnemonic phrase and server information.
         *
         * Derives the Taproot account descriptor for the wallet using the server's network
         * and returns a [Wallet] configured as an HD wallet with `lastUsedIndex` set to 0.
         *
         * @param mnemonics The mnemonic phrase to validate and use as the wallet seed.
         * @param destination Optional destination address associated with the wallet.
         * @param serverInfo Server network and signing information used to choose coin type
         * and key encoding.
         * @param repo Repository used by the returned Wallet for persistence.
         * @return A [Wallet] instance of type `HD` with a derived Taproot account descriptor.
         */
        private fun createHDWallet(
            mnemonics: String,
            destination: String?,
            serverInfo: ArkServerInfo,
            repo: WalletRepo,
        ): Wallet {
            runCatching {
                MnemonicCode.validate(mnemonics)
            }.onFailure { throw it }

            val (masterKey, fingerprint) = masterKeyFromSecret(mnemonics)

            fun encodePubKeyByNetwork(
                pubKey: DeterministicWallet.ExtendedPublicKey,
                network: Network,
            ): String =
                when (network) {
                    Network.MAINNET -> pubKey.encode(false)
                    else -> pubKey.encode(true)
                }
            val coinType =
                when (serverInfo.network) {
                    Network.MAINNET -> 0
                    else -> 1
                }
            val accountKeyPath = KeyPath("m/86'/$coinType'/0'")
            val accountPrivateKey = masterKey.derivePrivateKey(accountKeyPath)
            val accountPublicKey = encodePubKeyByNetwork(accountPrivateKey.extendedPublicKey, serverInfo.network)
            require(fingerprint.length == 8) { "Invalid fingerprint length: expected 8 but is ${fingerprint.length}" }
            val accountDescriptor = "tr([$fingerprint/86'/$coinType'/0']$accountPublicKey/0/*)"

            return WalletImpl(
                repo,
                accountDescriptor,
                mnemonics,
                destination,
                Type.HD,
                accountDescriptor,
                0,
            )
        }

        /**
         * Validates that the provided Ark address targets the given server by comparing server
         * public keys.
         *
         * @param address Bech32-encoded [ArkAddress] whose embedded server public key will be
         * checked.
         * @param serverInfo Server information whose `signerPubKey` must match the address's
         * server public key.
         * @throws IllegalArgumentException if the address's server public key does not match
         * `serverInfo.signerPubKey`.
         */
        private fun validateDestination(
            address: String,
            serverInfo: ArkServerInfo,
        ) {
            val arkAddress = ArkAddress.decode(address)
            if (!serverInfo.signerPubKey.value
                    .toByteArray()
                    .contentEquals(arkAddress.serverPubKey)
            ) {
                throw IllegalArgumentException("Invalid destination server key")
            }
        }

        /**
         * Decodes a Bech32 `nsec` string and returns the corresponding private key.
         *
         * @param nsec The Bech32-encoded secret (expected HRP "nsec").
         * @return The private key represented by the decoded nsec payload.
         * @throws IllegalArgumentException If the HRP is not "nsec" or the decoded payload
         * is not 32 bytes.
         */
        private fun getPrivateKeyFromNSec(nsec: String): PrivateKey {
            val (hrp, bytes, _) = Bech32.decodeBytes(nsec)
            require(hrp == NSEC_HRP) { "Invalid nsec HRP: $hrp" }
            require(bytes.size == 32) { "Invalid nsec payload size: ${bytes.size}" }
            return PrivateKey.fromHex(bytes.toHexString())
        }
    }
}
