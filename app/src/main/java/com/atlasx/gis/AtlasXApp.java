package com.atlasx.gis;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.osmdroid.config.Configuration;

import java.io.File;

/**
 * Application entry point shared by every activity.
 *
 * <p>Initialises the osmdroid tile cache and user agent (required by the
 * OpenStreetMap tile usage policy) and creates the notification channel used
 * to report finished background file operations.</p>
 */
public class AtlasXApp extends Application {

    public static final String CHANNEL_FILE_OPERATIONS = "atlasx_file_operations";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.atlasx.gis.util.LocaleHelper.updateBaseContext(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // osmdroid needs a stable user agent and an explicit cache directory.
        File cache = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().load(
                this,
                getSharedPreferences("osmdroid_config", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(cache);
        Configuration.getInstance().setOsmdroidTileCache(new File(cache, "tiles"));

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_FILE_OPERATIONS,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(getString(R.string.notif_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
