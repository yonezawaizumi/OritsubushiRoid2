package com.wsf_lp.oritsubushi;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by yonezawaizumi on 2017/01/01.
 */

public class FirMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data == null) {
            return;
        }
        String fragment = data.get(MainActivity.ARG_FRAGMENT);
        if (fragment == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "default");
        builder.setSmallIcon(R.drawable.notification);
        builder.setContentTitle(remoteMessage.getNotification().getTitle());
        builder.setContentText(remoteMessage.getNotification().getBody());
        builder.setDefaults(Notification.DEFAULT_SOUND
                | Notification.DEFAULT_VIBRATE
                | Notification.DEFAULT_LIGHTS);
        builder.setAutoCancel(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("FCM");
        intent.putExtra(MainActivity.ARG_FRAGMENT, fragment);
        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Notification表示
        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.notify(0, builder.build());
    }
}
