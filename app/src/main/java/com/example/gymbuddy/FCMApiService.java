package com.example.gymbuddy;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FCMApiService {
    public static final String TAG = "FCMApiService";

    //Create a backend url
    private static final String BASE_URL = "http://10.0.2.2:3000";


    private static FCMApiService instance;
    private final OkHttpClient client;
    private String currentUserToken;


    //Create client
    private FCMApiService(){
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

    }

    public static synchronized FCMApiService getInstance(){
        if(instance == null){
            instance = new FCMApiService();
        }
        return instance;
    }

    //Store device token
    public void setCurrentUserToken(String token){
        this.currentUserToken = token;
    }

    /*Register device token with created backend (Server.js)
    public void registerDevice(String userId, String fcmToken, ApiCallBack callBack){
        try{
            JSONObject object = new JSONObject();
            object.put("userId", userId);
            object.put("fcmToken", fcmToken);
            object.put("deviceType", "android");

            RequestBody body = RequestBody.create(
                    object.toString(),
                    MediaType.parse("application/json;charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/register")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG,"Failed to register device");
                    e.printStackTrace();
                    if(callBack!=null){
                        callBack.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        Log.d(TAG, "Successfully registered device" + response.code());
                        if (callBack != null) {
                            callBack.onSuccess(response.body().string());
                        }
                    }else {
                        Log.e(TAG, "Failed to register device" + response.code());

                        if (callBack != null) {
                            callBack.onFailure(response.message());
                        }
                    }
                }
            });



        }catch(JSONException e){
            Log.e(TAG,"JSON error" + e);
            e.printStackTrace();
            if(callBack!=null){
                callBack.onFailure(e.getMessage());
            }
        }
    }

     */

    //Register token with firestore
    public void registerDeviceWithFirestore(String userId, String fcmToken, ApiCallBack callBack){
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("userId", userId);
            tokenData.put("fcmToken", fcmToken);
            tokenData.put("deviceType", "android");

            db.collection("users")
                    .document(userId)
                    .update(tokenData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore Registration", "Token has successfully been added to firestore");
                        if (callBack != null) {
                            callBack.onSuccess("Token has successfully been added to firestore");
                        }
                    })
                    .addOnFailureListener(e->{
                        Log.e("Firestore Registration", "Failed to save token to Firestore");
                        if (callBack != null) {
                            callBack.onFailure(e.getMessage());
                        }
                    });
        }catch (Exception e){
            Log.e("Firestore Registration", "Failed to save token to Firestore");
        }
    }

    public void sendMessage(String targetUser, String title, String message,  ApiCallBack callBack) {
        try{
            //Create JSON file; how message will be sent to backend
            JSONObject object = new JSONObject();
            object.put("targetUser", targetUser);
            object.put("title", title);
            object.put("message", message);
            object.put("sender", currentUserToken);

            Log.d(TAG, "Message JSON: " + object.toString());

            RequestBody body = RequestBody.create(
                    object.toString(),
                    MediaType.parse("application/json;charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/send")
                    .post(body)
                    .build();

            Log.d(TAG, "Sending message request to: " + request.url());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Failed to send message");
                    if(callBack!= null){
                        callBack.onFailure("Why message failed: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    if(response.isSuccessful()){
                        Log.d(TAG, "Successfully sent message. Code: " + response.code() + ", Body: " + responseBody);
                        if(callBack != null){
                            callBack.onSuccess(responseBody);
                        }
                    } else {
                        Log.e(TAG, "Failed to send message. Code: " + response.code() + ", Body: " + responseBody);
                        if(callBack != null){
                            callBack.onFailure("Send failed: " + response.message() + " - " + responseBody);
                        }
                    }
                }
            });


        }catch(JSONException e){
            Log.e(TAG, "Failed to load JSON");
            if(callBack!= null){
                callBack.onFailure(e.getMessage());
            }
        }
    }

    /*Get list registered users
    public void getRegisteredUsers(ApiCallBack callBack) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/users")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG,"Failed to get users",e);
                if (callBack!=null){
                    callBack.onFailure(e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    Log.d(TAG,"Successfully get users" + responseData);
                    if(callBack!=null) {
                        callBack.onSuccess(responseData);
                    }else{
                        if(callBack!=null){
                            callBack.onFailure("Failed to get users: " + response.message());
                        }
                    }
                }
            }
        });
    }

     */

    public interface ApiCallBack {
        void onSuccess(String response);

        void onFailure(String error);
    }


}
