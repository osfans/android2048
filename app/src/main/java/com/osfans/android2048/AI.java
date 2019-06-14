package com.osfans.android2048;

import java.util.ArrayList;
import java.util.Date;

/*
 *
 * This is a simple AI for the 2048 game
 * Based on alpha-beta method
 * Credits to: Matt Overlan
 *
 */

class AI {
    private static final long MAX_CONSIDERING_TIME = 100;
    private static final float WEIGHT_SMOOTH = 0.1f, WEIGHT_MONO = 1.0f,
            WEIGHT_EMPTY = 2.7f, WEIGHT_MAX = 1.0f;
            //WEIGHT_ISLANDS = 0.5f, WEIGHT_TWOANDFOUR = 2.5f;
    private final MainGame mGame;

    AI(MainGame game) {
        mGame = game;
    }

    int getBestMove() {

        int bestMove = 0;
        int depth = 0;
        long start = new Date().getTime();

        do {
            int move = (Integer) search(mGame.clone(), depth, -10000, 10000, Player.DOCTOR)[0];
            if (move == -1) {
                break;
            } else {
                bestMove = move;
                depth++;
            }
        } while (new Date().getTime() - start < MAX_CONSIDERING_TIME);

        return bestMove;
    }

    /*
     *
     * Search for the best move
     * Based on alpha-beta search method
     * Simulates two players' game
     * The Doctor V.S. The Daleks
     *
     */
    private Object[] search(MainGame game, int depth, int alpha, int beta, Player player) {
        int bestMove = -1;
        int bestScore = 0;

        if (player == Player.DOCTOR) {
            // The Doctor's turn
            // Doctor wants to defeat the Daleks
            bestScore = alpha;

            for (int i = 0; i <= 3; i++) {
                MainGame g = game.clone();

                if (!g.move(i)) {
                    continue;
                }

                if (game.won) {
                    // If won, just do it
                    return new Object[]{i, 10000};
                }

                int score;

                if (depth == 0) {
                    // Just evaluate if this is at the bottom
                    score = evaluate(g);
                } else {
                    // Pass the game to the Daleks
                    score = (Integer) search(g, depth - 1, bestScore, beta, Player.DALEKS)[1];

                    // Don't search any further if won
                    if (score > 9900) {
                        score--;
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }

                // We have found a much much better move
                // So, cutoff
                if (bestScore > beta) {
                    return new Object[]{bestMove, beta};
                }
            }
        } else if (player == Player.DALEKS) {
            // The Daleks' turn
            // "EXTERMINATE!"
            bestScore = beta;

            int maxScore = Integer.MIN_VALUE;

            ArrayList<Object[]> conditions = new ArrayList<>();

            ArrayList<Cell> cells = game.grid.getAvailableCells();

            // Pick out the worst ones for the Doctor
            // Try to insert 2
            for (Cell cell : cells) {
                Tile t = new Tile(cell, 2);
                game.grid.insertTile(t);
                int score = -getSmoothness(game) + countIslands(game);
                conditions.add(new Object[]{cell, 2, score});
                game.grid.removeTile(t);
            }

            // Try to insert 4
            for (Cell cell : cells) {
                Tile t = new Tile(cell, 4);
                game.grid.insertTile(t);
                int score = -getSmoothness(game) + countIslands(game);
                conditions.add(new Object[]{cell, 4, score});
                game.grid.removeTile(t);
            }

            // Find the max score(the worst for the Doctor)
            for (Object[] obj : conditions) {
                int score = (Integer) obj[2];
                if (score > maxScore) {
                    maxScore = score;
                }
            }

            // Play all the games with the Doctor
            for (Object[] obj : conditions) {
                int s = (Integer) obj[2];

                // If not worst, just skip it
                if (s != maxScore) continue;

                Cell cell = (Cell) obj[0];
                int value = (Integer) obj[1];
                MainGame g = game.clone();

                Tile t = new Tile(cell, value);
                g.grid.insertTile(t);

                // Pass the game to human
                int score = (Integer) search(g, depth, alpha, bestScore, Player.DOCTOR)[1];

                if (score < bestScore) {
                    bestScore = score;
                }

                // Computer lose
                // Cutoff
                if (bestScore < alpha) {
                    return new Object[]{-1, alpha};
                }
            }
            //return new Object[]{bestMove, beta};
        }

        return new Object[]{bestMove, bestScore};
    }

    // Evaluate how is it if we take the step
    private int evaluate(MainGame game) {
        int smooth = getSmoothness(game);
        int mono = getMonotonicity(game);
        int empty = game.grid.getAvailableCells().size();
        int max = getMaxValue(game);
        //int islands = countIslands(game);
        //int twoAndFour = countTwosAndFours(game);

        return (int) (smooth * WEIGHT_SMOOTH
                + mono * WEIGHT_MONO
                + Math.log(empty) * WEIGHT_EMPTY
                + max * WEIGHT_MAX
                //- islands * WEIGHT_ISLANDS
                    /*- twoAndFour * WEIGHT_TWOANDFOUR*/);
    }

    // How smooth the grid is
    private int getSmoothness(MainGame game) {
        int smoothness = 0;
        for (int x = 0; x < MainGame.numSquaresX; x++) {
            for (int y = 0; y < MainGame.numSquaresY; y++) {
                Tile t = game.grid.field[x][y];
                if (t != null) {
                    int value = (int) (Math.log(t.getValue()) / Math.log(2));
                    for (int direction = 1; direction <= 2; direction++) {
                        Cell vector = game.getVector(direction);
                        Cell targetCell = game.findFarthestPosition(new Cell(x, y), vector)[1];

                        if (game.grid.isCellOccupied(targetCell)) {
                            Tile target = game.grid.getCellContent(targetCell);
                            int targetValue = (int) (Math.log(target.getValue()) / Math.log(2));

                            smoothness -= Math.abs(value - targetValue);
                        }
                    }

                }
            }
        }

        return smoothness;
    }

    // How monotonic the grid is
    private int getMonotonicity(MainGame game) {
        int[] totals = {0, 0, 0, 0};

        // Up-down
        for (int x = 0; x < MainGame.numSquaresX; x++) {
            int current = 0;
            int next = current + 1;
            while (next < MainGame.numSquaresY) {
                while (next < MainGame.numSquaresY && game.grid.isCellAvailable(new Cell(x, next))) {
                    next++;
                }
                if (next >= MainGame.numSquaresY) {
                    next--;
                }
                int currentValue = game.grid.isCellOccupied(new Cell(x, current)) ?
                        (int) (Math.log(game.grid.getCellContent(x, current).getValue()) / Math.log(2)) :
                        0;
                int nextValue = game.grid.isCellOccupied(new Cell(x, next)) ?
                        (int) (Math.log(game.grid.getCellContent(x, next).getValue()) / Math.log(2)) :
                        0;
                if (currentValue > nextValue) {
                    totals[0] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[1] += currentValue - nextValue;
                }
                current = next;
                next++;
            }
        }

        // Left-right
        for (int y = 0; y < MainGame.numSquaresY; y++) {
            int current = 0;
            int next = current + 1;
            while (next < MainGame.numSquaresX) {
                while (next < MainGame.numSquaresX && game.grid.isCellAvailable(new Cell(next, y))) {
                    next++;
                }
                if (next >= MainGame.numSquaresX) {
                    next--;
                }
                int currentValue = game.grid.isCellOccupied(new Cell(current, y)) ?
                        (int) (Math.log(game.grid.getCellContent(current, y).getValue()) / Math.log(2)) :
                        0;
                int nextValue = game.grid.isCellOccupied(new Cell(next, y)) ?
                        (int) (Math.log(game.grid.getCellContent(next, y).getValue()) / Math.log(2)) :
                        0;
                if (currentValue > nextValue) {
                    totals[2] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[3] += currentValue - nextValue;
                }
                current = next;
                next++;
            }
        }

        return Math.max(totals[0], totals[1]) + Math.max(totals[2], totals[3]);
    }

    private int getMaxValue(MainGame game) {
        int max = 0;
        for (int x = 0; x < MainGame.numSquaresX; x++) {
            for (int y = 0; y < MainGame.numSquaresY; y++) {
                Cell cell = new Cell(x, y);
                if (game.grid.isCellOccupied(cell)) {
                    Tile t = game.grid.getCellContent(cell);
                    int value = t.getValue();
                    if (value > max) {
                        max = value;
                    }
                }
            }
        }
        return max;
    }

    private int countIslands(MainGame game) {
        int islands = 0;

        for (int x = 0; x < MainGame.numSquaresX; x++) {
            for (int y = 0; y < MainGame.numSquaresY; y++) {
                if (game.grid.isCellOccupied(new Cell(x, y))) {
                    game.grid.getCellContent(x, y).marked = false;
                }
            }
        }

        for (int x = 0; x < MainGame.numSquaresX; x++) {
            for (int y = 0; y < MainGame.numSquaresY; y++) {
                if (game.grid.isCellOccupied(new Cell(x, y))) {
                    Tile t = game.grid.getCellContent(x, y);
                    if (!t.marked) {
                        islands++;
                        mark(game, x, y, t.getValue());
                    }
                }
            }
        }

        return islands;
    }

    private void mark(MainGame game, int x, int y, int value) {
        if (game.grid.isCellWithinBounds(x, y) && game.grid.isCellOccupied(new Cell(x, y))) {
            Tile t = game.grid.getCellContent(x, y);
            if (!t.marked && t.getValue() == value) {
                t.marked = true;

                for (int i = 0; i <= 3; i++) {
                    Cell vector = game.getVector(i);
                    mark(game, x + vector.getX(), y + vector.getY(), value);
                }
            }
        }
    }

    /*
    private int countTwosAndFours(MainGame game) {
        int num = 0;
        for (int x = 0; x < MainGame.numSquaresX; x++) {
            for (int y = 0; y < MainGame.numSquaresY; y++) {
                Cell cell = new Cell(x, y);
                if (game.grid.isCellOccupied(cell)) {
                    Tile t = game.grid.getCellContent(cell);

                    if (t.getValue() <= 4) {
                        num++;
                    }
                }
            }
        }
        return num;
    }
    */

    enum Player {
        DOCTOR,
        DALEKS
    }
}
