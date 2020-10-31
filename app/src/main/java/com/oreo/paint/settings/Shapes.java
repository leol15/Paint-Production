package com.oreo.paint.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.HashMap;

import com.oreo.paint.actions.AbstractPaintActionExtendsView;
import com.oreo.paint.help.Calculator;

public class Shapes extends AbstractSetting {

    HashMap<Class<? extends AbstractPaintActionExtendsView>, Integer> shapesMap;
    HashMap<Integer, Class<? extends AbstractPaintActionExtendsView>> indexToShape;
    RectF[] shapeBoxes;
    public Shapes() {
        shapesMap = new HashMap<>();
        indexToShape = new HashMap<>();
        for (int i = 0; i < AbstractPaintActionExtendsView.ALL_ACTIONS.length; i++) {
            shapesMap.put(AbstractPaintActionExtendsView.ALL_ACTIONS[i], AbstractPaintActionExtendsView.ACTION_STRING_IDS[i]);
            // a predefined order for shapes
            indexToShape.put(i, AbstractPaintActionExtendsView.ALL_ACTIONS[i]);
        }
        shapeBoxes = new RectF[shapesMap.keySet().size()];
        for (int i = 0; i < shapeBoxes.length; i++) {
            shapeBoxes[i] = new RectF();
        }
    }

    static final int MAIN_MARGIN = 80;
    @Override
    void privateInit() {
        paint.setTextAlign(Paint.Align.CENTER);
        // draw boxes evenly
        float boxSpace = mH / 1.0f / (shapeBoxes.length);
        float boxH = boxSpace * 0.8f;
        for (int i = 0; i < shapeBoxes.length; i++) {
            shapeBoxes[i].set(
                    MAIN_MARGIN,
                    boxSpace * 0.1f + boxSpace * i,
                    mW - MAIN_MARGIN,
                    boxSpace * 0.1f + boxSpace * i + boxH);
        }
    }

    @Override
    public void drawIcon(Canvas canvas) {
        super.drawIcon(canvas);

        Class<? extends AbstractPaintActionExtendsView> action = paper.getCurrentAction();
        if (action == null) {
            return;
        }
        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
        paint.setTextSize(iH * 0.75f);
        int actionStringId = shapesMap.get(action);
        canvas.drawText(paper.getContext().getResources().getString(actionStringId),
                iLeft + iW / 2f, iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2, paint);
    }

    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (inIcon(e.getX(), e.getY())) {
                    START_MAIN_ACTION.run();
                } else {
                    END_MAIN_ACTION.run();
                }
        }
        return true;
    }

    @Override
    public void drawMain(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BACKGROUND_COLOR);
        canvas.drawRect(0, 0, mW, mH, paint);
        paint.setTextSize(shapeBoxes[0].height() * 0.5f);
        for (int i = 0; i < shapeBoxes.length; i++) {
            if (paper.getCurrentAction() == indexToShape.get(i)) {
                paint.setColor(Color.argb(120, 200, 200, 200));
            } else if (nextShapeIndex == i) {
                paint.setColor(Color.argb(160, 200, 200, 200));
            } else {
                paint.setColor(Color.argb(220, 200, 200, 200));
            }
            canvas.drawRoundRect(shapeBoxes[i], 10, 10, paint);

            // draw text
            paint.setColor(Color.BLACK);
            int actionStringId = shapesMap.get(indexToShape.get(i));
            canvas.drawText(paper.getContext().getResources().getString(actionStringId),
                    shapeBoxes[i].centerX(), shapeBoxes[i].centerY() + paint.getTextSize() / 2 - paint.descent() / 2, paint);
        }
    }

    int nextShapeIndex = -1;
    @Override
    public boolean handleMainEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < shapeBoxes.length; i++) {
                    if (shapeBoxes[i].contains(e.getX() - iW, e.getY())) {
                        nextShapeIndex = i;
                        invalidate();
                        break;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (nextShapeIndex != -1) {
                    if (paper.getCurrentAction() != indexToShape.get(nextShapeIndex)) {
                        // change
                        paper.setDrawAction(indexToShape.get(nextShapeIndex));
                    }
                }
                // quit
                END_MAIN_ACTION.run();
                nextShapeIndex = -1;
        }
        return true;
    }
}
