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

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.ivAvatar.setOnClickListener(v -> pickImage.launch("image/*"));
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
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Tenant>> call, Throwable t) {}
                });
    }

    private void saveProfile() {
        Map<String, String> body = new HashMap<>();
        body.put("fullName", binding.etFullName.getText().toString().trim());
        body.put("phone",    binding.etPhone.getText().toString().trim());
        body.put("idCard",   binding.etIdCard.getText().toString().trim());
        body.put("address",  binding.etAddress.getText().toString().trim());

        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().updateProfile(body)
                .enqueue(new Callback<ApiResponse<Tenant>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Tenant>> call, Response<ApiResponse<Tenant>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Tenant>> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadAvatar(Uri uri) {
        try {
            File file = new File(uri.getPath());
            RequestBody rb = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", file.getName(), rb);

            RetrofitClient.getInstance(this).getApi().updateAvatar(part)
                    .enqueue(new Callback<ApiResponse<AvatarData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<AvatarData>> call, Response<ApiResponse<AvatarData>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                String url = response.body().getData().avatar;
                                Glide.with(EditProfileActivity.this).load(url).circleCrop().into(binding.ivAvatar);
                                Toast.makeText(EditProfileActivity.this, "Cập nhật ảnh thành công", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<AvatarData>> call, Throwable t) {}
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
        }
    }
}