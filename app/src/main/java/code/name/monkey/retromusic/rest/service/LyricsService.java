package code.name.monkey.retromusic.rest.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LyricsService {
    @GET("/lyrics")
    Call<String> lyrics(@Query("artist") String artistName, @Query("title") String songName);
}
