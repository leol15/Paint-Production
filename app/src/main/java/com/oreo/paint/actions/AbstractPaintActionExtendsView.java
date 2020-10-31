package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.view.MotionEvent;
import android.view.View;

import com.oreo.paint.R;
import com.oreo.paint.help.Calculator;
import com.oreo.paint.help.InterestingPoints;

import java.util.function.UnaryOperator;

/**
 * parent of all actions
 * a view that should be added, removed from paper
 */
public abstract class AbstractPaintActionExtendsView extends View {
    static final String TAG = "-=-= Abstract Action";

    // snap to interesting points
    static InterestingPoints interestingPoints;
    public void setInterestingPoints(InterestingPoints ip) {
        interestingPoints = ip;
    }
    public void addAllInterestingPoints() {

    }
    public void removeAllInterestingPoints() {
        interestingPoints.removeAllPoints(this);
    }

    public static final Class<? extends AbstractPaintActionExtendsView>[] ALL_ACTIONS = new Class[]{
//            ActionLetters.class,
            ActionNumbers.class,
            ActionOval.class,
            ActionRectangle.class,
            ActionTriangle.class,
            ActionStroke.class,
            ActionPen.class,
            ActionDash.class,
            ActionStraightLine.class,
            ActionArrow.class
//            ActionTest.class
    };

    public static final int[] ACTION_STRING_IDS = new int[]{
//            R.string.letter,
            R.string.number,
            R.string.oval,
            R.string.rect,
            R.string.triangle,
            R.string.stroke,
            R.string.pen,
            R.string.dash,
            R.string.line,
            R.string.arrow
//            R.string.test_action

    };

    static final int EDIT_TOUCH_RADIUS = 40;

    static final float HIGHLIGHT_STROKE_WIDTH = 2;
    static final int HIGHLIGHT_ALPHA = 125;
    static final int SNAP_DELTA = 10;

    // for multi-select
    // if one action is DRAGGING a point, instead of
    // everybody shifting, we skip and don't shift
    public static boolean GROUP_SELECTED = false;
    static boolean SHIFTING_LOCK = false;
    static Object SHIFTING_LOCKER = null;

    static Paint abstractActionPaint;

    // for all shapes
    Path containsPath;
    Path myPath;
    public AbstractPaintActionExtendsView(Context context) {
        super(context);
        if (abstractActionPaint == null) {
            abstractActionPaint = new Paint();
            abstractActionPaint.setStyle(Paint.Style.STROKE);
            abstractActionPaint.setStrokeWidth(4);
            abstractActionPaint.setColor(Color.GREEN);
            abstractActionPaint.setStrokeCap(Paint.Cap.ROUND);

//            abstractActionPaint.setXfermode(HIGHLIGHT_PAINT_MODE);
            abstractActionPaint.setTextAlign(Paint.Align.CENTER);
            abstractActionPaint.setTextSize(quickBoxWidth / 1.5f);
        }
        currentState = ActionState.NEW;
        containsPath = new Path();
        myPath = new Path();
    }

    static RectF quickEditBox;
    static final int quickBoxWidth = 70;
    // draw some quick clickable actions

    @Override
    protected void onDraw(Canvas canvas) {
        // default drawings
        // draw action box
        if (!shouldShowQuickAction ||
                currentState == ActionState.NEW ||
                currentState == ActionState.STARTED) {
            return;
        }
        if (quickEditBox == null) {
            quickEditBox = new RectF(getWidth() - quickBoxWidth ,
                    getHeight() / 2f - quickBoxWidth,
                    getWidth(),
                    getHeight() / 2f + quickBoxWidth);
        }


        if (animateQuickButton) {
            canvas.save();
            canvas.clipRect(animateBounds);
            abstractActionPaint.setAlpha(animateQBAlpha);
            abstractActionPaint.setStyle(Paint.Style.FILL);
//            abstractActionPaint.setXfermode(null);
            canvas.drawCircle(animateX, animateY, animateQBRadius, abstractActionPaint);
            canvas.restore();
            if (animateQBRadius < quickBoxWidth * 2) {
                animateQBRadius += (quickBoxWidth * 2 - animateQBRadius) * 0.08 + 1;
                animateQBAlpha = (int) (255 - 255 * (animateQBRadius / (quickBoxWidth * 2)));
                invalidate();
            }
        }

//        abstractActionPaint.setXfermode(HIGHLIGHT_PAINT_MODE);
        abstractActionPaint.setStyle(Paint.Style.STROKE);
        abstractActionPaint.setAlpha(255);
        canvas.drawRect(quickEditBox, abstractActionPaint);
        if (currentState == ActionState.FINISHED) {
            canvas.drawText("\u2704",
                    quickEditBox.centerX(),
                    quickEditBox.centerY() + abstractActionPaint.descent(),
                    abstractActionPaint);
        } else {
            canvas.drawText("\u2713",
                    quickEditBox.centerX(),
                    quickEditBox.centerY() + abstractActionPaint.descent(),
                    abstractActionPaint);
        }

//        canvas.drawRect(toggleFillBox, abstractActionPaint);
//        canvas.drawText("\u176e", toggleFillBox.centerX(), toggleFillBox.centerY(), abstractActionPaint);

    }



    boolean abstractSkippingEvent;
    /**
     * main way to draw/interact with user
     *
     * @param e the touch event
     */
    public boolean handleTouch(MotionEvent e) {
        // handle click on quick box
        if (abstractSkippingEvent) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                abstractSkippingEvent = false;
            }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN &&
            e.getPointerCount() == 1) {
            if (actionBoxTouched(e.getX(), e.getY())) {
                if (currentState == ActionState.FINISHED) {
                    currentState = ActionState.REVISING;
                    abstractSkippingEvent = true;
                    startButtonClickedAnimation(quickEditBox, e.getX(), e.getY());
                    return true;
                } else if (currentState == ActionState.REVISING) {
                    currentState = ActionState.FINISHED;
                    abstractSkippingEvent = true;
                    startButtonClickedAnimation(quickEditBox, e.getX(), e.getY());
                    return true;
                    // skip this event
                }
            }
        }

//        if (SHIFTING_LOCK && SHIFTING_LOCKER != this) {
//            return true;
//        }
        // leave to Actions to handle this
        return false;
    }

    /**
     * set the styles, ie color, thickness, ...
     * @param p Paint that has those info
     */
    public void setStyle(Paint p) {
        invalidate();
        abstractActionPaint.setColor(p.getColor());
    }

    // currState -> FINISHED
    // shouldShowQuickAction -> false
    UnaryOperator<AbstractPaintActionExtendsView> callWhenDone;
    UnaryOperator<AbstractPaintActionExtendsView> callWhenDoneSuper;

    public void setOnCompletion(UnaryOperator<AbstractPaintActionExtendsView> calledWhenDone) {
        callWhenDoneSuper = calledWhenDone;
        callWhenDone = abstractPaintActionExtendsView -> {
            callWhenDoneSuper.apply(this);
            currentState = ActionState.FINISHED;
            shouldShowQuickAction = false;
            invalidate();
            return null;
        };

    }

    /**
     * switch to edit, then call callWhenDone
     */
    ActionState currentState;
    boolean shouldShowQuickAction = true;
    public void editButtonClicked() {
        switch (currentState) {
            case NEW:
                return;
            case STARTED:
            case FINISHED:
                currentState = ActionState.REVISING;
                shouldShowQuickAction = true;
                break;
            case REVISING:
                callWhenDone.apply(this);
        }
        invalidate();
    }

    /**
     * change state to FINISHED
     * @return true if there is some thing in this action
     *      false if this is empty
     */
    public boolean focusLost() {
        shouldShowQuickAction = false;
        if (currentState == ActionState.NEW) {
            return false;
        } else {
            currentState = ActionState.FINISHED;
            invalidate();
            return true;
        }
    }

    public void removingFromView() {
        removeAllInterestingPoints();
    }

    public void addingToView() {
        addAllInterestingPoints();
    }

    /**
     * get state
     * @return the current state
     */
    public ActionState getCurrentState() {
        return currentState;
    }

    /**
     * test if a point touches actual content of action
     * @param x,y location
     * @param radius the bound to allow
     */
    public boolean contains(float x, float y, float radius) {
        containsPath.rewind();
        containsPath.moveTo(x, y);
        containsPath.addCircle(x, y, radius, Path.Direction.CW);
        return touchesPath(containsPath);
    }

    // touches by path: group select
    public boolean touchesPath(Path origin) {
        return containsPath.op(myPath, origin, Path.Op.INTERSECT) && !containsPath.isEmpty();
    }

    public boolean coveredByPath(Path cover) {
        containsPath.rewind();
        containsPath.op(myPath, cover, Path.Op.DIFFERENCE);
        return !myPath.isEmpty() && containsPath.isEmpty();
    }

    // combining actions
    public void addToPath(Path dst) {
        dst.addPath(myPath);
    }

    public void setStrokePath(Path src) {
        myPath.reset();
        myPath.addPath(src);
        currentState = ActionState.REVISING; // for Paper.setStyle to work
        invalidate();
    }


    public enum ActionState {
        NEW, STARTED, FINISHED, REVISING;

    }

    // quick buttons
    boolean animateQuickButton;
    RectF animateBounds;
    float animateQBRadius, animateX, animateY;
    int animateQBAlpha = 255;
    void startButtonClickedAnimation(RectF bounds, float x, float y) {
        animateQBRadius = 0;
        animateQBAlpha = 255;
        animateQuickButton = true;
        animateX = x;
        animateY = y;
        animateBounds = bounds;
        invalidate();
    }


    boolean actionBoxTouched(float x, float y) {
        return shouldShowQuickAction && currentState != ActionState.NEW &&
                currentState != ActionState.STARTED && quickEditBox != null &&
                quickEditBox.contains(x, y);
    }


    /**
     * for copy paste, if failed return null
     */
    public AbstractPaintActionExtendsView duplicate() {
        AbstractPaintActionExtendsView re = duplicateImp();
        if (re != null) {
            re.setOnCompletion(callWhenDoneSuper);
        }
        return re;
    }

    static final int DUPLICATE_OFFSET = 100;
    AbstractPaintActionExtendsView duplicateImp() {
        return null;
    }


    /**
     * useful helpers
     */

    void drawHighlight(Canvas canvas, Paint paint, float x, float y, int myColor, float extraRadius, float extraStokeWeight) {
        paint.setAlpha(HIGHLIGHT_ALPHA);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(HIGHLIGHT_STROKE_WIDTH + extraStokeWeight);
        paint.setColor(myColor);
        canvas.drawCircle(x, y,EDIT_TOUCH_RADIUS + extraRadius + paint.getStrokeWidth(), paint);
        paint.setColor(Calculator.CONTRAST_COLOR(myColor));
        canvas.drawCircle(x, y, EDIT_TOUCH_RADIUS + extraRadius, paint);
    }

    void drawHighlight(Canvas canvas, Paint paint, float x, float y, int myColor) {
        drawHighlight(canvas, paint, x, y, myColor, 0, 0);
    }
}
