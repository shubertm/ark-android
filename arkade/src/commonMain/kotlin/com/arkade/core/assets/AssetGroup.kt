package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput

class AssetGroup(
    val assetId: AssetId?,
    val controlAsset: AssetRef?,
    val inputs: List<AssetInput>,
    val outputs: List<AssetOutput>,
    val metadata: List<AssetMetadata>?,
) {
    val isIssuance: Boolean
        get() = assetId == null

    fun validate() {
        require(inputs.isNotEmpty() || outputs.isNotEmpty()) { "Empty asset group" }
        if (isIssuance) {
            require(inputs.isEmpty()) { "Issuance asset group must have no inputs" }
        } else {
            require(controlAsset == null) { "Only issuance can have a control asset" }
        }

        if (inputs.size > 1) {
            val firstType = inputs[0].type
            val allSameType = inputs.all { input -> input.type == firstType }
            require(allSameType) { "Asset inputs must be of the same type" }

            val seenVins: HashSet<Int> = hashSetOf()
            inputs.forEach { input ->
                val isNotSeen = seenVins.add(input.vin)
                require(isNotSeen) { "Duplicate asset input vin: ${input.vin}" }
            }
        }

        if (outputs.size > 1) {
            val seenVouts: HashSet<Int> = hashSetOf()
            outputs.forEach { output ->
                val isNotSeen = seenVouts.add(output.vout)
                require(isNotSeen) { "Duplicate asset output vout: ${output.vout}" }
            }
        }
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetGroup {
            val presence = input.read()

            var assetId: AssetId? = null
            var controlAsset: AssetRef? = null
            var metadata: List<AssetMetadata>? = null

            if ((presence and MASK_ASSET_ID) != 0) {
                assetId = AssetId.fromBytesInput(input)
            }

            if ((presence and MASK_CONTROL_ASSET) != 0) {
                controlAsset = AssetRef.fromBytesInput(input)
            }

            if ((presence and MASK_METADATA) != 0) {
                metadata = deserializeMetadataList(input)
            }

            val inputCount = input.readVarInt().toInt()
            val inputs: MutableList<AssetInput> = mutableListOf()
            for (i in 0 until inputCount) {
                inputs.add(AssetInput.fromBytesInput(input))
            }

            val outputCount = input.readVarInt().toInt()
            val outputs: MutableList<AssetOutput> = mutableListOf()
            for (i in 0 until outputCount) {
                outputs.add(AssetOutput.fromBytesInput(input))
            }

            val group = AssetGroup(assetId, controlAsset, inputs, outputs, metadata)
            group.validate()
            return group
        }

        private fun deserializeMetadataList(input: ByteArrayInput): List<AssetMetadata> {
            val count = input.readVarInt().toInt()
            val metadata: MutableList<AssetMetadata> = mutableListOf()
            for (i in 0 until count) {
                metadata.add(AssetMetadata.fromBytesInput(input))
            }
            return metadata
        }
    }
}
