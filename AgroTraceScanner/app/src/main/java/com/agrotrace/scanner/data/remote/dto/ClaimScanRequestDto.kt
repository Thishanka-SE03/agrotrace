package com.agrotrace.scanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClaimScanRequestDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("pairingCode") val pairingCode: String,
    @SerializedName("claimToken") val claimToken: String
)
