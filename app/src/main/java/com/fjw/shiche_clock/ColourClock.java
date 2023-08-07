package com.fjw.shiche_clock;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

enum ClockViewType {
    NORMAL,
    SHI_CHEN,
    JING_LUO,
}

public class ColourClock extends View implements View.OnClickListener, Runnable {

    private final ReentrantLock drawLock = new ReentrantLock();
    private boolean onDraw = false;
    private boolean dryLock = false;
    private final ReentrantLock drawStateLock = new ReentrantLock();
    private ClockViewType currentViewType = ClockViewType.NORMAL;
    private static final String LOG_TAG = "clock_view";
    private static final float OUTER_POS = 16;  // position of numbers in sixteenths of circle radius
    private static final float INNER_POS = 11;
    private static final float NUMBER_POS = (OUTER_POS + INNER_POS) / 2f;
    private static final float SEC_POS = 10;
    private static final float MIN_POS = 9;
    private static final float HOUR_POS = 6.5f;
    private static final float HOUR_WIDTH = 0.5f; // in sixteenths of circle radius
    private static final float MIN_WIDTH = 0.25f;
    private static final float SEC_WIDTH = 0.125f;
    private final Paint brushes = new Paint(Paint.ANTI_ALIAS_FLAG);
    //    private Time mCalendar;
    private float mHours;
    private float mMinutes;
    private float mSeconds;

    private float centreX;
    private float centreY;
    private float bandWidth;

    private Bitmap backing;
    private Canvas painting;

    private boolean running = false;
    private Timer updateTimer = null;


    private boolean started = false;

    public ColourClock(Context context) {
        super(context);
        init();
    }

    public ColourClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTick();
        this.setOnClickListener(this);
    }

    protected void updateTime() {
        Log.d(LOG_TAG, "updateTime");
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        long currentMillis = System.currentTimeMillis();
        int secInt = (int) ((currentMillis % 60000) / 1000);
        float mSecFrac = (currentMillis % 1000) / 1000f;
        mSecFrac = (float) (1 - Math.sin((0.5f - mSecFrac) * Math.PI)) / 2;
        mSeconds = secInt + mSecFrac;
        mMinutes = min + mSeconds / 60;
        mHours = hour + mMinutes / 60;
        mMinutes = (float) Math.floor(mMinutes); // now that the Hour is done, ensure minutes jump
        //Log.d("org.gringene.colourclock",String.format("Updating time, now %02d:%02d:%02.2f", hour, min, mSeconds));
        if (started) {
            drawClock();
        }
    }

    public int getAngleColour(double theta) {
        // top: red, right: yellow, bottom: [dark] green, left: blue
        theta = theta % 360;
        double h, s, v;
        h = 0;
        s = 1;
        v = 1;
        if ((theta >= 0) && (theta < 90)) {
            h = (theta * (60f / 90f)); // red to yellow
        } else if ((theta >= 90) && (theta < 180)) {
            h = ((theta - 90) * (60f / 90f)) + 60; // yellow to dark green
            v = 1 - ((theta - 90) / 180f);
        } else if ((theta >= 180) && (theta < 270)) {
            h = ((theta - 180) * (120f / 90f)) + 120; // dark green to blue
            v = ((theta - 180) / 180f) + 0.5;
        } else if ((theta >= 270) && (theta < 360)) {
            h = ((theta - 270) * (120f / 90f)) + 240; // blue to red
        }
        float[] hsvVals = {(float) h, (float) s, (float) v};
        return Color.HSVToColor(hsvVals);
    }

    public void drawNumbers(Canvas tPainting) {
        brushes.setStyle(Paint.Style.FILL);
        brushes.setColor(Color.BLACK);
        Rect b = new Rect();
        int tag_count;

        if (this.currentViewType == ClockViewType.NORMAL) {
            tag_count = 12;
            brushes.setTextSize(bandWidth * 3);
        } else {
            tag_count = 6;
            brushes.setTextSize(bandWidth * 2);
        }
        for (int i = 0; i < tag_count; i++) {
            String is = Integer.toString(i);
            brushes.getTextBounds(is, 0, is.length(), b);
            double angle;
            if (this.currentViewType == ClockViewType.NORMAL) {
                angle = (i * 30 - 90) * (Math.PI / 180);
            } else {
                angle = (i * 60 - 120) * (Math.PI / 180);
            }
            double cx = centreX + Math.cos(angle) * bandWidth * ColourClock.NUMBER_POS;
            double cy = centreY + Math.sin(angle) * bandWidth * ColourClock.NUMBER_POS + b.height() / 2f;
            String text;
            String[] tl;
            switch (this.currentViewType) {
                case NORMAL:
                default:
                    text = String.format(Locale.CHINA, "%d", i == 0 ? 12 : i);
                    break;
                case SHI_CHEN:
                    if (mHours < 12) {
                        tl = new String[]{"子", "丑", "寅", "卯", "辰", "巳"};
                    } else {
                        tl = new String[]{"午", "未", "申", "酉", "戌", "亥"};
                    }
                    text = tl[i];
                    break;
                case JING_LUO:
                    if (mHours < 12) {
                        tl = new String[]{"胆", "肝", "肺", "大肠", "胃", "脾"};
                    } else {
                        tl = new String[]{"心", "小肠", "膀胱", "肾", "心包", "三焦"};
                    }
                    text = tl[i];
                    break;
            }
            tPainting.drawText(text, (float) cx, (float) cy, brushes);
        }
        for (int i = 0; i < 60; i++) {
            double angle = (i * 6 - 90) * (Math.PI / 180);
            double tel = bandWidth / 3;
            double tsl = (i % 5 == 0) ? bandWidth : bandWidth / 2f;
            double tsx = centreX + Math.cos(angle) * (bandWidth * ColourClock.INNER_POS - tel - tsl);
            double tex = centreX + Math.cos(angle) * (bandWidth * ColourClock.INNER_POS - tel);
            double tsy = centreY + Math.sin(angle) * (bandWidth * ColourClock.INNER_POS - tel - tsl);
            double tey = centreY + Math.sin(angle) * (bandWidth * ColourClock.INNER_POS - tel);
            if (i % 5 == 0) {
                brushes.setStrokeWidth(bandWidth * ColourClock.MIN_WIDTH * 0.75f);
            } else {
                brushes.setStrokeWidth(bandWidth * ColourClock.SEC_WIDTH * 0.75f);
            }
            tPainting.drawLine((float) tsx, (float) tsy, (float) tex, (float) tey, brushes);
        }
    }

    public void drawClock() {
        if (!this.drawLock.tryLock()) {
            this.drawStateLock.lock();
            this.dryLock = true;
            this.drawStateLock.unlock();
            return;
        }
        try {
            onDraw = true;
            float hourAng = (mHours * 30); // 360/12
            float minAng = (mMinutes * 6); // 360/60
            float secAng = (mSeconds * 6); // 360/60
            this.painting.drawColor(Color.WHITE); // fill in background
            brushes.setStyle(Paint.Style.STROKE);
            drawCircle(ColourClock.OUTER_POS, 0.125f, Color.WHITE, Color.BLACK, this.painting); // outer face
            drawCircle(ColourClock.INNER_POS, 0.125f, Color.WHITE, Color.BLACK, this.painting); // inner face
            drawNumbers(this.painting);
            brushes.setStyle(Paint.Style.STROKE);
            drawLine(hourAng, ColourClock.HOUR_POS, ColourClock.HOUR_WIDTH, this.painting); // hour hand
            drawCircle(hourAng, ColourClock.HOUR_POS - 2, 1.5f, ColourClock.HOUR_WIDTH, getAngleColour(hourAng), Color.BLACK, this.painting); // hour circle
            drawLine(minAng, ColourClock.MIN_POS, ColourClock.MIN_WIDTH, this.painting); // minute hand
            drawCircle(minAng, ColourClock.MIN_POS - 1.25f, 1f, ColourClock.MIN_WIDTH, getAngleColour(minAng), Color.BLACK, this.painting); // minute circle
            drawLine(secAng, ColourClock.SEC_POS, ColourClock.SEC_WIDTH, this.painting); // second hand
            drawCircle(secAng, ColourClock.SEC_POS - 0.75f, 0.5f, ColourClock.SEC_WIDTH, getAngleColour(secAng), Color.BLACK, this.painting); // second circle
            drawCircle(2, 0.5f, Color.WHITE, Color.BLACK, this.painting); // centre dot
            brushes.setStrokeWidth(1);
            brushes.setStyle(Paint.Style.FILL);

        } finally {
            onDraw = false;
            this.drawLock.unlock();
            this.postInvalidate();
        }
        this.drawStateLock.lock();
        if (this.dryLock) {
            Thread t = new Thread(this);
            t.start();
        }
        this.dryLock = false;
        this.drawStateLock.unlock();
    }

    private void drawCircle(float radiusFactor, float strokeWFactor,
                            int fillCol, int strokeCol, Canvas tPainting) {
        brushes.setStrokeWidth(strokeWFactor * bandWidth);
        brushes.setColor(fillCol);
        brushes.setStyle(Paint.Style.FILL);
        tPainting.drawCircle(centreX, centreY, radiusFactor * bandWidth, brushes);
        brushes.setColor(strokeCol);
        brushes.setStyle(Paint.Style.STROKE);
        tPainting.drawCircle(centreX, centreY, radiusFactor * bandWidth, brushes);
    }

    private void drawCircle(float angle, float lengthFactor, float radiusFactor, float strokeWFactor,
                            int fillCol, int strokeCol, Canvas tPainting) {
        angle = (float) ((angle - 90) * Math.PI / 180);
        double cx = centreX + Math.cos(angle) * lengthFactor * bandWidth;
        double cy = centreY + Math.sin(angle) * lengthFactor * bandWidth;
        brushes.setStrokeWidth(bandWidth * strokeWFactor);
        brushes.setStrokeWidth(strokeWFactor * bandWidth);
        brushes.setColor(fillCol);
        brushes.setStyle(Paint.Style.FILL);
        tPainting.drawCircle((float) cx, (float) cy, radiusFactor * bandWidth, brushes);
        brushes.setColor(strokeCol);
        brushes.setStyle(Paint.Style.STROKE);
        tPainting.drawCircle((float) cx, (float) cy, radiusFactor * bandWidth, brushes);

    }

    private void drawLine(float angle, float lengthFactor, float widthFactor, Canvas tPainting) {
        angle = (float) ((angle - 90) * Math.PI / 180);
        brushes.setStrokeWidth(bandWidth * widthFactor);
        tPainting.drawLine(centreX, centreY,
                (float) (centreX + Math.cos(angle) * bandWidth * lengthFactor),
                (float) (centreY + Math.sin(angle) * bandWidth * lengthFactor), brushes);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.drawLock.tryLock()) {
            this.invalidate();
            return;
        }
        try {
            if (!onDraw) {
                canvas.drawBitmap(backing, 0, 0, null);
            } else {
                this.invalidate();
            }
        } finally {
            this.drawLock.unlock();
        }
    }

    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        backing = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        painting = new Canvas(backing);
        centreX = width / 2f;
        centreY = height / 2f;
        float clockRadius = Math.min(width - 16, height - 16) / 2f;
        bandWidth = clockRadius / 16;
        started = true;
        brushes.setTextSize(bandWidth * 2);
        brushes.setColor(Color.BLACK);
        brushes.setTextAlign(Paint.Align.CENTER);
        brushes.setStrokeCap(Paint.Cap.ROUND);
        Thread t = new Thread(this);
        t.start();
    }

    public void stopTick() {
        /* try to remove all traces of the update threads and stop them from running */
        Log.d(LOG_TAG, "stopTick");
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
//        clockTicker.cancel(true);
//        for (Runnable t : tickerTimer.getQueue()) {
//            tickerTimer.remove(t);
//        }
//        clockTicker = null;
    }

    public void startTick() {
        Log.d(LOG_TAG, "startTick");
        if (updateTimer == null) {
            updateTimer = new Timer();
            updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(started){
                        updateTime();
                    }
                }
            }, 50,50);
        }
    }

    //        if (clockTicker == null) {
//            int refreshRate = 50;
//            clockTicker = tickerTimer.scheduleWithFixedDelay(this,
//                    0, refreshRate, TimeUnit.MILLISECONDS);
//        }
    @Override
    public void onClick(View view) {
        switch (this.currentViewType) {
            case NORMAL:
                this.currentViewType = ClockViewType.SHI_CHEN;
                break;
            case SHI_CHEN:
                this.currentViewType = ClockViewType.JING_LUO;
                break;
            case JING_LUO:
                this.currentViewType = ClockViewType.NORMAL;
                break;
        }
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        this.updateTime();
    }
}