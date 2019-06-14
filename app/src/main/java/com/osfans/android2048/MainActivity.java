package com.osfans.android2048;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.osfans.android2048.settings.SettingsActivity;
import com.osfans.android2048.settings.SettingsProvider;

public class MainActivity extends AppCompatActivity {

    public static boolean save = true;
    private static MainActivity mSelf;
    private MainView view;

    public static MainActivity getInstance() {
        return mSelf;
    }

    public void newGame() {
        view = new MainView(getBaseContext());

        // Restore state
        SharedPreferences prefs = getSharedPreferences("state", 0);
        int size = prefs.getInt("size", 0);
        if (size == MainGame.numSquaresX) {
            Tile[][] field = view.game.grid.field;
            String[] saveState = new String[field[0].length];
            for (int xx = 0; xx < saveState.length; xx++) {
                saveState[xx] = prefs.getString("" + xx, "");
            }
            for (int xx = 0; xx < saveState.length; xx++) {
                String[] array = saveState[xx].split("\\|");
                for (int yy = 0; yy < array.length; yy++) {
                    if (!array[yy].startsWith("0")) {
                        view.game.grid.field[xx][yy] = new Tile(xx, yy, Integer.valueOf(array[yy]));
                    } else {
                        view.game.grid.field[xx][yy] = null;
                    }
                }
            }
            view.game.score = prefs.getLong("score", 0);
            view.game.highScore = prefs.getLong("high score", 0);
            view.game.won = prefs.getBoolean("won", false);
            view.game.lose = prefs.getBoolean("lose", false);
        }
        setContentView(view);
        initTitle();
    }

    public void newCell() {
        view.initRectangleDrawables();
    }

    private void initTitle() {
        Resources resources = getResources();
        String s = SettingsProvider.getString(SettingsProvider.KEY_VARIETY, resources.getString(R.string.variety_entries_default));
        int i = Integer.valueOf(s);
        String[] varietySummaries = resources.getStringArray(R.array.settings_variety_entries);
        setTitle(varietySummaries[i]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelf = this;
        SettingsProvider.initPreferences(this);
        InputListener.loadSensitivity();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        newGame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_auto_run).setCheckable(true);
        if (MainView.inverseMode) {
            menu.findItem(R.id.menu_undo).setEnabled(false);
            menu.findItem(R.id.menu_auto_run).setEnabled(false);
        } else if (view.aiRunning) {
            menu.findItem(R.id.menu_undo).setEnabled(false);
            menu.findItem(R.id.menu_auto_run).setEnabled(true);
            menu.findItem(R.id.menu_auto_run).setChecked(true);
        } else {
            menu.findItem(R.id.menu_undo).setEnabled(view.game.grid.canRevert);
            menu.findItem(R.id.menu_auto_run).setEnabled(true);
            menu.findItem(R.id.menu_auto_run).setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_undo:
                view.game.revertState();
                return true;
            case R.id.menu_settings:
                Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.setClass(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.menu_auto_run:
                view.toggleAi();
                return true;
            case R.id.menu_new_game:
                view.stopAi();
                view.game.newGame();
                return true;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        // If variety switched, do not save
        if (!save) return;

        SharedPreferences prefs = getSharedPreferences("state", 0);
        SharedPreferences.Editor edit = prefs.edit();
        Tile[][] field = view.game.grid.field;
        String[] saveState = new String[field[0].length];
        for (int xx = 0; xx < field.length; xx++) {
            saveState[xx] = "";
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    saveState[xx] += String.valueOf(field[xx][yy].getValue());
                } else {
                    saveState[xx] += "0";
                }
                if (yy < field[0].length - 1) {
                    saveState[xx] += "|";
                }
            }
        }
        for (int xx = 0; xx < saveState.length; xx++) {
            edit.putString("" + xx, saveState[xx]);
        }
        edit.putLong("score", view.game.score);
        edit.putLong("high score", view.game.highScore);
        edit.putBoolean("won", view.game.won);
        edit.putBoolean("lose", view.game.lose);
        edit.putInt("size", MainGame.numSquaresX);
        edit.apply();
    }
}
