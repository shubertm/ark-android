package com.arkade.core.bitcoin

import com.arkade.core.toXOnlyPubKey
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptorTest {
    @Test
    fun decode_taproot_descriptor_from_string() {
        val compressedPubKey = "03a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c"
        val descriptor = "tr(a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c)"

        val decoded = Descriptor.from(compressedPubKey)
        val encoded = decoded.encodeTaproot()

        val decodedFromDescriptor = Descriptor.from(descriptor)

        assertEquals(descriptor, decodedFromDescriptor.encodeTaproot())
        assertEquals(compressedPubKey.toXOnlyPubKey().value.toHex(), encoded.removeSurrounding("tr(", ")"))
    }

    @Test
    fun decode_taproot_descriptor_from_x_pubkey() {
        val xPubKey = "03a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c".toXOnlyPubKey()
        val decoded = Descriptor.from(xPubKey)
        val encoded = decoded.encodeTaproot()

        assertEquals(xPubKey.value.toHex(), encoded.removeSurrounding("tr(", ")"))
    }
}
