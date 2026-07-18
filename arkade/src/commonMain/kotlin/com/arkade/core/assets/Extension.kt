package com.arkade.core.assets

import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.OP_RETURN
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.io.ByteArrayInput
import fr.acinq.bitcoin.io.ByteArrayOutput
import fr.acinq.bitcoin.io.readNBytes

class Extension(
    private val packets: List<ExtensionPacket>,
) {
    fun getAssetPacket(): Packet? {
        packets.forEach { packet ->
            if (packet is Packet) return packet
        }
        return null
    }

    companion object {
        private val ArkadeMagic = byteArrayOf(0x41, 0x52, 0x4B)

        fun isExtension(script: ByteArray): Boolean {
            val script = Script.parse(script)
            if (
                script.isEmpty() ||
                script.size < 2 ||
                script[0] != OP_RETURN
            ) {
                return false
            }
            val op = script[1] as? OP_PUSHDATA ?: return false
            val data = op.data
            if (data.size() == 0 || data.size() < ArkadeMagic.size) return false

            return data.take(ArkadeMagic.size).contentEquals(ArkadeMagic)
        }

        fun fromScript(script: ByteArray): Extension {
            val script = Script.parse(script)
            require(script.isNotEmpty()) { "Missing OP_RETURN" }
            require(script[0] == OP_RETURN) { "Expected OP_RETURN" }

            val payload = ByteArrayOutput()
            for (i in 1 until script.size) {
                val op = script[i]
                if (op is OP_PUSHDATA) {
                    payload.write(op.data.toByteArray())
                }
            }
            return fromPayload(payload.toByteArray())
        }

        private fun fromPayload(payload: ByteArray): Extension {
            val payloadInput = ByteArrayInput(payload)

            require(payloadInput.availableBytes >= ArkadeMagic.size) {
                "Missing Arkade magic prefix"
            }
            val magic = payloadInput.readNBytes(ArkadeMagic.size)

            require(magic!!.contentEquals(ArkadeMagic)) {
                "Expected magic prefix ${ArkadeMagic.toHexString().lowercase()}, got ${magic.toHexString().lowercase()}"
            }

            val packets =
                runCatching {
                    val packets: MutableList<ExtensionPacket> = mutableListOf()
                    while (payloadInput.availableBytes > 0) {
                        val packetType = payloadInput.read().toByte()
                        val packetData = payloadInput.readVarBytes()
                        requireNotNull(packetData) { "Missing packet data" }
                        val packet = parsePacket(packetType, packetData)
                        packets.add(packet)
                    }
                    packets
                }.getOrElse { e ->
                    throw IllegalArgumentException("Invalid extension payload", e)
                }

            require(packets.isNotEmpty()) { "Missing packets" }

            val seenPacketsTypes: HashSet<Byte> = hashSetOf()
            packets.forEach { packet ->
                val isNotSeen = seenPacketsTypes.add(packet.type)
                require(isNotSeen) { "Duplicate packet type: ${packet.type}" }
            }
            return Extension(packets)
        }

        private fun parsePacket(
            packetType: Byte,
            packetData: ByteArray,
        ): ExtensionPacket =
            when (packetType) {
                Packet.PACKET_TYPE -> Packet.fromBytes(packetData)
                else -> UnknownPacket(packetType, packetData)
            }
    }
}
