package com.passivestrokedetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    private static final int NOTIF_ID = 1;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    public ForegroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(intent);

//        return START_NOT_STICKY;
        sendNotification("IMMEDIATE ACTION REQUIRED", "Stroke has possibly been detected");
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    private void startForeground(Intent intent) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
//                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void sendNotification(String subject, String body) {
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(subject)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        manager.notify(0, notify);
    }
}
