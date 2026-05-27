package com.arkade.core.assets

import com.arkade.core.Json
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    @SerialName("asset_id")
    val id: String,
    @Serializable(Json.BigDecimalSerializer::class)
    val amount: BigDecimal,
)
