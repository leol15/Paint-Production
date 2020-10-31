package com.oreo.paint.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.oreo.paint.Paper;
import com.oreo.paint.help.Calculator;


/**
 * knobs and dials for paper and paint objects
 */
public abstract class AbstractSetting { // ????????????????

    static final int BACKGROUND_COLOR = Color.argb(150, 0, 0, 0);

    /**
     * set the paper that this setting works on
     * @param paper the paper to be setted
     */
    Paper paper;
    Paint paint;
    int iW, iH, mW, mH, iTop, iLeft;
    public void init(Paper paper, int iconW, int iconH, int mainW, int mainH, int iconTop, int iconLeft) {
        paint = new Paint();
        paint.setAntiAlias(true);
        this.paper = paper;
        iW = iconW;
        iH = iconH;
        mW = mainW;
        mH = mainH;
        iTop = iconTop;
        iLeft = iconLeft;
        privateInit();
    }

    /**
     * where init for certain classes happens
     */
    abstract void privateInit();

    /**
     * draw the setting icon
     */
    public void drawIcon(Canvas canvas) {
        // BACKGROUND & ANIMATION ONLY
//        paint.setColor(Color.argb(100, 0, 0, 0));
//        canvas.drawRoundRect(iLeft, iTop, iLeft + iW, iTop + iH, 5, 5, paint);
        if (SU_clickSize < 100) {
            // animate touch
            paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
            paint.setAlpha((int) Calculator.MAP(SU_clickSize,0,100,180, 20));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(SU_posX, SU_posY, SU_clickSize, paint);
            SU_clickSize += 0.1 * (100 - SU_clickSize) + 1;
            invalidate();
        }
    }

    float SU_posX, SU_posY;
    float SU_clickSize = 999;
    /**
     * quick event on the Icon
     */
    public boolean handleQuickEvent(MotionEvent e) {
        // ANIMATION ONLY
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            SU_posX = e.getX();
            SU_posY = e.getY();
            SU_clickSize = 0;
            invalidate();
        }
        return false;
    }

    /**
     * draw the whole UI
     */
    public abstract void drawMain(Canvas canvas);

    /**
     * interact with tool
     */
    public abstract boolean handleMainEvent(MotionEvent e);

    // taken care of paperController
    View PARENT_VIEW;
    public void setView(View v) {
        PARENT_VIEW = v;
    }
    public void invalidate() {
        PARENT_VIEW.invalidate();
    }
    Runnable START_MAIN_ACTION;
    public void setStartMainAction(Runnable r) {
        START_MAIN_ACTION = r;
    }
    Runnable END_MAIN_ACTION;
    public void setEndMainAction(Runnable r) {
        END_MAIN_ACTION = r;
    }


    public boolean inIcon(float xPos, float yPos) {
        return xPos >= iLeft && xPos <= iLeft + iW &&
                yPos >= iTop && yPos <= iTop + iH;
    }

}
