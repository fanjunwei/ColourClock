package org.gringene.colourclock;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

public class ClockActivity extends Activity {
    final String LOG_TAG = "colourclock";
    ColourClock mClock;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClock = new ColourClock(this);
        setContentView(mClock);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "Resuming...");
        mClock.startTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "Pausing...");
        mClock.stopTick();
    }
}