package com.smartboarding.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.smartboarding.R;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.*;
import com.smartboarding.databinding.ActivityEditProfileBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;

    private Uri frontImageUri = null;
    private Uri backImageUri = null;

    private final ActivityResultLauncher<String> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    private final ActivityResultLauncher<String> pickFrontImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    frontImageUri = uri;
                    Glide.with(this).load(uri).into(binding.ivFrontImage);
                }
            });

    private final ActivityResultLauncher<String> pickBackImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    backImageUri = uri;
                    Glide.with(this).load(uri).into(binding.ivBackImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.ivAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));
        binding.ivFrontImage.setOnClickListener(v -> pickFrontImage.launch("image/*"));
        binding.ivBackImage.setOnClickListener(v -> pickBackImage.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        RetrofitClient.getInstance(this).getApi().getProfile()
                .enqueue(new Callback<ApiResponse<Tenant>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Tenant>> call, Response<ApiResponse<Tenant>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Tenant t = response.body().getData();
                            binding.etFullName.setText(t.fullName);
                            binding.etPhone.setText(t.phone);
                            binding.etIdCard.setText(t.idCard);
                            binding.etAddress.setText(t.address);

                            if (t.avatar != null)
                                Glide.with(EditProfileActivity.this).load(t.avatar)
                                        .placeholder(R.drawable.ic_avatar_placeholder)
                                        .circleCrop().into(binding.ivAvatar);

                            if (t.frontImage != null)
                                Glide.with(EditProfileActivity.this).load(t.frontImage)
                                        .placeholder(R.drawable.ic_image_placeholder)
                                        .into(binding.ivFrontImage);

                            if (t.backImage != null)
                                Glide.with(EditProfileActivity.this).load(t.backImage)
                                        .placeholder(R.drawable.ic_image_placeholder)
                                        .into(binding.ivBackImage);
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Tenant>> call, Throwable t) {}
                });
    }

    private void saveProfile() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        // Bước 1: Lưu thông tin Text trước
        Map<String, String> body = new HashMap<>();
        body.put("fullName", binding.etFullName.getText().toString().trim());
        body.put("phone",    binding.etPhone.getText().toString().trim());
        body.put("idCard",   binding.etIdCard.getText().toString().trim());
        body.put("address",  binding.etAddress.getText().toString().trim());

        RetrofitClient.getInstance(this).getApi().updateProfile(body)
                .enqueue(new Callback<ApiResponse<Tenant>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Tenant>> call, Response<ApiResponse<Tenant>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // Bước 2: Kiểm tra xem có cần upload ảnh CMND không
                            if (frontImageUri != null || backImageUri != null) {
                                uploadIdentityCards();
                            } else {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.btnSave.setEnabled(true);
                                Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnSave.setEnabled(true);
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Tenant>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadIdentityCards() {
        MultipartBody.Part frontPart = null;
        MultipartBody.Part backPart = null;

        File f1 = null, f2 = null;

        if (frontImageUri != null) {
            f1 = copyUriToTempFile(frontImageUri, "front");
            if (f1 != null) {
                RequestBody rb = RequestBody.create(f1, MediaType.parse("image/*"));
                frontPart = MultipartBody.Part.createFormData("frontImage", f1.getName(), rb);
            }
        }

        if (backImageUri != null) {
            f2 = copyUriToTempFile(backImageUri, "back");
            if (f2 != null) {
                RequestBody rb = RequestBody.create(f2, MediaType.parse("image/*"));
                backPart = MultipartBody.Part.createFormData("backImage", f2.getName(), rb);
            }
        }

        // Tạo tham số tạm thời cho biến Part bị null để Retrofit không báo lỗi
        if (frontPart == null) {
            frontPart = MultipartBody.Part.createFormData("emptyFront", "");
        }
        if (backPart == null) {
            backPart = MultipartBody.Part.createFormData("emptyBack", "");
        }

        File finalF1 = f1;
        File finalF2 = f2;
        RetrofitClient.getInstance(this).getApi().uploadIdentityCard(frontPart, backPart)
                .enqueue(new Callback<ApiResponse<AvatarData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AvatarData>> call, Response<ApiResponse<AvatarData>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);

                        if (finalF1 != null) finalF1.delete();
                        if (finalF2 != null) finalF2.delete();

                        if (response.isSuccessful()) {
                            Toast.makeText(EditProfileActivity.this, "Đã lưu toàn bộ thông tin và ảnh", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Lưu thông tin thành công nhưng lỗi tải ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<AvatarData>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        if (finalF1 != null) finalF1.delete();
                        if (finalF2 != null) finalF2.delete();
                        Toast.makeText(EditProfileActivity.this, "Lưu thông tin thành công, lỗi mạng khi tải ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadAvatar(Uri uri) {
        try {
            File file = copyUriToTempFile(uri, "avatar");
            if (file == null) return;

            RequestBody rb = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", file.getName(), rb);

            binding.progressBar.setVisibility(View.VISIBLE);
            RetrofitClient.getInstance(this).getApi().updateAvatar(part)
                    .enqueue(new Callback<ApiResponse<AvatarData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<AvatarData>> call, Response<ApiResponse<AvatarData>> response) {
                            binding.progressBar.setVisibility(View.GONE);
                            file.delete();
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                String url = response.body().getData().avatar;
                                Glide.with(EditProfileActivity.this).load(url).circleCrop().into(binding.ivAvatar);
                                Toast.makeText(EditProfileActivity.this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<AvatarData>> call, Throwable t) {
                            binding.progressBar.setVisibility(View.GONE);
                            file.delete();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm tiện ích copy URI thành File ảo để gửi HTTP
    private File copyUriToTempFile(Uri uri, String prefix) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File outFile = new File(getCacheDir(), prefix + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}