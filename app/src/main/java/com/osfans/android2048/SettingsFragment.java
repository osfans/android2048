package com.osfans.android2048;

import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;


/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
    private Preference mCustomVariety;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        ListPreference sensitivity = findPreference(SettingsProvider.KEY_SENSITIVITY);
        ListPreference order = findPreference(SettingsProvider.KEY_ORDER);
        ListPreference rows = findPreference(SettingsProvider.KEY_ROWS);
        ListPreference variety = findPreference(SettingsProvider.KEY_VARIETY);
        CheckBoxPreference inverse = findPreference(SettingsProvider.KEY_INVERSE_MODE);
        CheckBoxPreference systemFont = findPreference(SettingsProvider.KEY_SYSTEM_FONT);
        mCustomVariety = findPreference(SettingsProvider.KEY_CUSTOM_VARIETY);

        if (sensitivity != null) {
            int value = SettingsProvider.getInt(SettingsProvider.KEY_SENSITIVITY, getResources().getString(R.string.default_sensitivity));
            String[] summaries = getResources().getStringArray(R.array.settings_sensitivity_entries);
            sensitivity.setSummary(summaries[value]);
            sensitivity.setOnPreferenceChangeListener(this);
        }
        if (order != null) {
            int value = SettingsProvider.getInt(SettingsProvider.KEY_ORDER, getResources().getString(R.string.default_order));
            String[] summaries = getResources().getStringArray(R.array.settings_order_entries);
            order.setSummary(summaries[value]);
            order.setOnPreferenceChangeListener(this);
        }
        if (rows != null) {
            String value = SettingsProvider.getString(SettingsProvider.KEY_ROWS, getResources().getString(R.string.default_row));
            rows.setSummary(value);
            rows.setOnPreferenceChangeListener(this);
        }
        if (variety != null) {
            int value = SettingsProvider.getInt(SettingsProvider.KEY_VARIETY, getResources().getString(R.string.default_variety));
            String[] summaries = getResources().getStringArray(R.array.settings_variety_entries);
            variety.setSummary(summaries[value]);
            variety.setOnPreferenceChangeListener(this);
        }
        if (inverse != null) inverse.setOnPreferenceChangeListener(this);
        if (systemFont != null) systemFont.setOnPreferenceChangeListener(this);
        if (mCustomVariety != null) mCustomVariety.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case SettingsProvider.KEY_SENSITIVITY:
                int sensitivity = Integer.parseInt((String) newValue);
                String[] sensitivitySummaries = getResources().getStringArray(R.array.settings_sensitivity_entries);
                preference.setSummary(sensitivitySummaries[sensitivity]);
                SettingsProvider.putString(SettingsProvider.KEY_SENSITIVITY, (String) newValue);
                InputListener.loadSensitivity();
                break;
            case SettingsProvider.KEY_VARIETY:
                int variety = ((ListPreference)preference).findIndexOfValue((String) newValue);
                String[] varietySummaries = getResources().getStringArray(R.array.settings_variety_entries);
                mCustomVariety.setEnabled(variety == varietySummaries.length - 1);
                preference.setSummary(varietySummaries[variety]);
                SettingsProvider.putString(SettingsProvider.KEY_VARIETY, (String) newValue);
                MainActivity.getInstance().newGame();
                break;
            case SettingsProvider.KEY_CUSTOM_VARIETY:
                SettingsProvider.putString(SettingsProvider.KEY_CUSTOM_VARIETY, (String) newValue);
                MainActivity.getInstance().newGame();
                break;
            case SettingsProvider.KEY_INVERSE_MODE:
                boolean inverse = (Boolean) newValue;
                SettingsProvider.putBoolean(SettingsProvider.KEY_INVERSE_MODE, inverse);
                MainView.inverseMode = inverse;
                break;
            case SettingsProvider.KEY_SYSTEM_FONT:
                boolean value = (Boolean) newValue;
                SettingsProvider.putBoolean(SettingsProvider.KEY_SYSTEM_FONT, value);
                MainActivity.getInstance().newGame();
                break;
            case SettingsProvider.KEY_ORDER:
                int order = Integer.parseInt((String) newValue);
                String[] orderSummaries = getResources().getStringArray(R.array.settings_order_entries);
                preference.setSummary(orderSummaries[order]);
                SettingsProvider.putString(SettingsProvider.KEY_ORDER, (String) newValue);
                MainActivity.getInstance().newCell();
                break;
            case SettingsProvider.KEY_ROWS:
                SettingsProvider.putString(SettingsProvider.KEY_ROWS, (String) newValue);
                preference.setSummary((String) newValue);
                clearState();
                MainActivity.getInstance().newGame();
                break;
            default:
                return false;
        }
        return true;
    }

    private void clearState() {
        MainActivity.getInstance().getSharedPreferences("state", 0)
                .edit()
                .remove("size")
                .apply();
        MainActivity.save = false;
    }
}
