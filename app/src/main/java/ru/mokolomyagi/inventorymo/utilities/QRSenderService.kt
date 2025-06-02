package ru.mokolomyagi.inventorymo.utilities

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Интерфейс API
interface QRSenderService {
    @POST("/api/v1/services/get_invertory/")
    fun sendQRData(@Body data: QRDataRequest): Call<QRResponseModel>
}