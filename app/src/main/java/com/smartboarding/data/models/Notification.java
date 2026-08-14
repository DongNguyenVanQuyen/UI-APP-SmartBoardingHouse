package com.smartboarding.data.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")       public String id;
    @SerializedName("title")     public String title;
    @SerializedName("body")      public String body;
    @SerializedName("type")      public String type;
    @SerializedName("refId")     public String refId;
    @SerializedName("refModel")  public String refModel;
    @SerializedName("isRead")    public boolean isRead;
    @SerializedName("createdAt") public String createdAt;
    // Dùng JsonElement thay vì JsonObject: JsonObject không tự xử lý được giá trị JSON null
    // (Gson ném JsonSyntaxException "Expected a JsonObject but was JsonNull"), còn JsonElement thì an toàn.
    @SerializedName("meta")      public JsonElement meta; // dữ liệu phụ, cấu trúc thay đổi theo từng loại thông báo

    // meta chỉ thực sự "có dữ liệu" khi nó tồn tại, không phải null, và là object
    public boolean hasMeta() {
        return meta != null && !meta.isJsonNull() && meta.isJsonObject();
    }
}