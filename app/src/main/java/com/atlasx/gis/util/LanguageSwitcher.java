package com.atlasx.gis.util;

import android.app.Activity;
import android.content.Intent;

import com.atlasx.gis.MainActivity;
import com.atlasx.gis.R;
import com.atlasx.gis.data.SettingsRepository;

import androidx.appcompat.app.AlertDialog;

/**
 * Language selection for the mandatory English / Persian support.
 *
 * <p>The choice is written to {@link SettingsRepository} before the task is
 * restarted, so {@link LocaleHelper} wraps every context with the new locale
 * and the whole interface - not only the current screen - switches language.
 * The selection survives process restarts because it lives in
 * SharedPreferences.</p>
 */
public final class LanguageSwitcher {

    private LanguageSwitcher() {
    }

    /** Shows the single choice language dialog. */
    public static void showChooser(final Activity activity) {
        final SettingsRepository settings = new SettingsRepository(activity);
        String current = settings.getLanguage();
        int checked = LocaleHelper.LANG_PERSIAN.equals(current) ? 1 : 0;
        String[] labels = {
                activity.getString(R.string.language_english),
                activity.getString(R.string.language_persian)
        };

        new AlertDialog.Builder(activity)
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String tag = which == 1
                            ? LocaleHelper.LANG_PERSIAN
                            : LocaleHelper.LANG_ENGLISH;
                    dialog.dismiss();
                    if (!tag.equals(settings.getLanguage())) {
                        apply(activity, tag);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /** Persists the language and restarts the task so every screen updates. */
    public static void apply(Activity activity, String languageTag) {
        new SettingsRepository(activity).setLanguage(languageTag);
        restartTask(activity);
    }

    /**
     * Recreates the whole task. Using CLEAR_TASK guarantees that screens still
     * in the back stack are rebuilt with the new locale instead of keeping
     * views inflated with the previous language.
     */
    private static void restartTask(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }
}
