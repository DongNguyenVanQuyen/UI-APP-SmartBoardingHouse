package com.smartboarding.data.api;

import com.smartboarding.data.models.*;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ─── AUTH ───────────────────────────────────────────
    @POST("auth/register")
    Call<ApiResponse<AuthData>> register(@Body Map<String, String> body);

    @POST("auth/login")
    Call<ApiResponse<AuthData>> login(@Body Map<String, String> body);

    @POST("auth/refresh-token")
    Call<ApiResponse<TokenData>> refreshToken(@Body Map<String, String> body);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    @PUT("auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body Map<String, String> body);

    @POST("auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<ApiResponse<Void>> verifyOtpAndResetPassword(@Body Map<String, String> body);


    // ─── PROFILE ────────────────────────────────────────
    @GET("profile")
    Call<ApiResponse<Tenant>> getProfile();

    @PUT("profile")
    Call<ApiResponse<Tenant>> updateProfile(@Body Map<String, String> body);

    @Multipart
    @POST("profile/avatar")
    Call<ApiResponse<AvatarData>> updateAvatar(@Part MultipartBody.Part avatar);

    @Multipart
    @POST("profile/identity-card")
    Call<ApiResponse<AvatarData>> uploadIdentityCard(
            @Part MultipartBody.Part frontImage,
            @Part MultipartBody.Part backImage
    );
    // ─── ROOM ───────────────────────────────────────────
    @GET("rooms/current")
    Call<ApiResponse<RoomData>> getCurrentRoom();

    // ─── CONTRACT ───────────────────────────────────────
    @GET("contracts")
    Call<ApiResponse<List<Contract>>> getContracts(
            @Query("page") Integer page,
            @Query("limit") Integer limit);

    @GET("contracts/{id}")
    Call<ApiResponse<Contract>> getContractById(@Path("id") String id);

    // ─── INVOICE ────────────────────────────────────────
    @GET("invoices")
    Call<ApiResponse<List<Invoice>>> getInvoices();

    @GET("invoices")
    Call<ApiResponse<List<Invoice>>> getInvoicesByStatus(@Query("status") String status);

    // Lọc hóa đơn theo hợp đồng (contract) và/hoặc loại hóa đơn (rent/deposit).
    // Truyền null cho tham số nào không cần lọc.
    @GET("invoices")
    Call<ApiResponse<List<Invoice>>> getInvoicesFiltered(
            @Query("contract") String contractId,
            @Query("type") String type,
            @Query("page") Integer page,
            @Query("limit") Integer limit);

    @GET("invoices/{id}")
    Call<ApiResponse<Invoice>> getInvoiceById(@Path("id") String id);

    // Danh sách phòng CHỈ theo hợp đồng còn hiệu lực (active) — dùng cho bộ
    // lọc/chuyển phòng ở màn hóa đơn. Không dùng getContracts() nữa vì
    // endpoint đó trả về TẤT CẢ hợp đồng (kể cả đã hủy/hết hạn), gây trùng
    // phòng trên bộ lọc.
    @GET("invoices/rooms")
    Call<ApiResponse<List<RoomOption>>> getInvoiceRooms();

    // Chuyển phòng đang chọn ngay tại màn hóa đơn — hợp đồng của phòng muốn
    // chuyển tới bắt buộc phải còn hiệu lực. Trả về danh sách hóa đơn của
    // phòng vừa chuyển.
    @PATCH("invoices/select-room")
    Call<ApiResponse<List<Invoice>>> selectInvoiceRoom(@Body Map<String, String> body);

    // ─── DEBT ───────────────────────────────────────────
    @GET("debts")
    Call<ApiResponse<DebtData>> getDebts();

    // ─── PAYMENT ────────────────────────────────────────
    @POST("payments/create-session")
    Call<ApiResponse<PaymentSessionData>> createPaymentSession(@Body Map<String, Object> body);

    @GET("payments/status/{token}")
    Call<ApiResponse<PaymentResult>> getPaymentStatus(@Path("token") String token);

    @GET("payments/history")
    Call<ApiResponse<PaymentHistoryData>> getPaymentHistory();

    // Route /pay/:token/confirm là route PUBLIC, được mount ở ROOT (ngoài /api),
    // KHÔNG được nằm dưới baseUrl "/api/". Dấu "/" ở đầu path bắt buộc phải có,
    // để OkHttp resolve về root domain thay vì nối vào "/api/" của baseUrl.
    // Thiếu dấu "/" này chính là nguyên nhân gây lỗi 404 trước đó.
    @POST("/pay/{token}/confirm")
    Call<ApiResponse<PaymentResult>> confirmPayment(@Path("token") String token);

    @Multipart
    @POST("/pay/{token}/confirm")
    Call<ApiResponse<PaymentResult>> confirmPaymentWithReceipt(
            @Path("token") String token,
            @Part MultipartBody.Part receiptImage
    );
    // ─── METER READING ──────────────────────────────────

    // Danh sách phòng (theo hợp đồng active) để chọn khi ghi chỉ số —
    // dùng cho màn hình chọn phòng khi tenant có nhiều hợp đồng cùng lúc.
    @GET("meter-readings/rooms")
    Call<ApiResponse<List<MeterRoomOption>>> getMeterRooms();

    // contractId có thể null nếu tenant chỉ có đúng 1 hợp đồng đang active
    // (server tự chọn hợp đồng duy nhất đó); bắt buộc truyền nếu có nhiều hợp đồng.
    @GET("meter-readings/previous")
    Call<ApiResponse<MeterReadingPrevious>> getPreviousReading(
            @Query("type") String type,
            @Query("contract") String contractId
    );

    @Multipart
    @POST("meter-readings/scan")
    Call<ApiResponse<MeterScanResult>> scanMeterImage(
            @Part("type") RequestBody type,
            @Part("contract") RequestBody contractId,
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("meter-readings")
    Call<ApiResponse<MeterReading>> createMeterReadingWithUrl(
            @Part("type") RequestBody type,
            @Part("currentReading") RequestBody currentReading,
            @Part("imageUrl") RequestBody imageUrl,
            @Part("ocrRawText") RequestBody ocrRawText,
            @Part("contract") RequestBody contractId
    );

    @Multipart
    @POST("meter-readings")
    Call<ApiResponse<MeterReading>> createMeterReading(
            @Part("type") RequestBody type,
            @Part("currentReading") RequestBody currentReading,
            @Part("contract") RequestBody contractId,
            @Part MultipartBody.Part image
    );

    @Multipart
    @PATCH("meter-readings/{id}")
    Call<ApiResponse<MeterReading>> updateMeterReading(
            @Path("id") String id,
            @Part("currentReading") RequestBody currentReading,
            @Part("imageUrl") RequestBody imageUrl,
            @Part MultipartBody.Part image
    );

    @GET("meter-readings/history")
    Call<ApiResponse<List<MeterReading>>> getMeterReadingHistory();

    @GET("meter-readings/history")
    Call<ApiResponse<List<MeterReading>>> getMeterReadingByType(
            @Query("type") String type
    );

    // ─── MAINTENANCE ────────────────────────────────────
    @Multipart
    @POST("maintenance-requests")
    Call<ApiResponse<MaintenanceRequest>> createMaintenanceRequest(
            @Part("title") RequestBody title,
            @Part("description") RequestBody description,
            @Part("priority") RequestBody priority,
            @Part("category") RequestBody category,
            @Part("contract") RequestBody contractId,
            @Part List<MultipartBody.Part> images
    );

    @GET("maintenance-requests")
    Call<ApiResponse<List<MaintenanceRequest>>> getMaintenanceRequests(
            @Query("page") Integer page,
            @Query("limit") Integer limit);

    @GET("maintenance-requests/{id}")
    Call<ApiResponse<MaintenanceRequest>> getMaintenanceById(@Path("id") String id);

    // ─── NOTIFICATION ───────────────────────────────────
    @GET("notifications")
    Call<ApiResponse<NotificationData>> getNotifications(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @PUT("notifications/read")
    Call<ApiResponse<Void>> markNotificationsRead(@Body Map<String, Object> body);

    @PUT("notifications/fcm-token")
    Call<ApiResponse<Void>> updateFcmToken(@Body Map<String, String> body);

    // ─── MESSAGES (CHAT) ────────────────────────────────
    @GET("messages/me")
    Call<ApiResponse<List<Message>>> getMyMessages(
            @Query("page") int page,
            @Query("limit") int limit
    );

    // Upload ảnh chat lên Cloudinary, trả về imageUrl để gửi tiếp qua socket
    @Multipart
    @POST("messages/upload-image")
    Call<ApiResponse<ImageUploadResponse>> uploadChatImage(
            @Part MultipartBody.Part image
    );

    // ─── DASHBOARD ──────────────────────────────────────
    @GET("dashboard")
    Call<ApiResponse<DashboardData>> getDashboard();

    // Chuyển phòng đang chọn (dựa vào hợp đồng, hợp đồng phải còn hiệu lực).
    // Phòng vừa chuyển sẽ được lưu làm phòng hiện tại, dùng làm mặc định cho
    // chụp công tơ và cho màn Hóa đơn. Trả về dashboard mới nhất theo phòng
    // vừa chuyển.
    @PATCH("dashboard/select-room")
    Call<ApiResponse<DashboardData>> selectDashboardRoom(@Body Map<String, String> body);

    // ─── STATISTICS ─────────────────────────────────────
    @GET("statistics/monthly")
    Call<ApiResponse<MonthlyStats>> getMonthlyStats(
            @Query("year") int year,
            @Query("month") int month
    );

    @GET("statistics/yearly")
    Call<ApiResponse<YearlyStats>> getYearlyStats(@Query("year") int year);

    // ─── REPORT ─────────────────────────────────────────
    @GET("reports/monthly")
    Call<ApiResponse<MonthlyReport>> getMonthlyReport(
            @Query("year") int year,
            @Query("month") int month
    );

    // ─── DEBUG / TEST ──
    @POST("admin/test/advance-month")
    Call<ApiResponse<Invoice>> debugAdvanceMonth();

    @POST("debug/clear-month")
    Call<ApiResponse<Object>> clearMonthData(@Body Map<String, Object> body);
}