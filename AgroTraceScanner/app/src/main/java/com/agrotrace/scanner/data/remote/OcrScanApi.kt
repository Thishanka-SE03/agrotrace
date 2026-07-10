package com.agrotrace.scanner.data.remote

import com.agrotrace.scanner.data.remote.dto.ClaimScanRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface OcrScanApi {

    @POST
    suspend fun claimScan(
        @Url url: String,
        @Body request: ClaimScanRequestDto
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Part image: MultipartBody.Part,
        @Part("deviceId") deviceId: RequestBody,
        @Part("claimToken") claimToken: RequestBody
    ): Response<ResponseBody>
}
