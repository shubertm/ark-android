package com.arkade.core.contracts

/**
 * Represents the lifecycle state of an [ArkContract].
 *
 * Contracts transition between states as funds move through the Ark protocol.
 */
enum class ContractState {
    /** The contract has been deactivated and is no longer in use. */
    INACTIVE,

    /** The contract is currently active and holding funds. */
    ACTIVE,

    /**
     * The contract is awaiting an incoming funding transaction before it will be deactivated.
     * This is a transitional state between [ACTIVE] and [INACTIVE].
     */
    AWAITING_FUNDS_BEFORE_DEACTIVATE,
}
