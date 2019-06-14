package com.osfans.android2048;

class AnimationCell extends Cell {
    final int[] extras;
    private final int animationType;
    private long timeElapsed;
    private final long animationTime;
    private final long delayTime;

    AnimationCell(int x, int y, int animationType, long length, long delay, int[] extras) {
        super(x, y);
        this.animationType = animationType;
        animationTime = length;
        delayTime = delay;
        this.extras = extras;
    }

    int getAnimationType() {
        return animationType;
    }

    void tick(long timeElapsed) {
        this.timeElapsed = this.timeElapsed + timeElapsed;
    }

    boolean animationDone() {
        return animationTime + delayTime < timeElapsed;
    }

    double getPercentageDone() {
        return Math.max(0, 1.0 * (timeElapsed - delayTime) / animationTime);
    }

    boolean isActive() {
        return (timeElapsed >= delayTime);
    }

}
