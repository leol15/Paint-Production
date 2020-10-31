package com.oreo.paint.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.view.MotionEvent;

import java.util.HashMap;

import com.oreo.paint.R;
import com.oreo.paint.actions.AbstractPaintActionExtendsView;
import com.oreo.paint.help.Calculator;


/**
 * a draggable FAB that toggles
 */
public class Fab extends AbstractSetting {

    int iconRadius;
    HashMap<Class<? extends AbstractPaintActionExtendsView>, Integer> shapesMap;
    PathEffect DASH_PATH_EFFECT;
    @Override
    void privateInit() {
        iconRadius = Math.min(iW, iH) / 2;
        paint.setTextSize(iconRadius / 1.7f);
        paint.setTextAlign(Paint.Align.CENTER);

        shapesMap = new HashMap<>();
        for (int i = 0; i < AbstractPaintActionExtendsView.ALL_ACTIONS.length; i++) {
            shapesMap.put(AbstractPaintActionExtendsView.ALL_ACTIONS[i], AbstractPaintActionExtendsView.ACTION_STRING_IDS[i]);
        }
        DASH_PATH_EFFECT = new DashPathEffect(new float[]{10, 20},0);

        paper.postDelayed(this::snapToEdge, 2300);
//        snapToEdge();
    }

    int iconAlpha = 0;
    int targetIconAlpha = 111;
    @Override
    public void drawIcon(Canvas canvas) {
        super.drawIcon(canvas);

        // draw pie menu
        if (pieing) {
            // draw a big circle
            // and small sections
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
            paint.setAlpha(70);
            canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f, iconRadius * 4, paint);
            paint.setAlpha(140);
            // draw selected action
            if (pieAction == 0) {
                // change color
                canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f - iconRadius * 2.5f, iconRadius, paint);
            } else if (pieAction == 1) {
                canvas.drawCircle(iLeft + iW / 2f + iconRadius * 2.5f,
                        iTop + iH / 2f, iconRadius, paint);
            } else if (pieAction == 2) {
                canvas.drawCircle(iLeft + iW / 2f,
                        iTop + iH / 2f + iconRadius * 2.5f,
                        iconRadius, paint);
            } else if (pieAction == 3) {
                canvas.drawCircle(iLeft + iW / 2f - iconRadius * 2.5f,
                        iTop + iH / 2f, iconRadius, paint);
            }

            // draw names
            paint.setAlpha(255);
            paint.setTextSize(iconRadius);
            // left and right texts
            canvas.drawText("\u27f2", iLeft + iW / 2f - iconRadius * 2.5f,
                    iTop + iH / 2f + paint.getTextSize() / 2.8f - paint.descent() / 2, paint);
            canvas.drawText("\u27f2", iLeft + iW / 2f + iconRadius * 2.5f,
                    iTop + iH / 2f + paint.getTextSize() / 2.8f - paint.descent() / 2, paint);
            // down and up things
            canvas.drawText(paper.getContext().getResources().getString(shapesMap.get(paper.getPreviousAction())),
                    iLeft + iW / 2f,
                    iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2 + iconRadius * 2.5f, paint);
            paint.setColor(paper.getPreviousColor());
            canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f - iconRadius * 2.5f, iconRadius / 2f, paint);

        }

        paint.setTextSize(iconRadius / 1.7f);
        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
        paint.setAlpha(iconAlpha);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(iconRadius / 3f);
        canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f, iconRadius - paint.getStrokeWidth(), paint);

        // show color
        paint.setColor(paper.getPaintToEdit().getColor());
        paint.setAlpha(iconAlpha);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f, iconRadius - paint.getStrokeWidth() / 2, paint);

        // show state
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()) == Color.WHITE ? Color.BLACK : Color.WHITE);
        if (paper.isPanning()) {
            paint.setStrokeWidth(8);
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(DASH_PATH_EFFECT);
            canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f,iconRadius / 2f,paint);
            paint.setPathEffect(null);
        } else if (paper.isErasing()) {
            canvas.drawText(paper.getContext().getResources().getString(R.string.erase),
                    iLeft + iW / 2f, iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2, paint);
        } else {
            // show shapes
            canvas.drawText(paper.getContext().getResources().getString(shapesMap.get(paper.getCurrentAction())),
                    iLeft + iW / 2f, iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2, paint);
        }

        // draw dragging indicator
        if (dragging) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
            paint.setAlpha(120);
            canvas.drawCircle(iLeft + iW / 2f, iTop + iH / 2f, iconRadius * 2, paint);
        }


        if (snapToEdge) {
            snapToEdge();
        }
        if (iconAlpha != targetIconAlpha) {
            iconAlpha += (targetIconAlpha - iconAlpha) * 0.2;
            if (targetIconAlpha > iconAlpha) {
                iconAlpha += 1;
            } else {
                iconAlpha -= 1;
            }
            invalidate();
        }
    }

    @Override
    public boolean inIcon(float xPos, float yPos) {
        return Calculator.DIST(xPos, yPos, iLeft + iW / 2f, iTop + iH / 2f) < Math.min(iW, iH) / 2f + 40; // 40 spare
    }

    // pieAction
    int pieAction = -1; // 0 last color, 1/3 undo, 2 last shape
    float sX, sY;
    boolean dragging, pieing, quickActionOver;
    long touchDownTime;
    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        // click vs drag?
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                snapToEdge = false;
                quickActionOver = false;
                sX = e.getX();
                sY = e.getY();
                targetIconAlpha = 240;
                touchDownTime = System.currentTimeMillis();
//                paper.postDelayed(() -> {
//                    if (!quickActionOver && !dragging && !pieing) {
//                        dragging = true;
//                        invalidate();
//                    }
//                },1000);
                invalidate();
            case MotionEvent.ACTION_MOVE:
                if (Calculator.DIST(sX, sY, e.getX(), e.getY()) > 50 && !(dragging || pieing)) {
                    if (System.currentTimeMillis() - touchDownTime > 500) {
                        dragging = true;
                    } else {
                        pieing = true;
                    }
                }
                if (dragging) {
                    iLeft = (int) (e.getX() - iW / 2);
                    iTop = (int) (e.getY() - iH / 2);
                    invalidate();
                } else if (pieing) {
                    // update pieAction
                    if (Calculator.DIST(iLeft + iW /2f,iTop + iH / 2f,e.getX(), e.getY()) < iconRadius) {
                        pieAction = -1;
                    } else {
                        double angle = Math.atan2(e.getY() - (iTop + iH / 2f), e.getX() - (iLeft + iW / 2f));
                        if (Math.abs(angle) < Math.PI / 4) {
                            // undo
                            pieAction = 1;
                        } else if (Math.PI - Math.abs(angle) < Math.PI / 4) {
                            pieAction = 3;
                        } else if (angle < 0) {
                            // color
                            pieAction = 0;
                        } else {
                            pieAction = 2;
                        }
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                // decide
                if (!(dragging || pieing)) {
                    toggle.run();
                }
                if (pieing) {
                    // select an action here
                    // todo
                    switch (pieAction) {
                        case 0:
                            paper.getPaintToEdit().setColor(paper.getPreviousColor());
                            paper.applyPaintEdit();
                            // also change color of Colors???? todo
                            break;
                        case 1:
                        case 3:
                            paper.undo();
                            break;
                        case 2:
                            paper.setDrawAction(paper.getPreviousAction());
                        default:
                    }
                    pieAction = -1;
                    invalidate();
                }
                dragging = false;
                pieing = false;
                quickActionOver = true;
                snapToEdge();
                paper.postDelayed(() -> {
                    if (quickActionOver) {
                        targetIconAlpha = 111;
                        invalidate();
                    }
                }, 1000);
                END_MAIN_ACTION.run();
        }
        return true;
    }

    boolean snapToEdge;
    void snapToEdge() {
        float dist = iLeft + iW / 2f - (mW + 80) / 2f;
        if (Math.abs(dist) > (mW + 80) / 2f * 0.7) {
            if (iLeft + iW / 2 < 10 || Math.abs(iLeft + iW / 2) > mW + 80 - 10) { // 10 is extra out from side
                // snapped into position, stop
                snapToEdge = false;
            } else {
                snapToEdge = true;
                // snapping
                if (dist > 0) {
                    iLeft += Math.abs((mW + 80) / 2f - dist) * 0.2 + 1;
                } else {
                    iLeft -= ((mW + 80) / 2 + dist) * 0.2 + 1;
                }
                invalidate();
            }
        } else {
            snapToEdge = false;
        }
    }

    @Override
    public void drawMain(Canvas canvas) {

    }

    @Override
    public boolean handleMainEvent(MotionEvent e) {
        END_MAIN_ACTION.run();
        return false;
    }

    Runnable toggle;
    public void setToggleWork(Runnable r) {
        toggle = r;
    }
}
