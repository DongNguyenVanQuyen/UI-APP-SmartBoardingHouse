package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class TokenData {
    @SerializedName("accessToken")  public String accessToken;
    @SerializedName("refreshToken") public String refreshToken;
}