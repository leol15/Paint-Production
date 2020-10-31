package com.oreo.paint.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.oreo.paint.help.Calculator;


public class Backgrounds extends AbstractSetting {

    int COLOR;

    void changeBackgroundColor() {
        paper.setBackgroundColor(COLOR);
    }

    @Override
    void privateInit() {
        paint.setTextSize(iW / 1.3f);
        paint.setTextAlign(Paint.Align.CENTER);
        COLOR = paper.getBackgroundColor();
    }

    @Override
    public void drawIcon(Canvas canvas) {
        super.drawIcon(canvas);
//        canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f, Math.min(iW, iH) / 2f, paint);
        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
        canvas.drawText("\u274f", iLeft + iW / 2f,
                iTop + iH / 2f + paint.descent() + 5, paint);
    }

    float sX, sY;
    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                sX = e.getX();
                skip = false;
            case MotionEvent.ACTION_MOVE:
                if (skip) {
                    break;
                }
                if (e.getX() - sX > mW / 3f) {
                    COLOR = Calculator.CONTRAST_COLOR(paper.getBackgroundColor());
                    paper.setBackgroundColor(COLOR);
                    skip = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (inIcon(e.getX(), e.getY())) {
                    START_MAIN_ACTION.run();
                } else {
                    END_MAIN_ACTION.run();
                }
                skip = false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void drawMain(Canvas canvas) {
        // rg combination
        // b combination
        for (int i = 0; i < mW; i+=10) {
            for (int j = 0; j < mH / 2; j+=10) {
                paint.setColor(Color.argb(
                        Color.alpha(COLOR),
                        (int) Calculator.MAP(i, 0, mW, 0, 255),
                        (int) Calculator.MAP(j, 0, mH / 2f, 0, 255),
                        Color.blue(COLOR)));
                canvas.drawRect(i, j, i + 10, j + 10, paint);
            }
        }
        for (int i = 0; i < mW; i+=10) {
            for (int j = 0; j < mH / 2; j+=10) {
                paint.setColor(Color.argb(
                        (int) Calculator.MAP(i, 0, mW, 0, 255),
                        Color.red(COLOR),
                        Color.green(COLOR),
                        (int) Calculator.MAP(j, 0, mH / 2f, 0, 255)
                        ));
                canvas.drawRect(i, mH / 2f + j, i + 10, mH /2f + j + 10, paint);
            }
        }
        // draw a ring at current color
        paint.setColor(COLOR);
        // top:
        float s = 200;
        float tX = Calculator.MAP(Color.red(COLOR), 0, 255, 0, mW);
        float tY = Calculator.MAP(Color.green(COLOR), 0, 255, 0, mH / 2f);
        canvas.save();
        canvas.clipRect(0,0,mW,mH / 2);
        canvas.clipRect(tX - s, tY - s, tX + s, tY + s);
        s = 150;
        canvas.clipOutRect(tX - s, tY - s, tX + s, tY + s);
        s = 200;
        canvas.drawRect(tX - s, tY - s, tX + s, tY + s, paint);
        canvas.restore();
        // bottom
        tX = Calculator.MAP(Color.alpha(COLOR), 0, 255, 0, mW);
        tY = mH / 2f + Calculator.MAP(Color.blue(COLOR), 0, 255, 0, mH / 2f);
        canvas.save();
        canvas.clipRect(0, mH / 2, mW, mH);
        canvas.clipRect(tX - s, tY - s, tX + s, tY + s);
        s = 150;
        canvas.clipOutRect(tX - s, tY - s, tX + s, tY + s);
        s = 200;
        canvas.drawRect(tX - s, tY - s, tX + s, tY + s, paint);
        canvas.restore();
    }

    boolean skip = false;
    @Override
    public boolean handleMainEvent(MotionEvent e) {
        float x = Math.max(0, Math.min(mW, e.getX() - iW));
        float y = e.getY();
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && x < iW) {
            skip = true;
        }
        if (skip) {
            if (x < iW) {
                if (e.getActionMasked() == MotionEvent.ACTION_UP ) {
                    END_MAIN_ACTION.run();
                    skip = false;
                }
                return true;
            } else {
                skip = false;
            }
        }
        if (y < mH / 2f) {
            // top
            COLOR = Color.argb(
                    Color.alpha(COLOR),
                    (int) Calculator.MAP(x, 0, mW, 0, 255),
                    (int) Calculator.MAP(y, 0, mH / 2f, 0, 255),
                    Color.blue(COLOR)
                    );
        } else {
            COLOR = Color.argb(
                    (int) Calculator.MAP(x, 0, mW, 0, 255),
                    Color.red(COLOR),
                    Color.green(COLOR),
                    (int) Calculator.MAP(y - mH / 2f, 0, mH / 2f, 0, 255)
            );


        }
        invalidate();
        changeBackgroundColor();
        return true;
    }
}
