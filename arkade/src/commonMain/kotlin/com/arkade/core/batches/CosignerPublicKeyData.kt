package com.arkade.core.batches

import fr.acinq.bitcoin.PublicKey

data class CosignerPublicKeyData(
    val index: Byte,
    val pubKey: PublicKey,
)
