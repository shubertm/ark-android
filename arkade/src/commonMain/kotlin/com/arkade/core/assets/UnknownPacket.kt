package com.arkade.core.assets

class UnknownPacket(
    override val type: Byte,
    val data: ByteArray,
) : ExtensionPacket {
    override fun serializePacketData(): ByteArray = data
}
