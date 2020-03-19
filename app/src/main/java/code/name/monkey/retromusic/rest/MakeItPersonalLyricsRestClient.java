package code.name.monkey.retromusic.rest;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import code.name.monkey.retromusic.providers.MakeItPersonalLyricsConverterFactory;
import code.name.monkey.retromusic.rest.service.LyricsService;
import code.name.monkey.retromusic.rest.service.MakeItPersonalLyricsService;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MakeItPersonalLyricsRestClient {

    private static final String BASE_URL = "https://makeitpersonal.co";

    private LyricsService apiService;

    public MakeItPersonalLyricsRestClient(@NonNull Context context) {
        this(createDefaultOkHttpClientBuilder(context).build());
    }

    private MakeItPersonalLyricsRestClient(@NonNull Call.Factory client) {
        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .callFactory(client)
                .addConverterFactory(new Converter.Factory() {
                    @Override
                    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
                        return new MakeItPersonalLyricsConverterFactory().responseBodyConverter(type, annotations,retrofit);
                    }
                })
                .build();

        apiService = restAdapter.create(LyricsService.class);
    }

    @NonNull
    public LyricsService getApiService() {
        return apiService;
    }

    private static Interceptor createCacheControlInterceptor() {
        return chain -> {
            Request modifiedRequest = chain.request().newBuilder()
                    .addHeader("Cache-Control", "max-age=31536000, max-stale=31536000")
                    .build();
            return chain.proceed(modifiedRequest);
        };
    }

    @Nullable
    private static Cache createDefaultCache(Context context) {
        File cacheDir = new File(context.getCacheDir().getAbsolutePath(), "/okhttp-makeitpersonal/");
        if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
            return new Cache(cacheDir, 1024 * 1024 * 10);
        }
        return null;
    }

    @NonNull
    private static OkHttpClient.Builder createDefaultOkHttpClientBuilder(@NonNull Context context) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                .retryOnConnectionFailure(true)
                .connectTimeout(1, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(1, TimeUnit.MINUTES) // write timeout
                .readTimeout(1, TimeUnit.MINUTES) // read timeout
                .cache(createDefaultCache(context))
                .addInterceptor(createCacheControlInterceptor())
                .addInterceptor(createLogInterceptor());
    }

    @NonNull
    private static Interceptor createLogInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
}
