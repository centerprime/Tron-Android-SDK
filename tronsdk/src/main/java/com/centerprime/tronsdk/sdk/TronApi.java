package com.centerprime.tronsdk.sdk;

import java.util.HashMap;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by CenterPrime on 2020/12/14.
 */
public interface TronApi {

    @GET("/api/account")
    Single<HashMap<String, Object>> getTrx20Balance(@Query("address") String address,
                                                    @Query("tokenId") String tokenId);

    @GET("/api/account")
    Single<HashMap<String, Object>> getTrxBalance(@Query("address") String address);

}
