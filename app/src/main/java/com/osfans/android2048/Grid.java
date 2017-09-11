package com.osfans.android2048;

import java.util.ArrayList;

public class Grid {

    public Tile[][] field;
    public Tile[][] lastField;
    public boolean canRevert = false;

    int sizeX, sizeY;

    public Grid(int sizeX, int sizeY) {
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

    public Cell randomAvailableCell() {
        ArrayList<Cell> availableCells = getAvailableCells();
        if (availableCells.size() >= 1) {
            return availableCells.get((int) Math.floor(Math.random() * availableCells.size()));
        }
        return null;
    }

    public ArrayList<Cell> getAvailableCells() {
        ArrayList<Cell> availableCells = new ArrayList<Cell>();
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] == null) {
                    availableCells.add(new Cell(xx, yy));
                }
            }
        }
        return availableCells;
    }

    public boolean isCellsAvailable() {
        return (getAvailableCells().size() >= 1);
    }

    public boolean isCellAvailable(Cell cell) {
        return !isCellOccupied(cell);
    }

    public boolean isCellOccupied(Cell cell) {
        return (getCellContent(cell) != null);
    }

    public Tile getCellContent(Cell cell) {
        if (cell != null && isCellWithinBounds(cell)) {
            return field[cell.getX()][cell.getY()];
        } else {
            return null;
        }
    }

    public Tile getCellContent(int x, int y) {
        if (isCellWithinBounds(x, y)) {
            return field[x][y];
        } else {
            return null;
        }
    }

    public boolean isCellWithinBounds(Cell cell) {
        return 0 <= cell.getX() && cell.getX() < field.length
                && 0 <= cell.getY() && cell.getY() < field[0].length;
    }

    public boolean isCellWithinBounds(int x, int y) {
        return 0 <= x && x < field.length
                && 0 <= y && y < field[0].length;
    }

    public void insertTile(Tile tile) {
        field[tile.getX()][tile.getY()] = tile;
    }

    public void removeTile(Tile tile) {
        field[tile.getX()][tile.getY()] = null;
    }

    public void saveTiles() {
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

    public void revertTiles() {
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
