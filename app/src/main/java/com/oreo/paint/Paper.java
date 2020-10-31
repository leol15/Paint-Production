package com.oreo.paint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oreo.paint.actions.AbstractPaintActionExtendsView;
import com.oreo.paint.actions.ActionPen;
import com.oreo.paint.help.Calculator;
import com.oreo.paint.help.InterestingPoints;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;


/**
 * the paper to hold all drawings (views)
 */
public class Paper extends FrameLayout {
    static final String TAG = "-=-= Paper";


    /////////////////////////
    // for super actions
    /////////////////////////
    // rep expose?


    /**
     * invariant if u can keep it:
     * action is not null
     * action is added to view, view group
     * action is not "not" in history, changed Sunday
     */

    public ArrayList<AbstractPaintActionExtendsView> history;
    public Stack<AbstractPaintActionExtendsView> redoStack;
    // current action
    public AbstractPaintActionExtendsView action;
    // current action's class
    Class<? extends AbstractPaintActionExtendsView> actionClass = ActionPen.class;
    Class<? extends AbstractPaintActionExtendsView> previousActionClass = ActionPen.class;
    static Paint theOneAndOnlyPaint;

    int background_color = -1;

    // snap to interesting points
    InterestingPoints interestingPoints;

    public Paper(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Paper: initializing");
        theOneAndOnlyPaint = new Paint();
        theOneAndOnlyPaint.setColor(CURRENT_COLOR);
        theOneAndOnlyPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        theOneAndOnlyPaint.setStrokeWidth(10);
        theOneAndOnlyPaint.setTextSize(120);
        theOneAndOnlyPaint.setStrokeCap(Paint.Cap.ROUND);
        interestingPoints = new InterestingPoints();

        // internal use
        internalPaint = new Paint();

        history = new ArrayList<>();
        redoStack = new Stack<>();
        // panning
        panningIndexes = new HashSet<>();
        groupSelectPath = new Path();
        groupSelectPathEffect = new DashPathEffect(new float[]{20, 10}, 0);
        initCurrentAction();

    }


    /**
     * initialize current action from actionClass
     */
    void initCurrentAction() {
        try {
            action = actionClass.getConstructor(Context.class).newInstance(getContext());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            Log.e(TAG, "initAction: cannot init class", e);
        }
        action.setInterestingPoints(interestingPoints);
        action.setStyle(theOneAndOnlyPaint); // apply current style
        // add to view
        addView(action);
        // add to history
        history.add(action);
        // animation start
        histTranslateX = getWidth() * 11;
        invalidate();
        // callback
        action.setOnCompletion((action) -> {
            initCurrentAction();    // let actions clone themselves
            return null;
        });
    }

    /**
     * end the current action
     * may produce empty history!
     */
    void finishAction() {
        if (!action.focusLost()) {
            if (history.size() > 0)
                removeView(history.remove(history.size() - 1));
        }
    }

    /**
     * interface to select an action
     * set next action - line, rect...
     */
    public void setDrawAction(Class<? extends AbstractPaintActionExtendsView> action) {
        // update previous action
        if (action != actionClass) {
            previousActionClass = actionClass;
        }
        if (isErasing()) toggleEraseMode();
        if (isPanning()) togglePanningMode();
        finishAction();
        actionClass = action;
        initCurrentAction();
    }

    public Class<? extends AbstractPaintActionExtendsView> getCurrentAction() {
        return actionClass;
    }

    public Class<? extends AbstractPaintActionExtendsView> getPreviousAction() {
        return previousActionClass;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // show/hide history
//        if (action.contains(getWidth() / 2f, getHeight() * -0.1f, getWidth() / 2f)) {
//            // hide
//            histYTarget = getHeight() * -0.1f;
//            delayCount += 1;
//            postDelayed(showHistory, 200);
//            performClick(); // weird lint issue
//        } else {
//            histYTarget = 0;
//        }
        // erasing
//        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        state += 1;
//        }
        if (erasing) {
            eraseAction(event);
            invalidate();
            return true;
        } else if (panning) {
            return panningAction(event);
        } else if (action != null) {
//            // editing history
//            if (selectingHistoryAction(event)) {
//                return true;
//            }
            // real action with Actions
            boolean b = action.handleTouch(event);
            if (!b) { // action has changed through call back
                b = action.handleTouch(event);
            }
            invalidate();
            return b;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    ///////////////////
    // for the settings
    ///////////////////

    // here u go, just edit the paint
    public Paint getPaintToEdit() {
        return theOneAndOnlyPaint;
    }

    int PREVIOUS_COLOR;
    int CURRENT_COLOR = Color.RED;
    // apply settings
    public void applyPaintEdit() {
        if (theOneAndOnlyPaint.getColor() != CURRENT_COLOR) {
            PREVIOUS_COLOR = CURRENT_COLOR; // save the color
            CURRENT_COLOR = theOneAndOnlyPaint.getColor();
        }
        if (isPanning()) {
            // apply edits to current panning thing
            for (int i : panningIndexes) {
                history.get(i).setStyle(theOneAndOnlyPaint);
            }
        } else {
            if (action.getCurrentState() == AbstractPaintActionExtendsView.ActionState.REVISING ||
                    action.getCurrentState() == AbstractPaintActionExtendsView.ActionState.NEW) {
                action.setStyle(theOneAndOnlyPaint);
            }
        }
    }

    public int getPreviousColor() {
        return PREVIOUS_COLOR;
    }

    // other settings

    @Override
    public void setBackgroundColor(int color) {
        background_color = color;
        super.setBackgroundColor(background_color);
    }

    public int getBackgroundColor() {
        return background_color;
    }


    ////////////////////
    // manage itself
    ////////////////////

    public boolean undo() {
        finishAction();
        if (canUndo()) {
            clearPaperStates();
            redoStack.push(history.remove(history.size() - 1));
            removeView(redoStack.peek());
            redoStack.peek().removingFromView();
            if (canUndo()) {
                action = history.get(history.size() - 1);
            } else {
//                Log.d(TAG, "undo: initing");
                initCurrentAction();
            }
            invalidate();
            return true;
        } else {
            Log.i(TAG, "unDo: nothing to undo");
            // add action back to view
            history.add(action);
            addView(action);
            invalidate();
            return false;
        }
    }

    public boolean redo() {
        if (canRedo()) {
            clearPaperStates();
            finishAction();
            history.add(redoStack.pop());
            action = history.get(history.size() - 1);
            addView(action);
            histTranslateX = getWidth() * 11;
            action.addingToView();
            invalidate();
            return true;
        } else {
//            Log.i(TAG, "redo: nothing to redo");
            return false;
        }
    }

    // you can undo a clear, slowly
    public void clear() {
        clearPaperStates();
        finishAction();
        for (int i = history.size() - 1; i > -1; i--) {
            redoStack.push(history.get(history.size() - 1 - i));
            redoStack.peek().removingFromView();
        }
        history.clear();
        removeAllViews();
        initCurrentAction();
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    float currPointerX, currPointerY;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        drawHistory(canvas);

        if (erasing) {
            // show touch
            canvas.drawCircle(currPointerX, currPointerY, eraserRadius, internalPaint);
        } else if (panning) {
            if (panningIndexes.size() != 0) {
                // show meta actions of current action
                drawPanningQuickActionBox(canvas);
            } else {
                // draw path
                internalPaint.setPathEffect(groupSelectPathEffect);
                internalPaint.setColor(theOneAndOnlyPaint.getColor());
                internalPaint.setStyle(Paint.Style.STROKE);
                internalPaint.setStrokeWidth(7);
                canvas.drawPath(groupSelectPath, internalPaint);
                internalPaint.setPathEffect(null);
            }
            // debug ?? show interesting points
            internalPaint.setColor(Calculator.CONTRAST_COLOR(background_color));
            internalPaint.setStyle(Paint.Style.FILL);
            for (InterestingPoints.Point p : interestingPoints.allPoints()) {
                canvas.drawCircle(p.x, p.y, 4, internalPaint);
            }

        }
    }


    // let actions know if they are editing or done
    public void editActionButtonClicked() {
        if (erasing) {
            toggleEraseMode();
            // stop erasing
        }
        if (action.getCurrentState() == AbstractPaintActionExtendsView.ActionState.NEW) {
            if (history.size() <= 1) {
                Log.i(TAG, "editActionButtonClicked: nothing to edit");
                return;
            } else {
                // edit the last one, not this one
                finishAction();
                action = history.get(history.size() - 1);
            }
        }
        // edit current action
        action.editButtonClicked();
    }

    /**
     * erase action
     */
    boolean erasing;
    float eraserRadius = 15;

    public void toggleEraseMode() {
        erasing = !erasing;
        if (erasing) {
            // check current
            finishAction();
            if (isPanning()) {
                togglePanningMode();
            }
        } else {
            if (canUndo()) {
                action = history.get(history.size() - 1);
            } else {
                initCurrentAction();
            }
        }
        invalidate();
    }

    public boolean isErasing() {
        return erasing;
    }

    void eraseAction(MotionEvent event) {
        currPointerX = event.getX();
        currPointerY = event.getY();
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).contains(currPointerX, currPointerY, eraserRadius)) {
                redoStack.add(history.remove(i));
                removeView(redoStack.peek());
                return;
            }
        }
    }


    /**
     * moving action
     */
    boolean panning;

    public void togglePanningMode() {
        panning = !panning;
        if (panning) {
            // start
            if (isErasing()) {
                toggleEraseMode();
            }
            if (action != null) {
                action.focusLost();
            }
        } else {
            // end
            if (panningIndexes.size() != 0) {
                // deactivate this view
                for (int i : panningIndexes) {
                    history.get(i).focusLost();
                }
                panningIndexes.clear();
            }
            if (canUndo()) {
                action = history.get(history.size() - 1);
            } else {
                initCurrentAction();
            }
        }
        invalidate();
    }

    public boolean isPanning() {
        return panning;
    }

    HashSet<Integer> panningIndexes;
    Path groupSelectPath;
    PathEffect groupSelectPathEffect;
    float groupSelectPathLength;
    long lastTapTime;
    float panX, panY;
    float panLastX, panLastY;
    boolean skipPan;
    RectF panFinishBox;
    RectF panDuplicateBox;
    RectF panMoveToFrontBox;
    RectF panMoveToBackBox;
    RectF panDeleteBox;
    void drawPanningQuickActionBox(Canvas c) {
        if (panMoveToFrontBox == null) {
            float top = getHeight() / 1.7f;
            panFinishBox = new RectF(getWidth() - 100, top, getWidth(), top + 100);
            top += 100;
            panDuplicateBox = new RectF(getWidth() - 100, top, getWidth(), top + 100);
            top += 100;
            panMoveToFrontBox = new RectF(getWidth() - 100, top, getWidth(), top + 100);
            top += 100;
            panMoveToBackBox = new RectF(getWidth() - 100, top, getWidth(), top + 100);
            top += 100;
            panDeleteBox = new RectF(getWidth() - 100, top, getWidth(), top + 100);
        }
        // draw boxes
        internalPaint.setColor(Color.BLACK);
        internalPaint.setStyle(Paint.Style.FILL);
        c.drawRect(panFinishBox, internalPaint);
        c.drawRect(panDuplicateBox, internalPaint);
        c.drawRect(panMoveToFrontBox, internalPaint);
        c.drawRect(panMoveToBackBox, internalPaint);
        c.drawRect(panDeleteBox, internalPaint);
        internalPaint.setTextSize(panMoveToFrontBox.height() / 2);
        internalPaint.setTextAlign(Paint.Align.CENTER);
        internalPaint.setColor(Color.WHITE);

        c.drawText("\u2713", panFinishBox.centerX(),
                panFinishBox.centerY() + internalPaint.getTextSize() / 2 - internalPaint.descent() / 2,
                internalPaint);

        c.drawText("⧉", panDuplicateBox.centerX(),
                panDuplicateBox.centerY() + internalPaint.getTextSize() / 2 - internalPaint.descent() / 2,
                internalPaint);
        c.drawText("▲", panMoveToFrontBox.centerX(),
                panMoveToFrontBox.centerY() + internalPaint.getTextSize() / 2 - internalPaint.descent() / 2,
                internalPaint);
        c.drawText("▼", panMoveToBackBox.centerX(),
                panMoveToBackBox.centerY() + internalPaint.getTextSize() / 2 - internalPaint.descent() / 2,
                internalPaint);

        c.drawText("⊗", panDeleteBox.centerX(),
                panDeleteBox.centerY() + internalPaint.getTextSize() / 2 - internalPaint.descent() / 2,
                internalPaint);
    }

    boolean panningAction(MotionEvent e) {
        if (skipPan) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP)
                skipPan = false;
            return true;
        }
        if (panningIndexes.size() == 0) {
            // selecting
            if (groupSelectPath.isEmpty()) {
                groupSelectPathLength = 0;
                panLastX = e.getX();
                panLastY = e.getY();
                groupSelectPath.moveTo(panLastX, panLastY);
                groupSelectPath.lineTo(panLastX + 1, panLastY);
            } else {
                // smooth line to
                if (dist(panLastX, panLastY, e.getX(), e.getY()) >= 5) {
                    groupSelectPath.quadTo(panLastX, panLastY,
                            (panLastX + e.getX()) / 2f,
                            (panLastY + e.getY()) / 2f);
                    if (groupSelectPathLength < 50) {
                        groupSelectPathLength += dist(panLastX, panLastY, e.getX(),e.getY());
                    }
                    panLastX = e.getX();
                    panLastY = e.getY();
                    invalidate();
                }
            }
            // see if got any
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                // now see any hit? which select?
                if (groupSelectPathLength >= 50) {
                    // group select
                    groupSelectPath.close();
                    for (int i = 0; i < history.size(); i++) {
                        if (history.get(i).coveredByPath(groupSelectPath)) {
                            panningIndexes.add(i);
                            history.get(i).editButtonClicked();
                        }
                    }
                } else {
                    // single select
                    for (int i = history.size() - 1; i > -1; i--) {
                        if (history.get(i).contains(e.getX(), e.getY(), 15)) {
                            panningIndexes.add(i);
                            history.get(i).editButtonClicked();
                            break;
                        }
                    }
                }
                // rewind path
                groupSelectPath.rewind();
                invalidate();
            }
        } else {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                // record time
                if (panFinishBox.contains(e.getX(), e.getY())) {
                    // deselect all
                    // copied from cancel
                    for (int i : panningIndexes) {
                        history.get(i).focusLost();
                    }
                    panningIndexes.clear();
                    invalidate();
                    skipPan = true;
                } else if (panMoveToFrontBox.contains(e.getX(), e.getY())) {
                    // moving to front
                    panningIndexes = shiftZIndex(panningIndexes, true);
                    skipPan = true;
                } else if (panMoveToBackBox.contains(e.getX(), e.getY())) {
                    // moving to back
                    panningIndexes = shiftZIndex(panningIndexes, false);
                    skipPan = true;
                } else if (panDuplicateBox.contains(e.getX(), e.getY())) {
                    // duplicate all actions selected
                    ArrayList<Integer> tempList = new ArrayList<>(panningIndexes);
                    panningIndexes.clear();
                    tempList.sort((a, b) -> a - b);
                    for (int i : tempList) {
                        history.get(i).focusLost();
                        AbstractPaintActionExtendsView dup = history.get(i).duplicate();
                        if (dup == null) continue;
                        history.add(dup);
                        dup.addingToView();
                        addView(dup);
                        dup.editButtonClicked();
                        panningIndexes.add(history.size() - 1);
                    }
                    skipPan = true;
                } else if (panDeleteBox.contains(e.getX(), e.getY())) {
                    // delete all
                    ArrayList<Integer> tempList = new ArrayList<>(panningIndexes);
                    panningIndexes.clear();
                    tempList.sort((a, b) -> b - a);
                    for (int i : tempList) {
                        redoStack.push(history.get(i));
                        history.get(i).focusLost();
                        history.get(i).removingFromView();
                        removeView(history.remove(i));
                    }
                } else if (Math.abs(panX - e.getX()) + Math.abs(panY - e.getY()) < 80 &&
                        System.currentTimeMillis() - lastTapTime < 200) {
                    // cancel
                    for (int i : panningIndexes) {
                        history.get(i).focusLost();
                    }
                    panningIndexes.clear();
                    skipPan = true;
                    invalidate();
                } else  {
                    // record time
                    panX = e.getX();
                    panY = e.getY();
                    lastTapTime = System.currentTimeMillis();
                    for (Integer i : panningIndexes) {
                        history.get(i).handleTouch(e);
                    }
                }
            } else {
                for (Integer i : panningIndexes) {
                    history.get(i).handleTouch(e);
                }
                // DEBUG: show interesting points
                invalidate();
            }
        }
        AbstractPaintActionExtendsView.GROUP_SELECTED = panningIndexes.size() > 1;
        return true;
    }

    // bring every element of a set (indecies) one up|down in history
    HashSet<Integer> shiftZIndex(HashSet<Integer> targetSet, boolean shiftUp) {
        HashSet<Integer> newSet = new HashSet<>();
        while (!targetSet.isEmpty()) {
            int low = targetSet.iterator().next();
            int high = low;
            targetSet.remove(low);
            while (targetSet.contains(high + 1)) {
                high++;
                targetSet.remove(high);
            }
            while (targetSet.contains(low - 1)) {
                low--;
                targetSet.remove(low);
            }
            // shift things
            if ((shiftUp && high == history.size() - 1) || (!shiftUp && low == 0)) {
                // cannot do anythin
                for (int i = low; i <= high; i++) {
                    newSet.add(i);
                }
            } else {
                // swaps
                if (shiftUp) {
                    // swap
                    for (int i = high; i >= low; i--) {
                        // swap
                        AbstractPaintActionExtendsView temp = history.get(i);
                        removeView(temp);
                        addView(temp, i + 1);
                        history.set(i, history.get(i + 1));
                        history.set(i + 1, temp);
                        newSet.add(i + 1);
                    }
                } else {
                    for (int i = low; i <= high; i++) {
                        // swap
                        AbstractPaintActionExtendsView temp = history.get(i);
                        removeView(temp);
                        addView(temp, i - 1);
                        history.set(i, history.get(i - 1));
                        history.set(i - 1, temp);
                        newSet.add(i - 1);
                    }
                }
            }
        }
        return newSet;
    }

    private float dist(float a, float b, float x, float y) {
        return (float) Math.sqrt(Math.pow(x - a, 2) + Math.pow(y - b, 2));
    }

    public void clearPaperStates() {
        if (isPanning())
            togglePanningMode();
        if (isErasing())
            toggleEraseMode();
    }

    // EXPERIMENTTs
    static Paint internalPaint;
    float histTranslateX, histY, histYTarget;

    void drawHistory(Canvas canvas) {
        if (history == null) {
            return;
        }
        if (internalPaint == null) {
            internalPaint = new Paint();
            internalPaint.setColor(Color.BLACK);
            internalPaint.setStyle(Paint.Style.STROKE);
        }
        // also draw the history
        canvas.save();
        canvas.translate(0, histY); // getHeight() * 0.9f
        canvas.scale(0.1f, 0.1f);
        float dx = histTranslateX; // getWidth() * 10;

        for (int i = history.size() - 1; i >= 0; i--) {
            if (dx <= 0) {
                break;
            }
            dx -= getWidth();
            canvas.save();
            canvas.translate(dx, 0);
            internalPaint.setStrokeWidth(20);
            canvas.drawRect(0, 0, getWidth(), getHeight(), internalPaint);
            history.get(i).draw(canvas);
            canvas.restore();
        }
        canvas.restore();

        // animate
        if (histTranslateX > getWidth() * 10) {
            histTranslateX -= (histTranslateX - getWidth() * 10) * 0.2 + 0.5;
            invalidate();
        } else {
            histTranslateX = getWidth() * 10;
        }
        if (Math.abs(histY - histYTarget) > 1) {
            histY += (histYTarget - histY) * 0.2;
            invalidate();
        }
    }


    int delayCount = 0;
    Runnable showHistory = () -> {
        delayCount--;
        if (delayCount == 0) {
            histYTarget = 0;
            invalidate();
        }
    };


    boolean selectingHistory = false;

    // select history
    boolean selectingHistoryAction(MotionEvent e) {
        if (history.size() == 0) {
            return false;
        }
        // the height of histroy is 0.1 h
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && e.getY() < histY + getHeight() * 0.1) {
            selectingHistory = true;
            return true;
        }
        if (selectingHistory) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                // deal with current action
                finishAction(); // zero size array
                if (!canUndo()) {
                    initCurrentAction();
                    return true;
                }
                // select index
                int index = history.size() - 10 + (int) (e.getX() * 10 / getWidth());
                index = Math.max(0, Math.min(history.size() - 1, index));
                // move index
                action = history.remove(index);
                history.add(action);
                action.editButtonClicked();
                selectingHistory = false;
                invalidate();
            }
            return true;
        }
        return false;
    }


    //////////////////
    // super actions
    //////////////////

    public void drawSelf(Canvas c) {
        finishAction();
        c.drawColor(background_color);
        for (AbstractPaintActionExtendsView act : history) {
            act.draw(c);
        }
        if (history.size() == 0) {
            initCurrentAction();
        }
    }

    // for sync

    int state;
    public int getState() {
        return state;
    }

}
