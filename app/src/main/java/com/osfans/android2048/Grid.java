package com.osfans.android2048;

import java.util.ArrayList;

public class Grid {

    Tile[][] field;
    private Tile[][] lastField;
    boolean canRevert = false;

    private final int sizeX;
    private final int sizeY;

    Grid(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        field = new Tile[sizeX][sizeY];
        lastField = new Tile[sizeX][sizeY];
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                field[xx][yy] = null;
                lastField[xx][yy] = null;
            }
        }
    }

    Cell randomAvailableCell() {
        ArrayList<Cell> availableCells = getAvailableCells();
        if (availableCells.size() >= 1) {
            return availableCells.get((int) Math.floor(Math.random() * availableCells.size()));
        }
        return null;
    }

    ArrayList<Cell> getAvailableCells() {
        ArrayList<Cell> availableCells = new ArrayList<>();
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] == null) {
                    availableCells.add(new Cell(xx, yy));
                }
            }
        }
        return availableCells;
    }

    boolean isCellsAvailable() {
        return (getAvailableCells().size() >= 1);
    }

    boolean isCellAvailable(Cell cell) {
        return !isCellOccupied(cell);
    }

    boolean isCellOccupied(Cell cell) {
        return (getCellContent(cell) != null);
    }

    Tile getCellContent(Cell cell) {
        if (cell != null && isCellWithinBounds(cell)) {
            return field[cell.getX()][cell.getY()];
        } else {
            return null;
        }
    }

    Tile getCellContent(int x, int y) {
        if (isCellWithinBounds(x, y)) {
            return field[x][y];
        } else {
            return null;
        }
    }

    boolean isCellWithinBounds(Cell cell) {
        return 0 <= cell.getX() && cell.getX() < field.length
                && 0 <= cell.getY() && cell.getY() < field[0].length;
    }

    boolean isCellWithinBounds(int x, int y) {
        return 0 <= x && x < field.length
                && 0 <= y && y < field[0].length;
    }

    void insertTile(Tile tile) {
        field[tile.getX()][tile.getY()] = tile;
    }

    void removeTile(Tile tile) {
        field[tile.getX()][tile.getY()] = null;
    }

    void saveTiles() {
        canRevert = true;

        lastField = new Tile[sizeX][sizeY];
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field.length; yy++) {
                if (field[xx][yy] == null) {
                    lastField[xx][yy] = null;
                } else {
                    lastField[xx][yy] = new Tile(xx, yy, field[xx][yy].getValue());
                }
            }
        }
    }

    void revertTiles() {
        canRevert = false;

        for (int xx = 0; xx < lastField.length; xx++) {
            for (int yy = 0; yy < lastField.length; yy++) {
                if (lastField[xx][yy] == null) {
                    field[xx][yy] = null;
                } else {
                    field[xx][yy] = new Tile(xx, yy, lastField[xx][yy].getValue());
                }
            }
        }
    }

    @Override
    public Grid clone() {
        Tile[][] newField = new Tile[sizeX][sizeY];
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field.length; yy++) {
                if (field[xx][yy] == null) {
                    newField[xx][yy] = null;
                } else {
                    newField[xx][yy] = new Tile(xx, yy, field[xx][yy].getValue());
                }
            }
        }

        Grid newGrid = new Grid(sizeX, sizeY);
        newGrid.field = newField;
        return newGrid;
    }
}
