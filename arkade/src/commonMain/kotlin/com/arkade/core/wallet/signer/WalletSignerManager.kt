package com.arkade.core.wallet.signer

import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

interface WalletSignerManager {
    val signer: Signer

    suspend fun sign(
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction

    suspend fun signMessage(message: ByteArray): ByteArray
}
