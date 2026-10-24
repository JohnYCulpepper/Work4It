package com.johnculpepper.work4it;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;
import java.io.IOException;

public class NetworkManager {
    private final OkHttpClient client = new OkHttpClient();

    public interface RepCountCallback {
        void onResult(int repCount, String stage);
        void onError(String error);
    }

    public void makeGetRequest(String url) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("NetworkManager", "Reset: " + responseData);
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
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            // Access rep_count and stage from Python server
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        // Parses the python data to an Integer and String respectively
                        JSONObject json = new JSONObject(responseData);
                        int repCount = json.getInt("rep_count");
                        String stage = json.getString("stage");
                        callback.onResult(repCount, stage);
                    } catch (JSONException e) {
                        callback.onError("JSON parsing failed: " + e.getMessage());
                    }
                }
            }
        });
    }
}