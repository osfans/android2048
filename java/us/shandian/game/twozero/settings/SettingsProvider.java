package us.shandian.game.twozero.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsProvider
{
    public static String KEY_PREFERENCES = "preferences";
    
    public static String KEY_SENSITIVITY = "settings_sensitivity";
    public static String KEY_ORDER = "settings_order";
    public static String KEY_ROWS = "settings_rows";
    public static String KEY_VARIETY = "settings_variety";
    public static String KEY_INVERSE_MODE = "settings_inverse_mode";

    public static SharedPreferences prefs;
    
    public static void initPreferences(Context context) {
        prefs = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_MULTI_PROCESS);
    }
    
    public static int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    public static String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
    
    public static void putInt(String key, int value) {
        prefs.edit().putInt(key, value).commit();
    }
    
    public static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }
    
    public static void putString(String key, String value) {
        prefs.edit().putString(key, value).commit();
    }
    
    public static void remove(String key) {
        prefs.edit().remove(key).commit();
    }
}
