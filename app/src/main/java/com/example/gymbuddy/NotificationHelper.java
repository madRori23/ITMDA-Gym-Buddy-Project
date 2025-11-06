package com.example.gymbuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    /*Role:
    * Create Notification
    * Manage notification channels
    * Display notifications
    * Cancel notifications
     */
    private static final String CHANNEL_ID = "Default_Channel";

    private static final String CHANNEL_NAME = "Default_Name";
    private static final String CHANNEL_DESCRIPTION = "Default_Description";
    private static final int NOTI_ID = 999;


    //Create channels for notifications
    public static void createChannel(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if(manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    //Display simple notification

    public static void displayNotification(Context context, String title, String body) {
        //Create the channel
        createChannel(context);

        //Create intents
        Intent intent = new Intent(context, LandingPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        //Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.rc_notification_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        //Show Notification

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null) {
            manager.notify(NOTI_ID, builder.build());
        }
    }

    //can display notificaiton with custom ID (Will implement later, based on user data for now will just show a simple notificaiton

    //Cancel all notificaiotn
    public static void cancelAllNotifications(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!= null){
            manager.cancelAll();
        }
    }
}
