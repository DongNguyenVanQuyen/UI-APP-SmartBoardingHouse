package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class AvatarData {
    @SerializedName("avatar") public String avatar;
    @SerializedName("frontImage")
    public String frontImage;

    @SerializedName("backImage")
    public String backImage;
}