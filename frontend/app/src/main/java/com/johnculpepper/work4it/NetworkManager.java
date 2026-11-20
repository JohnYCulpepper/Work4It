package com.johnculpepper.work4it;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;
import java.io.IOException;

import java.util.UUID;

public class NetworkManager {
    // One random session ID per app process. Static so every NetworkManager
    // instance (e.g. one per activity) sends the same ID and the server keeps
    // a single rep count for this user.
    private static final String SESSION_ID = UUID.randomUUID().toString();

    private final OkHttpClient client = new OkHttpClient();

    public interface RepCountCallback {
        void onResult(int repCount, String stage);
        void onError(String error);
    }

    // Used for /reset. Appends this user's session_id as a query parameter.
    public void makeGetRequest(String url) {
        new Thread(() -> {
            HttpUrl parsed;
            try {
                parsed = HttpUrl.get(url);
            } catch (IllegalArgumentException e) {
                Log.d("NetworkManager", "Reset failed, bad URL: " + e.getMessage());
                return;
            }

            HttpUrl urlWithSession = parsed.newBuilder()
                    .addQueryParameter("session_id", SESSION_ID)
                    .build();

            Request request = new Request.Builder()
                    .url(urlWithSession)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("NetworkManager", "Reset: " + responseData);
                } else {
                    Log.d("NetworkManager", "Reset failed: HTTP " + response.code());
                }
            } catch (IOException e) {
                Log.d("NetworkManager", "Reset failed: " + e.getMessage());
            }
        }).start();
    }

    public void sendFrame(String url, byte[] imageBytes, String exerciseType, RepCountCallback callback) {
        RequestBody fileBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("frame", "frame.jpg", fileBody)
                .addFormDataPart("exercise_type", exerciseType)
                .addFormDataPart("session_id", SESSION_ID)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            // Access rep_count and stage from Python server
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // try-with-resources makes sure the response body is always closed
                try (Response r = response) {
                    if (r.isSuccessful() && r.body() != null) {
                        String responseData = r.body().string();
                        try {
                            // Parses the python data to an Integer and String respectively
                            JSONObject json = new JSONObject(responseData);
                            int repCount = json.getInt("rep_count");
                            String stage = json.getString("stage");
                            callback.onResult(repCount, stage);
                        } catch (JSONException e) {
                            callback.onError("JSON parsing failed: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Server error: HTTP " + r.code());
                    }
                }
            }
        });
    }
}