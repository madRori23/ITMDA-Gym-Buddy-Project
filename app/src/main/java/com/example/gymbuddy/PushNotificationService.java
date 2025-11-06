package com.example.gymbuddy;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class PushNotificationService extends FirebaseMessagingService{

    /*Role:
    * Will reviece incomming push notifications
    * Uses notificaiton helper to display notifications

     */
    private static final String TAG = "FCMService";
    //Consider how i would save tokens for each device, for the desired app
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        //Take token and push to server and store token based on user account
        //Store token locally
        FCMApiService.getInstance().setCurrentUserToken(token);

        //Send token to backend (Server.js)
        //How to send to firebase db???
        sendTokenToFirestore(token);
    }


    //triggered when you recieve a message
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Log.d(TAG, "From: " + message.getFrom());

        //If message contains notificaiton data/paylaod
        if(message.getNotification()!=null){
            //get message title and payload
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();

            //Log the title and payload
            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + body);

            NotificationHelper.displayNotification(
                    this,
                    title!= null? title: "New Join Request",
                    body!= null?body:""
            );
        }

        //If message contains data payload
        if(!message.getData().isEmpty()){
            Log.d(TAG, "Message Data Payload: " + message.getData());

            //handle payload
            String title = message.getData().get("title");
            String body = message.getData().get("body");

            if(title!=null && body!=null) {
                NotificationHelper.displayNotification(
                        this,
                        title,
                        body
                );
            }
        }
    }

    //function will send my token to the Firestore
    public static void sendTokenToFirestore(String token){
        //User Data
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //Create DB instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Check if user is not null
        if(user!=null){
            Map<String, Object> newToken = new HashMap<>();
            newToken.put("userId", user.getUid());
            newToken.put("fcmToken", token);

            db.collection("users")
                    .document(user.getUid())
                    .update(newToken)
                    .addOnSuccessListener(aVoid->{
                        Log.d(TAG, "Token has successfully been updated");
                    }).addOnFailureListener(aVoid->{
                        Log.e(TAG,"Token has failed to update");
                    });
        }
    }
}
