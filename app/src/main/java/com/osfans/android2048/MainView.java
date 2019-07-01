package com.osfans.android2048;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

public class MainView extends View {
    static final int BASE_ANIMATION_TIME = 120000000;
    private static final float MOVING_ACCELERATION = (float) 0.6;
    private static final float MERGING_ACCELERATION = (float) 0.6;
    private static final float MAX_VELOCITY = (float) (MERGING_ACCELERATION * 0.5); // v = at (t = 0.5)
    public static boolean inverseMode = false;
    static int sYIcons;
    static int sXNewGame;
    static int iconSize;
    static int maxValue;
    private static int textPaddingSize = 0;
    public final MainGame game;
    private final Paint paint = new Paint();
    private final Paint paintOrder = new Paint();
    private AI ai;
    int cellSize = 0;
    private float textSize = 0;
    int gridWidth = 0;
    private int boardMiddleX = 0;
    private int boardMiddleY = 0;
    private Drawable backgroundRectangle;
    private Drawable[] cellRectangle = new Drawable[12];
    private Drawable lightUpRectangle;
    private Drawable fadeRectangle;
    private Bitmap background = null;
    private int backgroundColor;
    private int TEXT_BLACK;
    private int TEXT_WHITE;
    private int TEXT_BROWN;
    int startingX;
    int startingY;
    private int endingX;
    private int endingY;
    private int sYAll;
    private int titleStartYAll;
    private int bodyStartYAll;
    private int eYAll;
    private int titleWidthHighScore;
    private int titleWidthScore;
    private long lastFPSTime = System.nanoTime();
    private float titleTextSize;
    private float bodyTextSize;
    private float headerTextSize;
    private float instructionsTextSize;
    private float gameOverTextSize;
    boolean refreshLastTime = true;
    private String highScore;
    private String score;
    private String youWin;
    private String gameOver;
    private String instructions = "";
    private String[] tileTexts;

    static class MyHandler extends Handler {
        private final WeakReference<MainView> mView;

        MyHandler(MainView view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mView.get() == null) {
                return;
            }
            MainView view = mView.get();
            if (!view.getGame().move((Integer) msg.obj)) {
                // If not moved, random move
                sendMessage(this.obtainMessage(0, Math.abs(new Random().nextInt()) % 4));
            } else {
                view.invalidate();
            }
        }
    }

    private final Handler aiHandler = new MyHandler(this);
    private Thread aiThread;
    boolean aiRunning = false;
    private final Runnable aiRunnable = new Runnable() {
        @Override
        public void run() {
            while (!game.won && !game.lose) {
                try {
                    int bestMove = ai.getBestMove();
                    aiHandler.sendMessage(aiHandler.obtainMessage(0, bestMove));
                } catch (NullPointerException e) {
                    break;
                }

                if (inverseMode) {
                    // Run only one step in inverse mode
                    aiRunning = false;
                    aiThread = null;
                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    ai = null;
                    break;
                }
            }
        }
    };

    private void updateFont() {
        boolean systemFont = SettingsProvider.getBoolean(SettingsProvider.KEY_SYSTEM_FONT, false);
        Typeface font = null;
        if (!systemFont) font = Typeface.createFromAsset(getResources().getAssets(), "Symbola.ttf");
        paint.setTypeface(font);
    }

    private MainGame getGame() {
        return game;
    }

    public MainView(Context context) {
        super(context);
        setFocusable(true);
        Resources resources = context.getResources();

        //Loading resources
        game = new MainGame(context, this);

        try {
            highScore = resources.getString(R.string.high_score);
            score = resources.getString(R.string.score);
            youWin = resources.getString(R.string.you_win);
            gameOver = resources.getString(R.string.game_over);
            backgroundRectangle = resources.getDrawable(R.drawable.background_rectangle);
            lightUpRectangle = resources.getDrawable(R.drawable.light_up_rectangle);
            fadeRectangle = resources.getDrawable(R.drawable.fade_rectangle);
            TEXT_WHITE = resources.getColor(R.color.text_white);
            TEXT_BLACK = resources.getColor(R.color.text_black);
            TEXT_BROWN = resources.getColor(R.color.text_brown);
            backgroundColor = resources.getColor(R.color.background);
            paint.setAntiAlias(true);
            updateFont();
        } catch (Exception e) {
            System.out.println("Error getting assets?");
        }
        InputListener listener = new InputListener(this);
        setOnTouchListener(listener);
        setOnKeyListener(listener);
        newGame();
    }

    private static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return (int) (Math.log(n) / Math.log(2));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {
        super.onSizeChanged(width, height, oldW, oldH);
        getLayout(width, height);
        createBackgroundBitmap(width, height);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //Reset the transparency of the screen

        canvas.drawBitmap(background, 0, 0, paint);

        drawScoreText(canvas);

        drawCells(canvas);

        drawEndGameState(canvas);

        if (game.aGrid.isAnimationActive()) {
            // Refresh when animation running
            invalidate(startingX, startingY, endingX, endingY);
            tick();
        } else if ((game.won || game.lose) && refreshLastTime) {
            // Refresh last time when game end
            invalidate();
            refreshLastTime = false;
        }
    }

    private void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }

    private void drawCellText(Canvas canvas, int value) {
        int n = tileTexts[value - 1].codePointCount(0, tileTexts[value - 1].length());
        float i = textSize / (float) (n > 1 ? n * 0.51 : 1);
        paint.setTextSize(n == 1 && tileTexts[value - 1].length() == 2 ? (float) (i * 1.6) : i);
        paintOrder.setTextSize((float) (textSize / 2.5));
        int textShiftY = centerText();
        if (value >= 3) {
            paint.setColor(TEXT_WHITE);
            paintOrder.setColor(TEXT_WHITE);
        } else {
            paint.setColor(TEXT_BLACK);
            paintOrder.setColor(TEXT_BLACK);
        }
        canvas.drawText(tileTexts[value - 1], cellSize / 2f, cellSize / 2f - textShiftY, paint);
        int order = SettingsProvider.getInt(SettingsProvider.KEY_ORDER, 0);
        if (order < 2) {
            canvas.drawText(order == 0 ? String.valueOf(value) : String.valueOf((char) (value - 1 + 'A')), 0, -paintOrder.ascent(), paintOrder);
        }
    }

    private void drawScoreText(Canvas canvas) {
        //Drawing the score text: Ver 2
        paint.setTextSize(bodyTextSize);
        paint.setTextAlign(Paint.Align.CENTER);

        int bodyWidthHighScore = (int) (paint.measureText("" + game.highScore));
        int bodyWidthScore = (int) (paint.measureText("" + game.score));

        int textWidthHighScore = Math.max(titleWidthHighScore, bodyWidthHighScore) + textPaddingSize * 2;
        int textWidthScore = Math.max(titleWidthScore, bodyWidthScore) + textPaddingSize * 2;

        int textMiddleHighScore = textWidthHighScore / 2;
        int textMiddleScore = textWidthScore / 2;

        int eXHighScore = endingX;
        int sXHighScore = eXHighScore - textWidthHighScore;

        int eXScore = sXHighScore - textPaddingSize;
        int sXScore = eXScore - textWidthScore;

        //Outputting high-scores box
        backgroundRectangle.setBounds(sXHighScore, sYAll, eXHighScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(highScore, sXHighScore + textMiddleHighScore, titleStartYAll, paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText("" + game.highScore, sXHighScore + textMiddleHighScore, bodyStartYAll, paint);


        //Outputting scores box
        backgroundRectangle.setBounds(sXScore, sYAll, eXScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(score, sXScore + textMiddleScore, titleStartYAll, paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText("" + game.score, sXScore + textMiddleScore, bodyStartYAll, paint);
    }

    private void drawHeader(Canvas canvas) {
        //Drawing the header
        paint.setTextSize(headerTextSize);
        paint.setColor(TEXT_BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        //int textShiftY = centerText() * 2;
        //int headerStartY = sYAll - textShiftY + (int) headerTextSize;
        canvas.drawText(String.valueOf(maxValue), startingX, bodyStartYAll, paint);
    }

    private void drawInstructions(Canvas canvas) {
        //Drawing the instructions
        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        canvas.drawText(instructions,
                startingX, endingY - textShiftY + textPaddingSize, paint);
    }

    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY);
    }

    private void drawBackgroundGrid(Canvas canvas) {
        // Outputting the game grid
        for (int xx = 0; xx < MainGame.numSquaresX; xx++) {
            for (int yy = 0; yy < MainGame.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                drawDrawable(canvas, cellRectangle[0], sX, sY, eX, eY);
            }
        }
    }

    private void drawCells(Canvas canvas) {
        // Outputting the individual cells
        for (int xx = 0; xx < MainGame.numSquaresX; xx++) {
            for (int yy = 0; yy < MainGame.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.field[xx][yy];
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    int value = currentTile.getValue();
                    int index = log2(value);
                    if (index >= cellRectangle.length) {
                        newGame();
                        return;
                    }

                    //Check for any active animations
                    ArrayList<AnimationCell> aArray = game.aGrid.getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationCell aCell = aArray.get(i);
                        //If this animation is not active, skip it
                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) {
                            animated = true;
                        }
                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // Spawning animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);

                            float cellScaleSize = cellSize / 2f * (1 - textScaleSize);
                            drawDrawable(canvas, cellRectangle[index], (int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                        } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // Merging Animation
                            double percentDone = aCell.getPercentageDone();

                            float currentVelocity;

                            // Accelerate and then moderate
                            if (percentDone < 0.5) {
                                currentVelocity = (float) (MERGING_ACCELERATION * percentDone); // v = at
                            } else {
                                currentVelocity = (float) (MAX_VELOCITY - MERGING_ACCELERATION * (percentDone - 0.5)); // v = v0 - at
                            }

                            float textScaleSize = (float) (1 + currentVelocity * percentDone); // s = vt

                            float cellScaleSize = cellSize / 2f * (1 - textScaleSize);
                            drawDrawable(canvas, cellRectangle[index], (int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                        } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) {  // Moving animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1) * (percentDone - 1) * -MOVING_ACCELERATION);
                            int dY = (int) ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1) * (percentDone - 1) * -MOVING_ACCELERATION);

                            drawDrawable(canvas, cellRectangle[tempIndex], sX + dX, sY + dY, eX + dX, eY + dY);
                        }
                        animated = true;
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        drawDrawable(canvas, cellRectangle[index], sX, sY, eX, eY);
                    }
                }
            }
        }
    }

    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        //Animation: Dynamically change the alpha
        for (AnimationCell animation : game.aGrid.globalAnimation) {
            if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.getPercentageDone();
            }

        }
        // Displaying game over
        if (game.won) {
            lightUpRectangle.setAlpha((int) (127 * alphaChange));
            drawDrawable(canvas, lightUpRectangle, startingX, startingY, endingX, endingY);
            lightUpRectangle.setAlpha(255);
            paint.setColor(TEXT_WHITE);
            paint.setAlpha((int) (255 * alphaChange));
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(youWin, boardMiddleX, boardMiddleY - centerText(), paint);
            paint.setAlpha(255);
        } else if (game.lose) {
            fadeRectangle.setAlpha((int) (127 * alphaChange));
            drawDrawable(canvas, fadeRectangle, startingX, startingY, endingX, endingY);
            fadeRectangle.setAlpha(255);
            paint.setColor(TEXT_BLACK);
            paint.setAlpha((int) (255 * alphaChange));
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(gameOver, boardMiddleX, boardMiddleY - centerText(), paint);
            paint.setAlpha(255);
        }
    }

    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        canvas.drawColor(backgroundColor);
        drawHeader(canvas);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);
    }

    private void tick() {
        long currentTime = System.nanoTime();

        try {
            game.aGrid.tickAll(currentTime - lastFPSTime);
        } catch (ConcurrentModificationException e) {
            // Might be modified in background
        }
        lastFPSTime = currentTime;
    }

    public void reSyncTime() {
        lastFPSTime = System.nanoTime();
    }

    private void getLayout(int width, int height) {
        cellSize = Math.min(width / (MainGame.numSquaresX + 1), height / (MainGame.numSquaresY + 3));
        gridWidth = cellSize / 7;
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;
        boardMiddleX = screenMiddleX;
        boardMiddleY = screenMiddleY + cellSize / 2;
        iconSize = cellSize / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize / Math.max(cellSize, paint.measureText("0000"));
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        instructionsTextSize = (int) (textSize / 1.5);
        headerTextSize = Math.min(textSize * 2, cellSize * cellSize * 2 / Math.max(cellSize, paint.measureText(String.valueOf(maxValue))));
        gameOverTextSize = textSize * 2;
        textPaddingSize = (int) (textSize / 3);
        //int iconPaddingSize = (int) (textSize / 5);

        //Grid Dimensions
        double halfNumSquaresX = MainGame.numSquaresX / 2d;
        double halfNumSquaresY = MainGame.numSquaresY / 2d;

        startingX = (int) (boardMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2f);
        endingX = (int) (boardMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2f);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2f);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2f);

        paint.setTextSize(titleTextSize);

        int textShiftYAll = centerText();
        //static variables
        sYAll = (int) (startingY - cellSize * 1.5);
        titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);
        bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize / 2 + bodyTextSize / 2);

        titleWidthHighScore = (int) (paint.measureText(highScore));
        titleWidthScore = (int) (paint.measureText(score));
        paint.setTextSize(bodyTextSize);
        textShiftYAll = centerText();
        eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
        sXNewGame = (endingX - iconSize);
        reSyncTime();
        //boolean getScreenSize = false;
        initRectangleDrawables();
    }

    public void initRectangleDrawables() {
        updateFont();
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);

        Resources resources = getResources();

        // Tile texts
        int index = Integer.valueOf(SettingsProvider.getString(SettingsProvider.KEY_VARIETY, resources.getString(R.string.variety_entries_default)));
        String[] values = resources.getStringArray(R.array.variety_strings);
        String s = "";
        if (index == values.length - 1) { // custom
            s = SettingsProvider.getString(SettingsProvider.KEY_CUSTOM_VARIETY, "");
        }
        if (s.length() == 0) s = values[index];
        tileTexts = s.split(s.contains(" ") ? " +" : "\\B");
        maxValue = (int) Math.pow(2, tileTexts.length);

        cellRectangle = new Drawable[12];
        cellRectangle[0] = resources.getDrawable(R.drawable.cell_rectangle);
        cellRectangle[1] = resources.getDrawable(R.drawable.cell_rectangle_2);
        cellRectangle[2] = resources.getDrawable(R.drawable.cell_rectangle_4);
        cellRectangle[3] = resources.getDrawable(R.drawable.cell_rectangle_8);
        cellRectangle[4] = resources.getDrawable(R.drawable.cell_rectangle_16);
        cellRectangle[5] = resources.getDrawable(R.drawable.cell_rectangle_32);
        cellRectangle[6] = resources.getDrawable(R.drawable.cell_rectangle_64);
        cellRectangle[7] = resources.getDrawable(R.drawable.cell_rectangle_128);
        cellRectangle[8] = resources.getDrawable(R.drawable.cell_rectangle_256);
        cellRectangle[9] = resources.getDrawable(R.drawable.cell_rectangle_512);
        cellRectangle[10] = resources.getDrawable(R.drawable.cell_rectangle_1024);
        cellRectangle[11] = resources.getDrawable(R.drawable.cell_rectangle_2048);
        // The last drawable
        Drawable lastDrawable = cellRectangle[11];

        // Array
        Drawable[] newArray = new Drawable[tileTexts.length + 1];
        newArray[0] = cellRectangle[0];

        // Draw the rectangles into cache
        for (int i = 1; i < tileTexts.length + 1; i++) {
            Drawable rect;
            if (i <= 11) {
                rect = cellRectangle[i];
            } else {
                rect = lastDrawable;
            }
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, rect, 0, 0, cellSize, cellSize);
            drawCellText(canvas, i);
            rect = new BitmapDrawable(bitmap);
            newArray[i] = rect;
        }

        cellRectangle = newArray;
    }

    private int centerText() {
        return (int) ((paint.descent() + paint.ascent()) / 2);
    }

    private void newGame() {
        Resources resources = getResources();
        // Inverse mode
        inverseMode = SettingsProvider.getBoolean(SettingsProvider.KEY_INVERSE_MODE, false);

        int i = Integer.valueOf(SettingsProvider.getString(SettingsProvider.KEY_ROWS, "4"));
        MainGame.numSquaresX = i;
        MainGame.numSquaresY = i;
        if (!inverseMode) {
            instructions = resources.getString(R.string.instructions);
        } else {
            instructions = resources.getString(R.string.instructions_inverse);
        }
        game.newGame();
    }

    public void startAi() {
        if (aiThread != null) {
            stopAi();
        }

        ai = new AI(game);
        aiThread = new Thread(aiRunnable);
        aiThread.start();
        aiRunning = true;
    }

    public void stopAi() {
        if (!aiRunning) return;
        aiThread.interrupt();

        aiThread = null;
        aiRunning = false;
    }

    public void toggleAi() {
        if (aiRunning) {
            stopAi();
            return;
        }
        startAi();
    }
}
