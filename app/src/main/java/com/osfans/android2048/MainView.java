package com.osfans.android2048;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.os.Handler;
import android.os.Message;

import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Random;
import java.util.ConcurrentModificationException;

import com.osfans.android2048.settings.SettingsProvider;

public class MainView extends View
{
    Paint paint = new Paint();
    Paint paintOrder = new Paint();
    public MainGame game;
    AI ai;
    InputListener listener;

    boolean getScreenSize = true;
    int cellSize = 0;
    float textSize = 0;
    int gridWidth = 0;
    int screenMiddleX = 0;
    int screenMiddleY = 0;
    int boardMiddleX = 0;
    int boardMiddleY = 0;
    Drawable backgroundRectangle;
    Drawable[] cellRectangle = new Drawable[12];
    Drawable settingsIcon;
    Drawable lightUpRectangle;
    Drawable fadeRectangle;
    Bitmap background = null;
    int backgroundColor;
    int TEXT_BLACK;
    int TEXT_WHITE;
    int TEXT_BROWN;


    double halfNumSquaresX;
    double halfNumSquaresY;

    int startingX;
    int startingY;
    int endingX;
    int endingY;

    int sYAll;
    int titleStartYAll;
    int bodyStartYAll;
    int eYAll;
    int titleWidthHighScore;
    int titleWidthScore;

    static int sYIcons;
    static int sXNewGame;

    static int iconSize;
    long lastFPSTime = System.nanoTime();
    long currentTime = System.nanoTime();

    float titleTextSize;
    float bodyTextSize;
    float headerTextSize;
    float instructionsTextSize;
    float gameOverTextSize;

    boolean refreshLastTime = true;
    
    String highScore, score, youWin, gameOver, instructions = "";

    String[] tileTexts;
    static int maxValue;
    
    public static boolean inverseMode = false;
    
    static final int BASE_ANIMATION_TIME = 120000000;
    static int textPaddingSize = 0;
    static int iconPaddingSize = 0;

    static final float MOVING_ACCELERATION = (float) 0.6;
    static final float MERGING_ACCELERATION = (float) 0.6;
    static final float MAX_VELOCITY = (float) (MERGING_ACCELERATION * 0.5); // v = at (t = 0.5)
    
    Handler aiHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!game.move((Integer) msg.obj)) {
                // If not moved, random move
                this.sendMessage(this.obtainMessage(0, Math.abs(new Random().nextInt()) % 4));
            } else {
                invalidate();
            }
        }
    };
    
    Runnable aiRunnable = new Runnable() {
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
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    ai = null;
                    break;
                }
            }
        }
    };
    
    Thread aiThread;
    
    boolean aiRunning = false;
    
    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH)
    {
        super.onSizeChanged(width, height, oldW, oldH);
        getLayout(width, height);
        createBackgroundBitmap(width, height);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        //Reset the transparency of the screen

        canvas.drawBitmap(background, 0, 0, paint);

        drawScoreText(canvas);

        if ((game.won || game.lose) && !game.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas);
        }

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

    public void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }

    public void drawCellText(Canvas canvas, int value, int sX, int sY) {
        int n = tileTexts[value - 1].codePointCount(0,tileTexts[value - 1].length());
        float i = textSize / (float)(n > 1 ? n * 0.51 : 1);
        paint.setTextSize(n == 1 && tileTexts[value - 1].length() == 2 ? (float)(i * 1.6) : i);
        paintOrder.setTextSize((float) (textSize / 2.5));
        int textShiftY = centerText();
        if (value >= 3) {
            paint.setColor(TEXT_WHITE);
            paintOrder.setColor(TEXT_WHITE);
        } else {
            paint.setColor(TEXT_BLACK);
            paintOrder.setColor(TEXT_BLACK);
        }
        canvas.drawText(tileTexts[value - 1], sX + cellSize / 2, sY + cellSize / 2 - textShiftY, paint);
        int order = SettingsProvider.getInt(SettingsProvider.KEY_ORDER, 0);
        if (order < 2) {
            canvas.drawText(order == 0 ? String.valueOf(value) : String.valueOf((char) (value - 1 + 'A')), 0, -paintOrder.ascent(), paintOrder);
        }
    }

    public void drawScoreText(Canvas canvas) {
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

    public void drawNewGameButton(Canvas canvas) {
        if ((game.won || game.lose)) {
            drawDrawable(canvas, lightUpRectangle, sXNewGame, sYIcons, sXNewGame + iconSize, sYIcons + iconSize);
        } else {
            drawDrawable(canvas, backgroundRectangle, sXNewGame, sYIcons, sXNewGame + iconSize, sYIcons + iconSize);
        }
        drawDrawable(canvas, settingsIcon, sXNewGame + iconPaddingSize, sYIcons + iconPaddingSize,
                sXNewGame + iconSize - iconPaddingSize, sYIcons + iconSize - iconPaddingSize);
    }

    public void drawHeader(Canvas canvas) {
        //Drawing the header
        paint.setTextSize(headerTextSize);
        paint.setColor(TEXT_BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        int headerStartY = sYAll - textShiftY + (int)headerTextSize ;
        canvas.drawText(getResources().getString(R.string.app_name), startingX, bodyStartYAll, paint);
    }

    public void drawInstructions(Canvas canvas) {
        //Drawing the instructions
        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        canvas.drawText(instructions,
                startingX, endingY - textShiftY + textPaddingSize, paint);
    }

    public void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY);
    }

    public void drawBackgroundGrid(Canvas canvas) {
        // Outputting the game grid
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                drawDrawable(canvas, cellRectangle[0], sX, sY, eX, eY);
            }
        }
    }

    public void drawCells(Canvas canvas) {
        // Outputting the individual cells
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
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

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
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

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
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

    public void drawEndGameState(Canvas canvas) {
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
            drawDrawable(canvas, lightUpRectangle ,startingX, startingY, endingX, endingY);
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

    public void createBackgroundBitmap(int width, int height) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        canvas.drawColor(backgroundColor);
        drawHeader(canvas);
        drawNewGameButton(canvas);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);
    }


    public void tick() {
        currentTime = System.nanoTime();
        
        try {
            game.aGrid.tickAll(currentTime - lastFPSTime);
        } catch (ConcurrentModificationException e) {
            // Might be modified in background
        }
        lastFPSTime = currentTime;
    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    public static int log2(int n){
        if(n <= 0) throw new IllegalArgumentException();
        return (int) (Math.log(n) / Math.log(2));
    }

    public void getLayout(int width, int height) {
        cellSize = Math.min(width / (game.numSquaresX + 1), height / (game.numSquaresY + 3));
        gridWidth = cellSize / 7;
        screenMiddleX = width / 2;
        screenMiddleY = height / 2;
        boardMiddleX = screenMiddleX;
        boardMiddleY = screenMiddleY  + cellSize / 2;
        iconSize = cellSize / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize / Math.max(cellSize, paint.measureText("0000"));
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        instructionsTextSize = (int) (textSize / 1.5);
        headerTextSize = textSize * 2;
        gameOverTextSize = textSize * 2;
        textPaddingSize = (int) (textSize / 3);
        iconPaddingSize = (int) (textSize / 5);

        //Grid Dimensions
        halfNumSquaresX = game.numSquaresX / 2d;
        halfNumSquaresY = game.numSquaresY / 2d;

        startingX = (int) (boardMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (boardMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2);

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
        resyncTime();
        getScreenSize = false;
        initRectangleDrawables();
    }
    
    public void initRectangleDrawables() {
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);

        Resources resources = getResources();

        // Tile texts
        String s = SettingsProvider.getString(SettingsProvider.KEY_VARIETY, resources.getString(R.string.variety_entries_default));
        tileTexts = s.split(s.contains(" ") ? " " : "\\B");
        for (int i = 0; i < tileTexts.length; i++) {
            s = tileTexts[i];
            if (s.startsWith("#"))  tileTexts[i] = new String(new int[]{new Integer(s.substring(1))},0,1);
        }
        maxValue = (int) Math.pow(2, tileTexts.length);

        cellRectangle = new Drawable[12];
        cellRectangle[0] =  resources.getDrawable(R.drawable.cell_rectangle);
        cellRectangle[1] =  resources.getDrawable(R.drawable.cell_rectangle_2);
        cellRectangle[2] =  resources.getDrawable(R.drawable.cell_rectangle_4);
        cellRectangle[3] =  resources.getDrawable(R.drawable.cell_rectangle_8);
        cellRectangle[4] =  resources.getDrawable(R.drawable.cell_rectangle_16);
        cellRectangle[5] =  resources.getDrawable(R.drawable.cell_rectangle_32);
        cellRectangle[6] =  resources.getDrawable(R.drawable.cell_rectangle_64);
        cellRectangle[7] =  resources.getDrawable(R.drawable.cell_rectangle_128);
        cellRectangle[8] =  resources.getDrawable(R.drawable.cell_rectangle_256);
        cellRectangle[9] =  resources.getDrawable(R.drawable.cell_rectangle_512);
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
            drawCellText(canvas, i, 0, 0);
            rect = new BitmapDrawable(bitmap);
            newArray[i] = rect;
        }
        
        cellRectangle = newArray;
    }

    public int centerText() {
        return  (int) ((paint.descent() + paint.ascent()) / 2);
    }

    public void newGame(){
        Resources resources = getResources();
        // Inverse mode
        inverseMode = SettingsProvider.getBoolean(SettingsProvider.KEY_INVERSE_MODE, false);

        int i = new Integer(SettingsProvider.getString(SettingsProvider.KEY_ROWS, "4"));
        game.numSquaresX = i;
        game.numSquaresY = i;
        if (!inverseMode) {
            instructions = resources.getString(R.string.instructions);
        } else {
            instructions = resources.getString(R.string.instructuons_inversed);
        }
        game.newGame();
    }

    public MainView(Context context) {
        super(context);
        Resources resources = context.getResources();

        //Loading resources
        game = new MainGame(context, this);

        try {
            highScore = resources.getString(R.string.high_score);
            score = resources.getString(R.string.score);
            youWin = resources.getString(R.string.you_win);
            gameOver = resources.getString(R.string.game_over);
            backgroundRectangle =  resources.getDrawable(R.drawable.background_rectangle);
            settingsIcon = resources.getDrawable(R.drawable.ic_action_refresh);
            lightUpRectangle = resources.getDrawable(R.drawable.light_up_rectangle);
            fadeRectangle = resources.getDrawable(R.drawable.fade_rectangle);
            TEXT_WHITE = resources.getColor(R.color.text_white);
            TEXT_BLACK = resources.getColor(R.color.text_black);
            TEXT_BROWN = resources.getColor(R.color.text_brown);
            backgroundColor = resources.getColor(R.color.background);
            Typeface font = Typeface.createFromAsset(resources.getAssets(), "Symbola.ttf");
            paint.setTypeface(font);
            paint.setAntiAlias(true);
        } catch (Exception e) {
            System.out.println("Error getting assets?");
        }
        listener = new InputListener(this);
        setOnTouchListener(listener);
        setOnKeyListener(listener);
        newGame();
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
        aiThread.interrupt();
        
        aiThread = null;
        aiRunning = false;
    }

}
