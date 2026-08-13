package com.arkade.core.batches

import fr.acinq.bitcoin.PublicKey

/**
 * The public key of one of the cosigners of a VTXO tree node, together with its position among
 * the cosigners.
 *
 * @property index The cosigner's position, as embedded in the PSBT's proprietary field key.
 * @property pubKey The cosigner's public key.
 */
data class CosignerPublicKeyData(
    val index: Byte,
    val pubKey: PublicKey,
)
