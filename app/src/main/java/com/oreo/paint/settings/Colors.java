package com.oreo.paint.settings;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.oreo.paint.help.Calculator;
import com.oreo.paint.help.KeyValManager;

public class Colors extends AbstractSetting {
    static final String TAG = "[][] Color Settings";
    static final String COLOR_STORAGE_KEY = "color setting";

    static final int BOX_GAP = 20;

    String[] DEFAULT_COLORS = new String[]{
            "#ccaf00fa", "#ccf23838", "#ccffa600", "#ccfffb00", "#cc04d400", "#cc0080db", "#ccf3f3f3", "#cc03fce3"
    };

    int[] colors;
    RectF[] colorBoxes;

    public Colors(int numColors) {
        colors = new int[numColors];
        colorBoxes = new RectF[numColors];
        for (int i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i] = new RectF();
            if (i < DEFAULT_COLORS.length) {
                colors[i] = Color.parseColor(DEFAULT_COLORS[i]);
            }
            // save or retrieve colors
            Object savedColor = KeyValManager.get(COLOR_STORAGE_KEY + i, 0);
            if (savedColor == null || (Integer) savedColor == -1) {
                KeyValManager.put(COLOR_STORAGE_KEY + i, colors[i]);
            } else {
                colors[i] = (Integer) savedColor;
            }
        }
        mainColorBoxes = new RectF[4];
        for (int i = 0; i < mainColorBoxes.length; i++) {
            mainColorBoxes[i] = new RectF();
        }
    }

    @Override
    void privateInit() {
        paint.setStrokeWidth(10);
        X_OFFSET_AMOUNT = iW * 0.4f;
        float boxH = iH - BOX_GAP * (colorBoxes.length - 1);
        boxH /= colorBoxes.length;
        if (boxH <= 10) {
            Log.e(TAG, "privateInit: box width too small");
        }
        for (int i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i].set(
                    iLeft,
                    iTop + i * (boxH + BOX_GAP),
                    iLeft + iW / 1.9f,
                    iTop + i * (boxH + BOX_GAP) + boxH);
        }
        for (int i = 0; i < mainColorBoxes.length; i++) {
            mainColorBoxes[i].set(MAIN_MARGIN, mH * 0.6f + i * 100, mW - MAIN_MARGIN, mH * 0.6f + i * 100 + 40);
        }
        changeColor();
    }

    void changeColor() {
        paint.setStyle(Paint.Style.FILL);
        paper.getPaintToEdit().setColor(colors[currColorIndex]);
        paper.applyPaintEdit();
        // save color as well
        KeyValManager.put(COLOR_STORAGE_KEY + currColorIndex, colors[currColorIndex]);
    }

    float selectedColorXOff;
    float X_OFFSET_AMOUNT;
    @Override
    public void drawIcon(Canvas canvas) {
        // draw the color boxes
        super.drawIcon(canvas);

        paint.setStyle(Paint.Style.FILL);
        if (colors[currColorIndex] != paper.getPaintToEdit().getColor()) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == paper.getPaintToEdit().getColor()) {
                    prevColorIndex = currColorIndex;
                    currColorIndex = i;
                    nextColorBoxIndex = currColorIndex;
                    break;
                }
            }
        }
        for (int i = 0; i < colorBoxes.length; i++) {
            paint.setColor(colors[i]);
            canvas.save();
            if (i == currColorIndex) {
                if (colors[currColorIndex] != paper.getPaintToEdit().getColor()) {
                    // do nothing
                } else {
                    canvas.translate(selectedColorXOff, 0);
                }
            } else if (i == prevColorIndex && selectedColorXOff < X_OFFSET_AMOUNT) {
                canvas.translate(X_OFFSET_AMOUNT - selectedColorXOff, 0);
            } else if (i == nextColorBoxIndex) {
                canvas.translate(selectedColorXOff / 2, 0);
            }
            canvas.drawRoundRect(colorBoxes[i], iW, iW, paint);
            canvas.restore();
        }
        if (selectedColorXOff < X_OFFSET_AMOUNT) {
            float diff = X_OFFSET_AMOUNT - selectedColorXOff;
            selectedColorXOff += 0.4 * diff + (diff > 0 ? 1 : -1);
            invalidate();
        }

    }

    int nextColorBoxIndex;
    int currColorIndex;
    int prevColorIndex;

    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < colorBoxes.length; i++) {
                    if (colorBoxes[i].contains(colorBoxes[i].centerX(), e.getY())) {
                        nextColorBoxIndex = i;
                        invalidate();
                        return true;
                    }
                }
                nextColorBoxIndex = -1;
                return true;
            case MotionEvent.ACTION_UP:
                if (nextColorBoxIndex != -1 && nextColorBoxIndex != currColorIndex) {
                    prevColorIndex = currColorIndex;
                    currColorIndex = nextColorBoxIndex;
                    selectedColorXOff = 0;
                    changeColor();
                    END_MAIN_ACTION.run();
                } else {
                    if (nextColorBoxIndex != -1) {
                        // edit color
                        START_MAIN_ACTION.run();
                    } else {
                        END_MAIN_ACTION.run();
                    }
                }
                invalidate();
                return true;
            default:
                return false;
        }
    }

    RectF[] mainColorBoxes;
    static final int MAIN_MARGIN = 80;
    @Override
    public void drawMain(Canvas canvas) {
        paint.setColor(BACKGROUND_COLOR);
        canvas.drawRect(0, 0, mW, mH, paint);
        paint.setColor(colors[currColorIndex]);
        canvas.drawRect(0, 0, mW, mH / 2f, paint);
        for (int i = MAIN_MARGIN; i < mW - MAIN_MARGIN; i+=16) {
            for (int j = MAIN_MARGIN; j < mH / 2 - MAIN_MARGIN; j+=16) {
                paint.setColor(Color.argb(
                        Color.alpha(colors[currColorIndex]),
                        (int) Calculator.MAP(i, MAIN_MARGIN, mW - MAIN_MARGIN, 0, 255),
                        (int) Calculator.MAP(j, MAIN_MARGIN, mH / 2f - MAIN_MARGIN, 0, 255),
                        Color.blue(colors[currColorIndex])));
                canvas.drawRect(i, j, i + 16, j + 16, paint);
            }
        }
        // draw where the current point in
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(
                Calculator.MAP(Color.red(colors[currColorIndex]), 0, 255, MAIN_MARGIN, mW - MAIN_MARGIN),
                Calculator.MAP(Color.green(colors[currColorIndex]), 0, 255, MAIN_MARGIN, mH / 2f - MAIN_MARGIN), 20, paint);
        // draw interactor
        for (int i = 0; i < 4; i++) {
            float buttonXPos;
            switch (i) {
                case 0:
                    paint.setColor(Color.RED);
                    buttonXPos = MAIN_MARGIN + Color.red(colors[currColorIndex]) * (mW - MAIN_MARGIN * 2) / 255f;
                    break;
                case 1:
                    paint.setColor(Color.GREEN);
                    buttonXPos = MAIN_MARGIN + Color.green(colors[currColorIndex]) * (mW - MAIN_MARGIN * 2) / 255f;
                    break;
                case 2:
                    paint.setColor(Color.BLUE);
                    buttonXPos = MAIN_MARGIN + Color.blue(colors[currColorIndex]) * (mW - MAIN_MARGIN * 2) / 255f;
                    break;
                default:
                    buttonXPos = MAIN_MARGIN + Color.alpha(colors[currColorIndex]) * (mW - MAIN_MARGIN * 2) / 255f;
                    paint.setColor(Color.BLACK);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(mainColorBoxes[i], 10, 10, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(buttonXPos, mainColorBoxes[i].centerY(), 30, paint);
        }
    }

    int movingColorBoxIndex = -1;
    boolean skipping = false;

    @Override
    public boolean handleMainEvent(MotionEvent e) {
        if (skipping) {
            if (e.getPointerCount() == 1 && e.getActionMasked() == MotionEvent.ACTION_UP) {
                skipping = false;
                END_MAIN_ACTION.run();
            }
            return true;
        }
        float eX = e.getX() - iW;
        float eY = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (eX < 0) {
                    movingColorBoxIndex = -1;
                    skipping = true;
                    return true;
                }
            case MotionEvent.ACTION_MOVE:
                if (movingColorBoxIndex == -1) {
                    // which one?
                    for (int i = 0; i < mainColorBoxes.length; i++) {
                        if (mainColorBoxes[i].contains(eX + 20, eY + 20, eX - 20, eY - 20)) {
                            movingColorBoxIndex = i;
                            break;
                        }
                    }
                    // could be selecting from gradient
                    if (eY < mH / 2f) {
                        movingColorBoxIndex = 4;
                    }
                }

                if (movingColorBoxIndex != -1) {
                    // move it
                    int co = colors[currColorIndex];
                    int newColComponent = (int) Math.max(0, Math.min(255, (eX - MAIN_MARGIN) * 255 / (mW - MAIN_MARGIN * 2)));
                    switch (movingColorBoxIndex) {
                        case 0:
                            colors[currColorIndex] = Color.argb(Color.alpha(co), newColComponent, Color.green(co), Color.blue(co));
                            break;
                        case 1:
                            colors[currColorIndex] = Color.argb(Color.alpha(co), Color.red(co), newColComponent, Color.blue(co));
                            break;
                        case 2:
                            colors[currColorIndex] = Color.argb(Color.alpha(co), Color.red(co), Color.green(co), newColComponent);
                            break;
                        case 3:
                            colors[currColorIndex] = Color.argb(newColComponent, Color.red(co), Color.green(co), Color.blue(co));
                            break;
                        default: // from gradient
                            int boundX = (int) Math.max(MAIN_MARGIN, Math.min(mW - MAIN_MARGIN, eX));
                            int boundY = (int) Math.max(MAIN_MARGIN, Math.min(mH / 2f - MAIN_MARGIN, eY));
                            boundX = (int) Calculator.MAP(boundX, MAIN_MARGIN, mW - MAIN_MARGIN, 0, 255);
                            boundY = (int) Calculator.MAP(boundY, MAIN_MARGIN, mH / 2f - MAIN_MARGIN, 0, 255);
                            colors[currColorIndex] = Color.argb(Color.alpha(co), boundX, boundY, Color.blue(co));
                    }
                    invalidate();
                }
                // else quit
                break;
            case MotionEvent.ACTION_UP:
                changeColor();
                if (movingColorBoxIndex == -1) {
                    END_MAIN_ACTION.run();
                }
                movingColorBoxIndex = -1;
        }
        return true;
    }

}
