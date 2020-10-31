package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.oreo.paint.help.Calculator;


public class ActionStroke extends AbstractPaintActionExtendsView {
    
    static final String TAG = "-=-= ActionStroke";
    

    public static final float MIN_MOVE_DIST = 5;
    static final long NEW_INSTANCE_DELAY_MS = 100;

    int thisColor;
    float thisWidth;
    Paint.Cap thisCap;
    Paint.Join thisJoin;

    static Paint paint;
    Path savedPath;
    Matrix pathTransform;
    float boundW, boundH, boundCX, boundCY; // remember info about path when updating savdPath

    public ActionStroke(Context context) {
        super(context);
        if (paint == null) {
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
        }
        savedPath = new Path();
        bound = new RectF();
        pathTransform = new Matrix();
//        Log.d(TAG, "ActionStroke: init");
    }

    long lastTimeStamp; // should combine quick strokes

    int action = 0; // 0 move, 1 scale, 2 rotate
    int secondPointerId = -1;
    int pointerId = -1;

    float lastX, lastY;
    @Override
    public boolean handleTouch(MotionEvent e) {
        if (super.handleTouch(e)) {
            return true;
        }
        int index = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int id = e.getPointerId(index);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (currentState == ActionState.NEW) {
                    pointerId = id;
                    lastX = e.getX(index);
                    lastY = e.getY(index);
                    myPath.moveTo(lastX, lastY);
                    currentState = ActionState.STARTED;
                } else if (currentState == ActionState.FINISHED) {
                    if (System.currentTimeMillis() - lastTimeStamp <= NEW_INSTANCE_DELAY_MS) {
                        // keep going
                        pointerId = id;
                        lastX = e.getX(index);
                        lastY = e.getY(index);
                        myPath.moveTo(lastX, lastY);
                        currentState = ActionState.STARTED;
                    } else {
                        callWhenDone.apply(this);
                        return false;
                    }
                } else if (currentState == ActionState.REVISING) {
                    // editing
                    revising_ptr_down(e, index, id);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                index = e.findPointerIndex(pointerId);
                if (index == -1) return true;
                if (currentState == ActionState.STARTED) {
                    if (Calculator.DIST(lastX, lastY, e.getX(index), e.getY(index)) >= MIN_MOVE_DIST) {
                        myPath.quadTo(lastX,lastY,
                                (lastX + e.getX(index)) / 2f,
                                (lastY + e.getY(index)) / 2f);
                        lastX = e.getX(index);
                        lastY = e.getY(index);
                        invalidate();
                    }
                } else if (currentState == ActionState.REVISING) {
                    // translate
                    revising_ptr_move(e, index, id);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (currentState == ActionState.STARTED) {
                    currentState = ActionState.FINISHED;
                    lastTimeStamp = System.currentTimeMillis();
                } else if (currentState == ActionState.REVISING) {
                    // revising
                    if ((action == 1 && e.getPointerCount() == 2) || (action == 2 && e.getPointerCount() == 1)) {
                        pathTransform.reset();
                        boundW = bound.width();
                        boundH = bound.height();
                        boundCX = bound.centerX();
                        boundCY = bound.centerY();
                        savedPath.rewind();
                        savedPath.addPath(myPath);
                        // reset saved path
                        action = 0; // back to move
                        boundCX = bound.centerX();
                        boundCY = bound.centerY();  // stop
                    }
                }
                invalidate();
                return true;
            default:
                return false;
        }
    }


    void revising_ptr_down(MotionEvent e, int index, int id) {
        if (action == 2 || action == 1) return;  // rotating, ignores new finger
        if (e.getPointerCount() == 1) {
            pointerId = id;
            action = 0;
            lastX = e.getX(index);
            lastY = e.getY(index);
        } else if (e.getPointerCount() == 2) {
            if (bound.contains(e.getX(index), e.getY(index))) {
                action = 1;
                // scale
                secondPointerId = id;
                if (e.getY(index) > e.getY(e.findPointerIndex(pointerId))) {
                    secondPointerId = pointerId;
                    pointerId = id;
                }
            } else {
                action = 2;
                // rotate
            }
        }
        pathTransform.reset();
        boundW = bound.width();
        boundH = bound.height();
        boundCX = bound.centerX();
        boundCY = bound.centerY();
        savedPath.rewind();
        savedPath.addPath(myPath);
    }

    void revising_ptr_move(MotionEvent e, int index, int id) {
        if (action == 0) {
            pathTransform.setTranslate(e.getX(index) - lastX, e.getY(index) - lastY);
        } else if (action == 1) {
            if (e.findPointerIndex(pointerId) == -1 || e.findPointerIndex(secondPointerId) == -1) {
                return; // finger disappeared
            }
            pathTransform.setScale(
                    (e.getX(e.findPointerIndex(pointerId)) - e.getX(e.findPointerIndex(secondPointerId))) / boundW,
                    (e.getY(e.findPointerIndex(pointerId)) - e.getY(e.findPointerIndex(secondPointerId))) / boundH,
                    boundCX, boundCY);
            pathTransform.postTranslate(
                    (e.getX(e.findPointerIndex(pointerId)) + e.getX(e.findPointerIndex(secondPointerId))) / 2f - boundCX,
                    (e.getY(e.findPointerIndex(pointerId)) + e.getY(e.findPointerIndex(secondPointerId))) / 2f - boundCY);
            // also translate
        } else if (action == 2) {
            // rotate
            pathTransform.setRotate(
                    Calculator.SNAP_ANGLE((float) -Calculator.ANGLE_BETWEEN(boundCX, boundCY, e.getX(), e.getY())),
                    boundCX, boundCY);
        }
        savedPath.transform(pathTransform, myPath);
    }


    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        thisColor = p.getColor();
        thisWidth = p.getStrokeWidth();
        thisCap = p.getStrokeCap();
        thisJoin = p.getStrokeJoin();
    }

    RectF bound;
    float animate;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        onDraw2(canvas);

        conditionalDrawHighlight(canvas);

    }

    void onDraw2(Canvas canvas) {
        applyStrokeStyles();
        canvas.drawPath(myPath, paint);
    }

    void applyStrokeStyles() {
        paint.setColor(thisColor);
        paint.setStrokeCap(thisCap);
        paint.setStrokeWidth(thisWidth);
        paint.setStrokeJoin(thisJoin);
        paint.setStyle(Paint.Style.STROKE);
    }

    void conditionalDrawHighlight(Canvas canvas) {
        if (currentState == ActionState.REVISING) {
            paint.setStrokeWidth(HIGHLIGHT_STROKE_WIDTH);
            if (action == 2) {
                // rotate center instead of bound
                paint.setAlpha(HIGHLIGHT_ALPHA);
                canvas.drawCircle(boundCX, boundCY, 10, paint);
            } else {
                // high light
                paint.setAlpha((int) (70 + 70 * Math.sin(animate)));
                animate += 0.1;
                invalidate();
                // or bound?
                myPath.computeBounds(bound, false);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(bound, paint);
            }
        }
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {
        ActionStroke re = new ActionStroke(getContext());
        duplicateWork(re);
        return re;
    }

    void duplicateWork(ActionStroke re) {
        re.thisColor = thisColor;
        re.thisWidth = thisWidth;
        re.thisCap = thisCap;
        re.thisJoin = thisJoin;
        re.savedPath.addPath(savedPath);
        re.myPath.addPath(myPath);
        re.currentState = ActionState.FINISHED;
        re.pathTransform.reset();
        re.pathTransform.setTranslate(DUPLICATE_OFFSET, DUPLICATE_OFFSET);
        re.myPath.transform(re.pathTransform);
        re.pathTransform.set(pathTransform);
    }
}
