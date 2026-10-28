package com.lara.denunciasapp.network;

import com.lara.denunciasapp.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final DenunciaApi API = new Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(150, TimeUnit.SECONDS)
                    // Un POST no se debe repetir automaticamente si su respuesta se pierde.
                    .retryOnConnectionFailure(false)
                    .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DenunciaApi.class);

    private ApiClient() { }
    public static DenunciaApi get() { return API; }
}
