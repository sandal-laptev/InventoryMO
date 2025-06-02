package ru.mokolomyagi.inventorymo.utilities

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Создание сервиса Retrofit
object ServiceBuilder {
    private const val BASE_URL = "https://kolomyagiquest.ru/"

    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    internal inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}