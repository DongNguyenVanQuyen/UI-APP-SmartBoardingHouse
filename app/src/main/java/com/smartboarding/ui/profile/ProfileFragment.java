package com.smartboarding.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.smartboarding.R;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.RoomData;
import com.smartboarding.data.models.Tenant;
import com.smartboarding.databinding.FragmentProfileBinding;
import com.smartboarding.ui.auth.LoginActivity;
import com.smartboarding.ui.contract.ContractActivity;
import com.smartboarding.ui.message.ChatActivity;
import com.smartboarding.ui.statistics.StatisticsActivity;
import com.smartboarding.utils.FormatUtils;
import com.smartboarding.viewmodel.AuthViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private AuthViewModel authViewModel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        loadProfile();
        loadRoom();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        binding.btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatActivity.class)));

        binding.btnContract.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ContractActivity.class)));

        binding.btnStatistics.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StatisticsActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            authViewModel.logout();
            authViewModel.logoutResult.observe(getViewLifecycleOwner(), done -> {
                if (done) {
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finishAffinity();
                }
            });
        });
    }

    private void loadProfile() {
        RetrofitClient.getInstance(requireContext()).getApi()
                .getProfile()
                .enqueue(new Callback<ApiResponse<Tenant>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Tenant>> call, Response<ApiResponse<Tenant>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Tenant t = response.body().getData();
                            binding.tvName.setText(t.fullName);
                            binding.tvRole.setText("Người thuê");
                            binding.tvEmail.setText(t.email);
                            binding.tvPhone.setText(t.phone != null ? t.phone : "--");
                            String avatarUrl = t.avatar;
                            if (avatarUrl == null || avatarUrl.isEmpty()) {
                                String encodedName = android.net.Uri.encode(t.fullName != null ? t.fullName : "U");
                                avatarUrl = "https://ui-avatars.com/api/?name=" + encodedName + "&background=random&color=fff&size=128&rounded=true";
                            }
                            Glide.with(requireContext()).load(avatarUrl)
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .circleCrop().into(binding.ivAvatar);
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Tenant>> call, Throwable t) {}
                });
    }

    private void loadRoom() {
        RetrofitClient.getInstance(requireContext()).getApi()
                .getCurrentRoom()
                .enqueue(new Callback<ApiResponse<RoomData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<RoomData>> call, Response<ApiResponse<RoomData>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            RoomData data = response.body().getData();
                            if (data.room != null) {
                                binding.tvRoomNumber.setText(data.room.roomNumber);
                                binding.tvRoomPrice.setText(FormatUtils.formatCurrency(data.room.price) + "/tháng");
                            }
                            if (data.contract != null) {
                                binding.tvStartDate.setText(FormatUtils.formatDate(data.contract.startDate));
                                binding.tvEndDate.setText(FormatUtils.formatDate(data.contract.endDate));
                            }
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<RoomData>> call, Throwable t) {}
                });
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}