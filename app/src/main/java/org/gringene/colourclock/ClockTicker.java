package org.gringene.colourclock;

/**
 * Created by gringer on 17/07/14.
 */
public class ClockTicker implements Runnable {
    private final ColourClock mClock;

    public ClockTicker(ColourClock tClock) {
        mClock = tClock;
    }

    public void run() {
        mClock.updateTime();
    }
}
