package com.arkade.core.wallet.signer

import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

interface WalletSignerManager {
    val signer: Signer

    suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction

    suspend fun signMessage(
        descriptor: String,
        message: ByteArray,
    ): ByteArray
}
