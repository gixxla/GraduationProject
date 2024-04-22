package com.example.graduationproject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DisasterMsgService {

    @GET("getDisasterMsg2List")
    Call<DisasterMsgResponse> getDisasterMsg(
            @Query("serviceKey") String serviceKey,
            @Query("pageNo") int pageNo,
            @Query("numOfRows") int numOfRows,
            @Query("type") String type,
            @Query("create_date") String createDate,
            @Query("location_name") String locationName
    );
}
