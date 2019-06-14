package com.osfans.android2048;

class Tile extends Cell {
    private final int value;
    //private Cell previousPosition = null;
    private Tile[] mergedFrom = null;

    Tile(int x, int y, int value) {
        super(x, y);
        this.value = value;
    }

    Tile(Cell cell, int value) {
        super(cell.getX(), cell.getY());
        this.value = value;
    }

    /*
    void savePosition() {
        previousPosition = new Cell(this.getX(), this.getY());
    }
    */

    void updatePosition(Cell cell) {
        this.setX(cell.getX());
        this.setY(cell.getY());
    }

    int getValue() {
        return this.value;
    }

    /*
    public void setValue(int value) {
        this.value = value;
    }
    */

    Tile[] getMergedFrom() {
        return mergedFrom;
    }

    void setMergedFrom(Tile[] tile) {
        mergedFrom = tile;
    }

    /*
    public Cell getPreviousPosition() {
        return previousPosition;
    }
    */
}
