package com.example.downloaderdemo;

import androidx.annotation.Size;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ApiService {
    @GET("/{dim}/")
    Call<ResponseBody> downloadImage(@Path("dim") String dim );

}
