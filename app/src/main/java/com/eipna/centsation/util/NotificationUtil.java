package com.eipna.centsation.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.eipna.centsation.R;
import com.eipna.centsation.data.Database;
import com.eipna.centsation.ui.activities.MainActivity;

import java.util.Objects;

public class NotificationUtil {

    public static String CHANNEL_DEADLINE_ID = "channel_deadline";
    public static String CHANNEL_DEADLINE_NAME = "Deadlines";
    public static int CHANNEL_DEADLINE_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel deadlineChannel = new NotificationChannel(CHANNEL_DEADLINE_ID, CHANNEL_DEADLINE_NAME, CHANNEL_DEADLINE_IMPORTANCE);
            notificationManager.createNotificationChannel(deadlineChannel);
        }
    }

    public static void create(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String savingName = intent.getStringExtra(Database.COLUMN_SAVING_NAME);
        int savingRequestCode = Objects.requireNonNull(intent.getStringExtra(Database.COLUMN_SAVING_ID)).hashCode();

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, savingRequestCode, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String notificationTitle = savingName + " Deadline!";
        String notificationBody = "The deadline for your " + savingName + " is today.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DEADLINE_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(savingRequestCode, builder.build());
    }
}