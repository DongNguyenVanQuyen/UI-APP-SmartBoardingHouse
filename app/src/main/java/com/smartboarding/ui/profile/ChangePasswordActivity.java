package com.smartboarding.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.smartboarding.databinding.ActivityChangePasswordBinding;
import com.smartboarding.viewmodel.AuthViewModel;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.btnBack.setOnClickListener(v -> finish());

        viewModel.loading.observe(this, l -> {
            binding.progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!l);
        });

        viewModel.errorMessage.observe(this, msg -> {
            if ("SUCCESS".equals(msg)) {
                Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                finish();
            } else if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            String current  = binding.etCurrentPassword.getText().toString().trim();
            String newPass  = binding.etNewPassword.getText().toString().trim();
            String confirm  = binding.etConfirmPassword.getText().toString().trim();

            if (current.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.changePassword(current, newPass);
        });
    }
}