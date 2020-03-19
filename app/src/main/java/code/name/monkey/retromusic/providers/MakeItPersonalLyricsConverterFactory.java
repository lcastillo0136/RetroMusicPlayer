package code.name.monkey.retromusic.providers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class MakeItPersonalLyricsConverterFactory extends Converter.Factory {

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return MakeItPersonalLyricsConverter.INSTANCE;
    }

    final static class MakeItPersonalLyricsConverter implements Converter<ResponseBody, String> {
        static final MakeItPersonalLyricsConverter INSTANCE = new MakeItPersonalLyricsConverter();

        @Override
        public String convert(ResponseBody responseBody) throws IOException {
            try {
                return responseBody.string();
            } catch (Exception e) {
                throw new IOException("Failed to parse String", e);
            }
        }
    }
}
