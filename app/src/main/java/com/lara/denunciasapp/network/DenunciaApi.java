package com.lara.denunciasapp.network;

import com.lara.denunciasapp.model.Denuncia;
import com.lara.denunciasapp.model.DenunciaRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DenunciaApi {
    @GET("denuncias/")
    Call<List<Denuncia>> listar(@Query("offset") int offset, @Query("limit") int limit);

    @GET("denuncias/{id}")
    Call<Denuncia> obtener(@Path("id") int id);

    @GET("denuncias/ciudadano/{dni}")
    Call<List<Denuncia>> buscarPorDni(@Path("dni") String dni);

    @POST("denuncias/")
    Call<Denuncia> registrar(@Body DenunciaRequest denuncia);

    @PUT("denuncias/{id}")
    Call<Denuncia> atender(@Path("id") int id);
}
