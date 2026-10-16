package com.atlasx.gis.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import com.atlasx.gis.data.SettingsRepository;

import java.util.Locale;

/**
 * Applies the language chosen in Settings to every context in the process.
 *
 * <p>Android only re-resolves string resources for the context that changed,
 * so each activity and the application itself wrap their base context here.
 * That is what makes a language switch update the whole interface instead of
 * only the screen that triggered it.</p>
 */
public final class LocaleHelper {

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_PERSIAN = "fa";

    private LocaleHelper() {
    }

    /** Returns the locale stored in preferences, defaulting to English. */
    public static Locale current(Context context) {
        String tag = new SettingsRepository(context).getLanguage();
        return localeFor(tag);
    }

    /** Wraps a base context with the persisted locale and layout direction. */
    public static Context updateBaseContext(Context base) {
        Locale locale = localeFor(new SettingsRepository(base).getLanguage());
        return apply(base, locale);
    }

    private static Locale localeFor(String tag) {
        if (tag == null || tag.isEmpty()) {
            return Locale.ENGLISH;
        }
        Locale locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? Locale.ENGLISH : locale;
    }

    private static Context apply(Context base, Locale locale) {
        Locale.setDefault(locale);
        Resources res = base.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        LocaleList list = LocaleList.forLanguageTags(toTag(locale));
        LocaleList.setDefault(list);
        config.setLocales(list);
        return base.createConfigurationContext(config);
    }

    private static String toTag(Locale locale) {
        return locale.toLanguageTag();
    }

    /** True when the active language writes right to left. */
    public static boolean isRtl(Context context) {
        return "fa".equals(new SettingsRepository(context).getLanguage());
    }
}
