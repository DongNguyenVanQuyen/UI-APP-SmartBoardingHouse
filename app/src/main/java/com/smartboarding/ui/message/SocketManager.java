package com.smartboarding.ui.message;

import android.util.Log;

import com.google.gson.Gson;
import com.smartboarding.BuildConfig;
import com.smartboarding.data.models.Message;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = BuildConfig.BASE_URL.replaceAll("/api/?$", "");

    private static SocketManager instance;
    private Socket socket;
    private final Gson gson = new Gson();
    private String currentToken;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect(String jwtToken) {
        if (socket != null && socket.connected() && jwtToken != null && jwtToken.equals(currentToken)) {
            return;
        }
        disconnect();
        currentToken = jwtToken;
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 2000;

            java.util.Map<String, String> authData = new java.util.HashMap<>();
            authData.put("token", jwtToken);
            options.auth = authData;

            socket = IO.socket(SOCKET_URL, options);
            socket.connect();

            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Socket connected"));
            socket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "Socket disconnected"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                if (args.length > 0) Log.e(TAG, "Connect error: " + args[0]);
            });

        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket URI error: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    // ----- Gửi tin nhắn -----
    public interface SendCallback {
        void onResult(boolean success, String message, Message data);
    }

    /**
     * Gửi tin nhắn văn bản.
     */
    public void sendMessage(String content, SendCallback callback) {
        sendMessageInternal(content, "Text", null, callback);
    }

    /**
     * Gửi tin nhắn ảnh. imageUrl phải là URL Cloudinary đã upload xong
     * (xem ApiService#uploadChatImage) — socket chỉ truyền URL, không truyền file.
     */
    public void sendImageMessage(String imageUrl, SendCallback callback) {
        sendMessageInternal("[Hình ảnh]", "Image", imageUrl, callback);
    }

    private void sendMessageInternal(String content, String type, String imageUrl, SendCallback callback) {
        if (socket == null || !socket.connected()) {
            callback.onResult(false, "Chưa kết nối tới server", null);
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("content", content);
            payload.put("type", type);
            if (imageUrl != null) payload.put("imageUrl", imageUrl);

            socket.emit("send_message", payload, (Ack) args -> {
                try {
                    JSONObject response = (JSONObject) args[0];
                    boolean success = response.getBoolean("success");
                    if (success) {
                        Message msg = gson.fromJson(response.getJSONObject("data").toString(), Message.class);
                        callback.onResult(true, null, msg);
                    } else {
                        callback.onResult(false, response.optString("message", "Lỗi gửi tin nhắn"), null);
                    }
                } catch (Exception e) {
                    callback.onResult(false, e.getMessage(), null);
                }
            });
        } catch (Exception e) {
            callback.onResult(false, e.getMessage(), null);
        }
    }

    public void markRead() {
        if (socket != null && socket.connected()) {
            socket.emit("mark_read", new JSONObject());
        }
    }

    public void sendTyping(boolean isTyping) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("isTyping", isTyping);
                socket.emit("typing", payload);
            } catch (Exception ignored) {
            }
        }
    }

    // ----- Lắng nghe tin nhắn mới -----
    public void onNewMessage(Emitter.Listener listener) {
        if (socket != null) socket.on("new_message", listener);
    }

    public void offNewMessage(Emitter.Listener listener) {
        if (socket != null) socket.off("new_message", listener);
    }

    public void onMessagesRead(Emitter.Listener listener) {
        if (socket != null) socket.on("messages_read", listener);
    }

    public void offMessagesRead(Emitter.Listener listener) {
        if (socket != null) socket.off("messages_read", listener);
    }

    public void onTyping(Emitter.Listener listener) {
        if (socket != null) socket.on("typing", listener);
    }

    public void offTyping(Emitter.Listener listener) {
        if (socket != null) socket.off("typing", listener);
    }

    // Interface hỗ trợ ack callback (socket.io-client Java dùng Ack)
    private interface Ack extends io.socket.client.Ack {}
}