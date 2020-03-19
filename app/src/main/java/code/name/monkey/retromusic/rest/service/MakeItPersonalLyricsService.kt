package code.name.monkey.retromusic.rest.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MakeItPersonalLyricsService {
    @GET("lyrics")
    suspend fun lyrics(@Query("artist") artistName: String,@Query("title") songName: String): Call<String>
}