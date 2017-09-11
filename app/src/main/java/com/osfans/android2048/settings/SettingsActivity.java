package com.osfans.android2048.settings;

import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.content.Context;
import android.view.MenuItem;
import android.os.Bundle;

import com.osfans.android2048.R;
import com.osfans.android2048.InputListener;
import com.osfans.android2048.MainActivity;
import com.osfans.android2048.MainView;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
    private ListPreference mSensitivity, mOrder, mRows;
    private ListPreference mVariety;
    private CheckBoxPreference mInverse;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        
        addPreferencesFromResource(R.xml.settings);
        
        mSensitivity = (ListPreference) findPreference(SettingsProvider.KEY_SENSITIVITY);
        mOrder = (ListPreference) findPreference(SettingsProvider.KEY_ORDER);
        mRows = (ListPreference) findPreference(SettingsProvider.KEY_ROWS);
        mVariety = (ListPreference) findPreference(SettingsProvider.KEY_VARIETY);
        mInverse = (CheckBoxPreference) findPreference(SettingsProvider.KEY_INVERSE_MODE);
        
        mSensitivity.setOnPreferenceChangeListener(this);
        mOrder.setOnPreferenceChangeListener(this);
        mRows.setOnPreferenceChangeListener(this);
        mVariety.setOnPreferenceChangeListener(this);
        mInverse.setOnPreferenceChangeListener(this);
        
        // Initialize values
        int sensitivity = SettingsProvider.getInt(SettingsProvider.KEY_SENSITIVITY, 1);
        mSensitivity.setValueIndex(sensitivity);
        String[] sensitivitySummaries = getResources().getStringArray(R.array.settings_sensitivity_entries);
        mSensitivity.setSummary(sensitivitySummaries[sensitivity]);

        int order = SettingsProvider.getInt(SettingsProvider.KEY_ORDER, 0);
        mOrder.setValueIndex(order);
        String[] orderSummaries = getResources().getStringArray(R.array.settings_order_entries);
        mOrder.setSummary(orderSummaries[order]);

        String rows = SettingsProvider.getString(SettingsProvider.KEY_ROWS, "4");
        mRows.setValue(rows);
        mRows.setSummary(rows);

        int variety = mVariety.findIndexOfValue(SettingsProvider.getString(SettingsProvider.KEY_VARIETY, getResources().getString(R.string.variety_entries_default)));
        mVariety.setValueIndex(variety);
        String[] varietySummaries = getResources().getStringArray(R.array.settings_variety_entries);
        mVariety.setSummary(varietySummaries[variety]);
        
        mInverse.setChecked(SettingsProvider.getBoolean(SettingsProvider.KEY_INVERSE_MODE, false));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSensitivity) {
            int sensitivity = Integer.valueOf((String) newValue);
            String[] sensitivitySummaries = getResources().getStringArray(R.array.settings_sensitivity_entries);
            mSensitivity.setSummary(sensitivitySummaries[sensitivity]);
            SettingsProvider.putInt(SettingsProvider.KEY_SENSITIVITY, sensitivity);
            InputListener.loadSensitivity();
            return true;
        } else if (preference == mVariety) {
            int variety = mVariety.findIndexOfValue((String) newValue);
            String[] varietySummaries = getResources().getStringArray(R.array.settings_variety_entries);
            mVariety.setSummary(varietySummaries[variety]);
            SettingsProvider.putString(SettingsProvider.KEY_VARIETY, (String) newValue);

            MainActivity.getInstance().newCell();
            MainActivity.getInstance().setTitle(varietySummaries[variety]);
            return true;
        } else if (preference == mInverse) {
            boolean inverse = (Boolean) newValue;
            SettingsProvider.putBoolean(SettingsProvider.KEY_INVERSE_MODE, inverse);
            MainView.inverseMode = inverse;
            return true;
        } else if (preference == mOrder) {
            int order = Integer.valueOf((String) newValue);
            String[] orderSummaries = getResources().getStringArray(R.array.settings_order_entries);
            mOrder.setSummary(orderSummaries[order]);
            SettingsProvider.putInt(SettingsProvider.KEY_ORDER, order);
            MainActivity.getInstance().newCell();
            return true;
        } else if (preference == mRows) {
            SettingsProvider.putString(SettingsProvider.KEY_ROWS, (String)newValue);
            mRows.setSummary((String)newValue);
            clearState();
            MainActivity.getInstance().newGame();
            return true;
        } else  {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void clearState() {
        getSharedPreferences("state", Context.MODE_WORLD_READABLE)
                 .edit()
                 .remove("size")
                 .commit();
        MainActivity.save = false;
    }
}
