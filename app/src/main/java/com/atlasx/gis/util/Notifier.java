package com.atlasx.gis.util;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.atlasx.gis.AtlasXApp;
import com.atlasx.gis.R;

/**
 * Posts the notifications that report a finished background file operation.
 *
 * <p>On Android 13 and above the POST_NOTIFICATIONS permission is requested at
 * runtime first; when it is denied the result is still shown on screen, which
 * the calling screen already does.</p>
 */
public final class Notifier {

    private static final int NOTIFICATION_ID = 4210;

    private Notifier() {
    }

    /** Returns true when posting notifications is currently allowed. */
    public static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Requests the notification permission (API 33+) when it is missing. */
    public static void requestIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !canNotify(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    public static final int REQUEST_NOTIFICATIONS = 4211;

    /** Posts a single, replaceable notification for a finished operation. */
    public static void notifyFileOperation(Context context, CharSequence title,
                                           CharSequence text) {
        if (!canNotify(context)) {
            return;
        }
        Notification notification = new NotificationCompat.Builder(
                context, AtlasXApp.CHANNEL_FILE_OPERATIONS)
                .setSmallIcon(R.drawable.ic_export)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
        } catch (SecurityException e) {
            // Permission revoked between the check and the post.
            android.util.Log.w("AtlasX", "Notification not posted", e);
        }
    }

    /** Static helper so screens can explain why the permission is requested. */
    public static CharSequence rationale(Context context) {
        return context.getString(R.string.notif_permission_rationale);
    }
}
