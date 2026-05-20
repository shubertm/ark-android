package com.arkade.core.bitcoin

enum class Network {
    MAINNET,
    TESTNET,
    SIGNET,
    REGTEST,
    ;

    companion object {
        /**
         * Parse a network name into the corresponding Network enum value.
         *
         * Recognizes the following case-insensitive names and aliases:
         * - "mainnet", "bitcoin" -> MAINNET
         * - "testnet" -> TESTNET
         * - "signet", "mutinynet" -> SIGNET
         * - "regtest" -> REGTEST
         *
         * @param network The network name to parse (case-insensitive).
         * @return The matching `Network` enum constant.
         * @throws IllegalArgumentException If `network` does not match any supported name.
         */
        fun fromString(network: String): Network =
            when (network.lowercase()) {
                "mainnet" -> MAINNET
                "bitcoin" -> MAINNET
                "testnet" -> TESTNET
                "signet" -> SIGNET
                "mutinynet" -> SIGNET
                "regtest" -> REGTEST
                else -> throw IllegalArgumentException("Invalid network: $network")
            }
    }
}
