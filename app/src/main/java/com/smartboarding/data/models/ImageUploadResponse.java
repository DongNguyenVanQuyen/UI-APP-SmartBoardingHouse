// src/main/java/com/smartboarding/data/models/ImageUploadResponse.java
package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class ImageUploadResponse {
    @SerializedName("imageUrl") public String imageUrl;
    @SerializedName("publicId") public String publicId;
}