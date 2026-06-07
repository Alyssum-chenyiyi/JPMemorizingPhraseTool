package com.example.memophrasestool

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DeepSeekClient {

    val api: DeepSeekApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }
}