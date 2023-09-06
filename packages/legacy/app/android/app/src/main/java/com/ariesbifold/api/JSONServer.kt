package com.ariesbifold.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface JSONServer {
    @GET("/urls/{id}")
    fun url(@Path("id") id: String): Call<Data>

    @GET("/urls")
    fun urls(): Call<List<Data>>
}
