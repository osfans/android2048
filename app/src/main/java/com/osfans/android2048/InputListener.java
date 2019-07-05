package com.osfans.android2048;

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

class InputListener implements View.OnTouchListener, View.OnKeyListener {

    private static final int SWIPE_MIN_DISTANCE = 100;
    private static int SWIPE_THRESHOLD_VELOCITY = 40;
    private static int MOVE_THRESHOLD = 250;
    private final MainView mView;
    private final GestureDetector mGestureDetector;
    private float x;
    private float y;
    /*
    private static final int RESET_STARTING = 10;
    private float lastDx;
    private float lastDy;
    private float previousX;
    private float previousY;
    private float startingX;
    private float startingY;
    private boolean moved = false;
    */
    private int previousDirection = 1;
    private int veryLastDirection = 1;

    InputListener(MainView view) {
        super();
        this.mView = view;
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float fX = e1.getX() - e2.getX();
                    float fY = e1.getY() - e2.getY();
                    if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY && Math.abs(fX) + SWIPE_MIN_DISTANCE / 2 >= Math.abs(fY) && Math.abs(fX) > SWIPE_MIN_DISTANCE && Math.abs(fX) < MOVE_THRESHOLD * 2) {
                        mView.game.move(fX > 0 ? 3 : 1);
                    } else if (Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY && Math.abs(fY) > SWIPE_MIN_DISTANCE && Math.abs(fY) < MOVE_THRESHOLD * 2) {
                        mView.game.move(fY > 0 ? 0 : 2);
                    } else return false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {

                x = event.getX();
                y = event.getY();
                previousDirection = 1;
                veryLastDirection = 1;
                if (inRange(MainView.sXNewGame, x, MainView.sXNewGame + MainView.iconSize)
                        && inRange(MainView.sYIcons, y, MainView.sYIcons + MainView.iconSize)) {
                    mView.game.newGame();
                }

                if (MainView.inverseMode) {
                    for (Cell cell : mView.game.grid.getAvailableCells()) {
                        int xx = cell.getX();
                        int yy = cell.getY();
                        int sX = mView.startingX + mView.gridWidth + (mView.cellSize + mView.gridWidth) * xx;
                        int eX = sX + mView.cellSize;
                        int sY = mView.startingY + mView.gridWidth + (mView.cellSize + mView.gridWidth) * yy;
                        int eY = sY + mView.cellSize;

                        if (inRange(sX, x, eX) && inRange(sY, y, eY)) {
                            mView.game.addRandomTile(cell);
                            mView.invalidate();
                            mView.startAi();
                            break;
                        }
                    }
                }
                return true;
            }
        });
    }

    static void loadSensitivity() {
        int sensitivity = SettingsProvider.getInt(SettingsProvider.KEY_SENSITIVITY, 1);
        switch (sensitivity) {
            case 0:
                SWIPE_THRESHOLD_VELOCITY = 20;
                MOVE_THRESHOLD = 200;
                break;
            case 1:
                SWIPE_THRESHOLD_VELOCITY = 60;
                MOVE_THRESHOLD = 250;
                break;
            case 2:
                SWIPE_THRESHOLD_VELOCITY = 100;
                MOVE_THRESHOLD = 300;
                break;
        }
    }

    public boolean onTouch(View view, MotionEvent event) {
        view.performClick();
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mView.game.move(2);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    mView.game.move(0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mView.game.move(3);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mView.game.move(1);
                    return true;
            }
        }
        return false;
    }

    private boolean inRange(float left, float check, float right) {
        return (left <= check && check <= right);
    }

}
