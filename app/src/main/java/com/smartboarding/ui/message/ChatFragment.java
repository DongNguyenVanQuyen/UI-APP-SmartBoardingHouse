package com.smartboarding.ui.message;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.smartboarding.R;
import com.smartboarding.data.api.ApiService;
import com.smartboarding.data.api.RetrofitClient;
import com.smartboarding.data.models.ApiResponse;
import com.smartboarding.data.models.ImageUploadResponse;
import com.smartboarding.data.models.Message;
import com.smartboarding.utils.SessionManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etContent;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private TextView tvTyping;

    private ChatAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 30;
    private boolean isLoadingMore = false;
    private boolean hasMoreMessages = true;

    private ApiService apiService;
    private String token;
    private String currentUserId; // id của Tenant đang đăng nhập, dùng để xác định "isMine"

    private ActivityResultLauncher<String> pickImageLauncher;

    private final Emitter.Listener onNewMessageListener = args -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Message message = new Gson().fromJson(data.toString(), Message.class);
                adapter.addMessage(message);
                rvMessages.scrollToPosition(adapter.getItemCount() - 1);

                if (!message.isMine(currentUserId)) {
                    SocketManager.getInstance().markRead();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    };

    private final Emitter.Listener onTypingListener = args -> {
        if (getActivity() == null || args.length == 0) return;
        getActivity().runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                boolean isTyping = data.optBoolean("isTyping", false);
                if (tvTyping != null) {
                    tvTyping.setVisibility(isTyping ? View.VISIBLE : View.GONE);
                }
            } catch (Exception ignored) {
            }
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMessages = view.findViewById(R.id.rvMessages);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        etContent = view.findViewById(R.id.etContent);
        btnSend = view.findViewById(R.id.btnSend);
        btnAttach = view.findViewById(R.id.btnAttach);
        tvTyping = view.findViewById(R.id.tvTyping);

        SessionManager session = SessionManager.getInstance(requireContext());
        token = session.getAccessToken();
        currentUserId = session.getUserId();
        apiService = RetrofitClient.getInstance(requireContext()).getApi();

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvMessages.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(messageList, currentUserId);
        rvMessages.setAdapter(adapter);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );

        loadMessages();

        swipeRefresh.setOnRefreshListener(this::loadMessages);

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // Cuộn lên gần đầu danh sách (còn tin cũ hơn) -> tải thêm trang trước đó
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                if (dy < 0 && !isLoadingMore && hasMoreMessages && firstVisible <= 2) {
                    loadMoreMessages(layoutManager);
                }
            }
        });

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnAttach.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        etContent.addTextChangedListener(new SimpleTextWatcher(text ->
                SocketManager.getInstance().sendTyping(!text.trim().isEmpty())));

        connectSocket();
    }

    private void connectSocket() {
        SocketManager.getInstance().connect(token);
        SocketManager.getInstance().onNewMessage(onNewMessageListener);
        SocketManager.getInstance().onTyping(onTypingListener);
        SocketManager.getInstance().markRead();
    }

    private void loadMessages() {
        currentPage = 1;
        apiService.getMyMessages(currentPage, PAGE_SIZE)
                .enqueue(new Callback<ApiResponse<List<Message>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Message>>> call,
                                           @NonNull Response<ApiResponse<List<Message>>> response) {
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Message> data = response.body().getData();
                            adapter.setMessages(data);
                            hasMoreMessages = data.size() == PAGE_SIZE;
                            if (!data.isEmpty()) {
                                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Message>>> call, @NonNull Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Không tải được tin nhắn", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMoreMessages(LinearLayoutManager layoutManager) {
        isLoadingMore = true;
        int nextPage = currentPage + 1;

        // Ghi lại item đầu tiên đang hiển thị để giữ vị trí cuộn sau khi chèn tin cũ vào đầu danh sách
        int anchorPosBefore = layoutManager.findFirstVisibleItemPosition();
        View anchorView = layoutManager.findViewByPosition(anchorPosBefore);
        int anchorOffsetBefore = anchorView != null ? anchorView.getTop() : 0;

        apiService.getMyMessages(nextPage, PAGE_SIZE)
                .enqueue(new Callback<ApiResponse<List<Message>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Message>>> call,
                                           @NonNull Response<ApiResponse<List<Message>>> response) {
                        isLoadingMore = false;
                        if (response.isSuccessful() && response.body() != null) {
                            List<Message> data = response.body().getData();
                            if (!data.isEmpty()) {
                                currentPage = nextPage;
                                adapter.prependMessages(data);
                                int newAnchorPos = anchorPosBefore + data.size();
                                layoutManager.scrollToPositionWithOffset(newAnchorPos, anchorOffsetBefore);
                            }
                            hasMoreMessages = data.size() == PAGE_SIZE;
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Message>>> call, @NonNull Throwable t) {
                        isLoadingMore = false;
                    }
                });
    }

    private void sendTextMessage() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) return;

        btnSend.setEnabled(false);
        SocketManager.getInstance().sendMessage(content, (success, message, data) -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                btnSend.setEnabled(true);
                if (success) {
                    etContent.setText("");
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) return;
        uploadAndSendImage(uri);
    }

    /**
     * Upload ảnh lên Cloudinary qua REST endpoint POST /messages/upload-image,
     * sau đó gửi tin nhắn ảnh qua socket bằng imageUrl trả về.
     */
    private void uploadAndSendImage(Uri uri) {
        btnAttach.setEnabled(false);
        Toast.makeText(requireContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        try {
            File file = copyUriToCacheFile(uri);
            RequestBody requestBody = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("image", file.getName(), requestBody);

            apiService.uploadChatImage(part).enqueue(new Callback<ApiResponse<ImageUploadResponse>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<ImageUploadResponse>> call,
                                       @NonNull Response<ApiResponse<ImageUploadResponse>> response) {
                    btnAttach.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        String imageUrl = response.body().getData().imageUrl;
                        SocketManager.getInstance().sendImageMessage(imageUrl, (success, message, data) -> {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!success) {
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    } else {
                        Toast.makeText(requireContext(), "Tải ảnh lên thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<ImageUploadResponse>> call, @NonNull Throwable t) {
                    btnAttach.setEnabled(true);
                    Toast.makeText(requireContext(), "Không thể tải ảnh lên", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            btnAttach.setEnabled(true);
            Toast.makeText(requireContext(), "Không đọc được ảnh đã chọn", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToCacheFile(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        File outFile = new File(requireContext().getCacheDir(), "chat_upload_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while (inputStream != null && (read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        if (inputStream != null) inputStream.close();
        return outFile;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SocketManager.getInstance().offNewMessage(onNewMessageListener);
        SocketManager.getInstance().offTyping(onTypingListener);
    }

    /** TextWatcher gọn cho typing indicator, tránh phải cài đủ 3 method rỗng ở nơi gọi. */
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        interface OnTextChanged { void onChanged(String text); }
        private final OnTextChanged callback;
        SimpleTextWatcher(OnTextChanged callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            callback.onChanged(s.toString());
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}