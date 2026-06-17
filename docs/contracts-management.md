# Contracts Management in Wallet

## Overview

This document describes the persistent Ark contract storage feature added to the `Wallet` interface. Each wallet can now save, retrieve, and delete `ArkContract` instances, scoped by wallet ID. The implementation follows the same repository/storage layered architecture used for VTXOs.

---

## Architecture

```
Wallet  (saveContract / getContracts / deleteContracts)
  └── WalletImpl  (delegates to WalletRepo)
        └── WalletRepo / WalletRepoImpl  (delegates to ContractRepo)
              └── ContractRepo / ContractRepoImpl  (ArkContractParserImpl injected via DI)
                    └── ContractStorage / ContractStorageImpl
                          └── ContractDao  (Room DAO)
```

---

## Wallet Interface — New Methods

```kotlin
interface Wallet {

    /**
     * Persists an [ArkContract] for this wallet.
     *
     * Associates the contract with the wallet's own [id] and the given [network]
     * so the correct `scriptPubKey` can be derived. Saving a contract whose
     * `scriptPubKey` already exists replaces the existing entry.
     *
     * @param contract The contract to persist.
     * @param state    The [ContractState] to associate with this contract.
     * @param network  The Bitcoin network used to derive the contract's `scriptPubKey`.
     */
    suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        network: Network,
    )

    /**
     * Returns all [ArkContract] instances persisted for this wallet.
     *
     * @return A list of contracts owned by this wallet, or an empty list if none exist.
     */
    suspend fun getContracts(): List<ArkContract>

    /**
     * Deletes all [ArkContract] instances persisted for this wallet.
     */
    suspend fun deleteContracts()
}
```

### Usage Example

```kotlin
// Save a boarding contract
wallet.saveContract(
    contract = boardingContract,
    state = ContractState.PENDING,
    network = Network.SIGNET,
)

// Load all contracts for this wallet
val contracts: List<ArkContract> = wallet.getContracts()

// Remove all contracts (e.g. on wallet wipe)
wallet.deleteContracts()
```

---

## WalletRepo Interface — New Methods

```kotlin
interface WalletRepo {

    /** Provides access to the underlying contract repository. */
    val contractRepo: ContractRepo

    /**
     * Persists an [ArkContract] for the wallet identified by [walletId].
     *
     * @param contract  The contract to persist.
     * @param state     The [ContractState] to associate with this contract.
     * @param walletId  The ID of the owning wallet.
     * @param network   The Bitcoin network used to derive the `scriptPubKey`.
     */
    suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    )

    /**
     * Retrieves a single [ArkContract] by its P2TR `scriptPubKey`.
     *
     * @param scriptPubKey Hex-encoded `scriptPubKey` identifying the contract.
     * @return The matching [ArkContract].
     * @throws IllegalArgumentException if no contract with the given key exists.
     */
    suspend fun getContract(scriptPubKey: String): ArkContract

    /**
     * Retrieves all [ArkContract] instances for the wallet identified by [walletId].
     *
     * @param walletId The wallet whose contracts are to be fetched.
     * @return A list of contracts; empty if none are found.
     */
    suspend fun getContracts(walletId: String): List<ArkContract>

    /**
     * Deletes all [ArkContract] instances for the wallet identified by [walletId].
     *
     * @param walletId The wallet whose contracts are to be deleted.
     */
    suspend fun deleteContracts(walletId: String)
}
```

---

## ContractRepo Interface

```kotlin
interface ContractRepo {

    /**
     * Persists an [ArkContract].
     *
     * @param contract  The contract to persist.
     * @param state     The [ContractState] to associate with this contract.
     * @param walletId  The ID of the owning wallet.
     * @param network   The Bitcoin network used to derive the `scriptPubKey`.
     */
    suspend fun save(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    )

    /**
     * Retrieves a single [ArkContract] by its P2TR `scriptPubKey`.
     *
     * @param scriptPubKey Hex-encoded scriptPubKey identifying the contract.
     * @return The matching [ArkContract].
     * @throws IllegalArgumentException if no contract exists for the given key.
     */
    suspend fun get(scriptPubKey: String): ArkContract

    /**
     * Retrieves all [ArkContract] instances belonging to [walletId].
     *
     * @param walletId The owning wallet's identifier.
     * @return A list of contracts; empty if none exist.
     */
    suspend fun getAll(walletId: String): List<ArkContract>

    /**
     * Deletes all [ArkContract] instances belonging to [walletId].
     *
     * @param walletId The owning wallet's identifier.
     */
    suspend fun deleteAll(walletId: String)
}
```

---

## ContractStorage Interface

```kotlin
interface ContractStorage {

    /**
     * Upserts a [ContractEntity] into the database.
     * An existing entry with the same `scriptPubKey` is replaced.
     *
     * @param contract The entity to save or update.
     */
    suspend fun save(contract: ContractEntity)

    /**
     * Retrieves a single [ContractEntity] by its `scriptPubKey`.
     *
     * @param scriptPubKey Hex-encoded P2TR scriptPubKey.
     * @return The matching entity, or null if none exists.
     */
    suspend fun get(scriptPubKey: String): ContractEntity?

    /**
     * Retrieves all [ContractEntity] rows belonging to [walletId].
     *
     * @param walletId The owning wallet's identifier.
     * @return A list of contract entities; empty if none exist.
     */
    suspend fun getAll(walletId: String): List<ContractEntity>

    /**
     * Deletes all [ContractEntity] rows belonging to [walletId].
     *
     * @param walletId The owning wallet's identifier.
     */
    suspend fun deleteAll(walletId: String)
}
```

---

## Taproot Utilities — Changed API

### `parseTaprootDescriptor` (replaces `taprootDescriptorFromPubKey`)

```kotlin
/**
 * Converts a raw hex public key or an existing tr(...) descriptor into a
 * normalized Taproot descriptor of the form tr(<xOnlyPubKeyHex>).
 *
 * Accepted input formats:
 * - A compressed (33-byte) hex public key.
 * - An x-only (32-byte) hex public key.
 * - An existing tr(<hex>) descriptor — the inner key is re-normalized.
 *
 * @param string A hex public key or a tr(...) descriptor string.
 * @return A normalized Taproot descriptor: tr(<xOnlyPubKeyHex>).
 */
fun parseTaprootDescriptor(string: String): String
```

### `pubKeyFromTaprootDescriptor` (unchanged signature)

```kotlin
/**
 * Extracts the hex-encoded x-only public key from a Taproot descriptor.
 *
 * @param descriptor A Taproot descriptor string, e.g. tr(<xOnlyPubKeyHex>).
 * @return The hex-encoded x-only public key contained in the descriptor.
 */
fun pubKeyFromTaprootDescriptor(descriptor: String): String
```

---

## `ArkBoardingContract` — Enhanced Validation

`ArkBoardingContract.Companion.parse()` now enforces stricter input validation:

| Validation | Behaviour |
|---|---|
| `server` key absent | throws `IllegalArgumentException("Invalid server public key")` |
| `server` value is blank | throws `IllegalArgumentException("Invalid server public key")` |
| `user` key absent | throws `IllegalArgumentException("Invalid user public key")` |
| `user` value is blank | throws `IllegalArgumentException("Invalid user public key")` |
| `exit_delay` is negative | throws `IllegalArgumentException("Invalid exit delay")` |
| `exit_delay` absent | defaults to `0` |

Both `server` and `user` strings are passed through `parseTaprootDescriptor()` before the contract is constructed.

---

## Dependency Injection — New Module

A new `parsersModule` Koin module registers `ArkContractParserImpl` as a **singleton**:

```kotlin
val parsersModule = module {
    single { ArkContractParserImpl() }
}
```

This module is included in `ArkadeDI` alongside `databaseModule`, `storageModule`, and `repoModule`.
`ContractRepoImpl` resolves the shared parser instance from the Koin container rather than creating a new parser on every call.

---

## Storage Interface Rename

| Before | After |
|---|---|
| `interface Storage` (package `com.arkade.core.wallet`) | `interface WalletStorage` (package `com.arkade.storage`) |
| `internal class StorageImpl` | `internal class WalletStorageImpl` |

Update any direct references or DI bindings that used `Storage` / `StorageImpl` to use `WalletStorage` / `WalletStorageImpl` respectively.
