package com.osfans.android2048;

import android.content.Context;
import android.content.SharedPreferences;

class SettingsProvider {
    private static final String KEY_PREFERENCES = "com.osfans.android2048_preferences";

    static final String KEY_SENSITIVITY = "settings_sensitivity";
    static final String KEY_ORDER = "settings_order";
    static final String KEY_ROWS = "settings_rows";
    static final String KEY_VARIETY = "settings_variety";
    static final String KEY_INVERSE_MODE = "settings_inverse_mode";
    static final String KEY_SYSTEM_FONT = "settings_system_font";
    static final String KEY_CUSTOM_VARIETY = "settings_custom_variety";

    private static SharedPreferences prefs;

    static void initPreferences(Context context) {
        prefs = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_MULTI_PROCESS);
    }

    static int getInt(String key, String defaultValue) {
        return Integer.parseInt(prefs.getString(key, defaultValue));
    }

    static boolean getBoolean(String key) {
        return prefs.getBoolean(key, false);
    }

    static String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
}
