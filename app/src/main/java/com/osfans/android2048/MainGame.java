package com.osfans.android2048;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MainGame {

    static final int SPAWN_ANIMATION = -1;
    static final int MOVE_ANIMATION = 0;
    static final int MERGE_ANIMATION = 1;
    static final int FADE_GLOBAL_ANIMATION = 0;
    private static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    private static final long SPAWN_ANIMATION_TIME = (int) (MainView.BASE_ANIMATION_TIME * 1.5);
    private static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final String HIGH_SCORE = "high score";
    static int numSquaresX = 4;
    static int numSquaresY = 4;
    Grid grid;
    AnimationGrid aGrid;
    private boolean emulating = false;
    long score = 0;
    private long lastScore = 0;
    long highScore = 0;
    boolean won = false;
    boolean lose = false;
    private final Context mContext;
    private final MainView mView;

    MainGame(Context context, MainView view) {
        mContext = context;
        mView = view;
    }

    void newGame() {
        grid = new Grid(numSquaresX, numSquaresY);
        aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        highScore = getHighScore();
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
        score = 0;
        won = false;
        lose = false;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.reSyncTime();
        mView.postInvalidate();
    }

    private void addStartTiles() {
        int startTiles = 2;
        for (int xx = 0; xx < startTiles; xx++) {
            this.addRandomTile();
        }
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            addRandomTile(grid.randomAvailableCell());
        }
    }

    void addRandomTile(Cell cell) {
        int value = Math.random() < 0.1 ? 2 : 4;
        Tile tile = new Tile(cell, value);
        grid.insertTile(tile);
        if (!emulating) aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
    }

    private void recordHighScore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(HIGH_SCORE, highScore);
        editor.apply();
    }

    private long getHighScore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getLong(HIGH_SCORE, -1);
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field) {
            for (Tile tile : array) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                    //tile.savePosition();
                }
            }
        }
    }

    private void moveTile(Tile tile, Cell cell) {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveState() {
        grid.saveTiles();
        lastScore = score;
    }

    void revertState() {
        aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        grid.revertTiles();
        score = lastScore;

        if (!emulating) {
            mView.refreshLastTime = true;
            mView.reSyncTime();
            mView.invalidate();
        }
    }

    boolean move(int direction) {
        saveState();

        if (!emulating) aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        // 0: up, 1: right, 2: down, 3: left
        if (lose || won) {
            return false;
        }
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        if (!emulating) {
                            int[] extras = {xx, yy};
                            aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                    MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = MOVING MERGED
                            aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                    SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);
                        }

                        // Update the score
                        score = score + merged.getValue();
                        highScore = Math.max(score, highScore);

                        // The mighty max tile
                        if (merged.getValue() == MainView.maxValue) {
                            won = true;
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        if (!emulating)
                            aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            if (!emulating && !MainView.inverseMode) {
                addRandomTile();
            }

            if (!movesAvailable()) {
                lose = true;
                endGame();
            }

        }

        if (!emulating) {
            mView.reSyncTime();
            mView.postInvalidate();
        }

        return moved;
    }

    private void endGame() {
        if (emulating) return;

        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }

        grid.canRevert = false;
    }

    Cell getVector(int direction) {
        Cell[] map = {
                new Cell(0, -1), // up
                new Cell(1, 0),  // right
                new Cell(0, 1),  // down
                new Cell(-1, 0)  // left
        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

        return new Cell[]{previous, nextCell};
    }

    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;

        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));

                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    // Only for emulation
    @Override
    public MainGame clone() {
        MainGame newGame = new MainGame(mContext, null);

        newGame.grid = grid.clone();
        newGame.score = score;
        newGame.emulating = true;

        return newGame;
    }
}
