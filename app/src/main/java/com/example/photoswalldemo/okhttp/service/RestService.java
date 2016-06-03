package com.example.photoswalldemo.okhttp.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * Created by Administrator on 5/17 0017.
 */
public interface RestService {

    //文件下载
    @Streaming
    @GET("{filename}")
    Call<ResponseBody> downBitmaps(@Path("filename") String fileName);

}
