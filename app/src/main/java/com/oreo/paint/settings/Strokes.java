package com.oreo.paint.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.oreo.paint.help.Calculator;

public class Strokes extends AbstractSetting {

    static final float MIN_STROKE_W = 5;

    float STROKE_WIDTH = 10;
    Paint.Style STROKE_STYLE;
    RectF iconBox;
    RectF[] strokeStyleBox;

    public Strokes() {
        iconBox = new RectF();
        strokeStyleBox = new RectF[3];
        for (int i = 0; i < strokeStyleBox.length; i++) {
            strokeStyleBox[i] = new RectF();
        }
    }

    void changeStroke() {
        paper.getPaintToEdit().setStrokeWidth(STROKE_WIDTH);
        paper.getPaintToEdit().setStyle(STROKE_STYLE);
        paper.applyPaintEdit();
    }

    static final int MAIN_MARGIN = 50;
    @Override
    void privateInit() {
        paint.setStrokeWidth(10);
        paint.setTextAlign(Paint.Align.CENTER);
        iconBox.set(iLeft, iTop, iLeft + iW, iTop + iH);
        for (int i = 0; i < strokeStyleBox.length; i++) {
            strokeStyleBox[i].set(
                    MAIN_MARGIN,
                    MAIN_MARGIN + i * mH / 5f,
                    mW - MAIN_MARGIN,
                    MAIN_MARGIN + i * mH / 5f + mH / 8f);
        }
        STROKE_STYLE = paper.getPaintToEdit().getStyle();
        paint.setTextSize(strokeStyleBox[0].height() / 3);
    }

    @Override
    public void drawIcon(Canvas canvas) {
        super.drawIcon(canvas);

        // draw an animated size
        if (quickActionActive) {
            paint.setColor(BACKGROUND_COLOR);
            paint.setAlpha(80);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, mW + iW, mH, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(iW + mW / 2f, mH / 2f, STROKE_WIDTH / 2, paint);
        }

        // draw icon
//        paint.setStyle(Paint.Style.FILL);
//        paint.setColor(getContrastColor(paper.getBackgroundColor()));
//        paint.setAlpha(200);
//        canvas.drawCircle(iconBox.centerX(), iconBox.centerY(), iW / 3f, paint);

        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
        paint.setStyle(STROKE_STYLE);
        canvas.drawCircle(iconBox.centerX(), iconBox.centerY(), iW / 3f, paint);

    }

    float cX, cY;
    boolean quickActionActive;

    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cX = e.getX();
                cY = e.getY();
                quickActionActive = true;
            case MotionEvent.ACTION_MOVE:
                float delta = (e.getY() - cY) * 0.1f;
                STROKE_WIDTH = Math.max(MIN_STROKE_W, STROKE_WIDTH - delta);
                changeStroke();
                cX = e.getX();
                cY = e.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (iconBox.contains(e.getX(), e.getY())) {
                    START_MAIN_ACTION.run();
                } else {
                    END_MAIN_ACTION.run();
                }
                quickActionActive = false;
        }
        return true;
    }

    @Override
    public void drawMain(Canvas canvas) {
        // select stroke style
        paint.setColor(BACKGROUND_COLOR);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, mW, mH, paint);
        // draw rects
        for (int i = 0; i < strokeStyleBox.length; i++) {
            if (i == 0) paint.setStyle(Paint.Style.FILL);
            if (i == 1) paint.setStyle(Paint.Style.STROKE);
            if (i == 2) paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setColor(Color.rgb(200, 200, 200));
            canvas.drawRect(strokeStyleBox[i], paint);
            paint.setColor(Color.BLACK);
            if (i == 0)
                canvas.drawText("FILL ONLY", strokeStyleBox[i].centerX(),
                        strokeStyleBox[i].centerY() + paint.getTextSize() / 2 - paint.descent() / 2, paint);
            if (i == 1)
                canvas.drawText("STROKE ONLY", strokeStyleBox[i].centerX(),
                        strokeStyleBox[i].centerY() + paint.getTextSize() / 2 - paint.descent() / 2, paint);
            if (i == 2)
                canvas.drawText("FILL n STROKE", strokeStyleBox[i].centerX(),
                        strokeStyleBox[i].centerY() + paint.getTextSize() / 2 - paint.descent() / 2, paint);
            if (paint.getStyle() == STROKE_STYLE) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(strokeStyleBox[i], paint);
            }
        }
        // draw stroke size
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(strokeStyleBox[0].centerX(), (strokeStyleBox[2].bottom + mH) / 2, STROKE_WIDTH / 2, paint);
        if (nextStyleIndex == 4) {
            // changing size
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(strokeStyleBox[0].centerX(), (strokeStyleBox[2].bottom + mH) / 2, STROKE_WIDTH / 2 + 40, paint);
        }
    }

    int nextStyleIndex = -1;
    @Override
    public boolean handleMainEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (nextStyleIndex != 4) {
                    for (int i = 0; i < strokeStyleBox.length; i++) {
                        if (strokeStyleBox[i].contains(e.getX() - iW,e.getY())) {
                            nextStyleIndex = i;
                            if (nextStyleIndex == 0) STROKE_STYLE = Paint.Style.FILL;
                            if (nextStyleIndex == 1) STROKE_STYLE = Paint.Style.STROKE;
                            if (nextStyleIndex == 2) STROKE_STYLE = Paint.Style.FILL_AND_STROKE;
                            nextStyleIndex = -1;
                            changeStroke();
                            invalidate();
                            break;
                        }
                    }
                }
                if (nextStyleIndex == -1) {
                    // might be changing size
                    if (Calculator.DIST(e.getX() - iW, e.getY(),
                            strokeStyleBox[0].centerX(), (strokeStyleBox[2].bottom + mH) / 2)
                            < STROKE_WIDTH / 2 + 40) {
                        nextStyleIndex = 4;
                    }
                }
                if (nextStyleIndex == 4) {
                    // changing size
                    STROKE_WIDTH = Math.abs(e.getY() - (strokeStyleBox[2].bottom + mH) / 2) / 2;
                    STROKE_WIDTH = Math.max(MIN_STROKE_W, STROKE_WIDTH);
                    changeStroke();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (nextStyleIndex == -1) {
                    // quit
                    END_MAIN_ACTION.run();
                }
                nextStyleIndex = -1;
                invalidate();
        }
        return true;
    }
}
