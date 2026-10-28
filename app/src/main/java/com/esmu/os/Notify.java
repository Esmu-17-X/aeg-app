package com.esmu.os;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.json.JSONObject;

final class Notify {

    private static final String CH = "esmu";

    static JSONObject send(Context ctx, String title, String text) throws Exception {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) throw new Exception("No notification service");

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "Esmu", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(c);
        }
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(ctx, CH)
                : new Notification.Builder(ctx);
        b.setSmallIcon(android.R.drawable.stat_notify_more)
         .setContentTitle(title == null ? "Esmu" : title)
         .setContentText(text == null ? "" : text)
         .setAutoCancel(true);
        nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        return new JSONObject();
    }
}
