package com.arkade.core.assets

interface ExtensionPacket {
    val type: Byte

    fun serializePacketData(): ByteArray
}
