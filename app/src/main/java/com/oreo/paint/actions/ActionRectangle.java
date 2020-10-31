package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.oreo.paint.help.Calculator;
import com.oreo.paint.help.InterestingPoints;
import com.oreo.paint.help.InterestingPoints.Point;

public class ActionRectangle extends AbstractPaintActionExtendsView {


    // positions
    float[] coors; // x1,y1, x2,y2

    // snap 2.0
    Set<InterestingPoints.Point> myIPs;
    // order, top, right, bottom, left, center

    static Paint paint;

    int myColor;
    float myWidth;
    Paint.Style myStyle;
    float rotateAngle;

    public ActionRectangle(Context context) {
        super(context);
        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
        }
        idMap = new HashMap<>();
        coors = new float[4];
        myIPs = new HashSet<>();
    }

    void updateMyIPs() {
        myIPs.clear();
        // respect the orders
        float cX = (coors[0] + coors[2]) / 2f;
        float cY = (coors[1] + coors[3]) / 2f;
        myIPs.add(new Point(cX, cY));
        // rotate
//        double r = dist(cX, cY, coors[0], coors[1]);
//        double deg = angleBetween(coors[0], coors[1], coors[2], coors[3]);
//        Log.d(TAG, "updateMyIPs: ang between " + deg);
//        deg -= rotateAngle;
//        deg = deg * Math.PI / 180;
//        myIPs.add(new Point((float) (cX + r * Math.cos(deg)), (float) (cY + r * Math.sin(deg))));
    }


    boolean deltaSnap() {
        for (Point p1 : myIPs) {
            InterestingPoints.Point p2 = interestingPoints.query(this, p1.x, p1.y);
            if (p2 != null) {
                // delta
                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                coors[0] += dx;
                coors[2] += dx;
                coors[1] += dy;
                coors[3] += dy;
                return true;
            }
        }
        return false;
    }


    // associate id to index
    HashMap<Integer, Integer> idMap;
    boolean firstTouch = true;
    int action = 0; // 0 resizing, 1 moving, 2 rotating
    float lastX, lastY;
    @Override
    public boolean handleTouch(MotionEvent e) {

        if (super.handleTouch(e)) {
            return true;
        }

        int index = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int id = e.getPointerId(index);

        if (!idMap.containsKey(id) && idMap.size() < 2) { // at most 2 fingers
            idMap.put(id, idMap.size() * 2); // hack  value = 0 or 2
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (currentState == ActionState.NEW) {
                    // init both points
                    coors[0] = e.getX(index);
                    coors[1] = e.getY(index);
                    coors[2] = e.getX(index);
                    coors[3] = e.getY(index);
                    firstTouch = false;
                    currentState = ActionState.STARTED;
                } else if (currentState == ActionState.FINISHED) {
                    // done
                    callWhenDone.apply(this);
                    return false;
                } else {
                    removeAllInterestingPoints();
                    // more than one finger or resize or move
                    if (e.getPointerCount() == 1) {
                        action = 1;
                        lastX = e.getX(index);
                        lastY = e.getY(index);
                    } else if (e.getPointerCount() == 2) {
                        if (contains(e.getX(index),e.getY(index),5) &&
                                Calculator.DIST((coors[0] + coors[2]) / 2, (coors[1] + coors[3]) / 2,e.getX(index), e.getY(index)) <
                                Math.max(Math.abs(coors[2] - coors[0]), Math.abs(coors[3] - coors[1]))) {
                            action = 0; // resize
                        } else {
                            action = 2; // rotate
                        }
                    }
                }
                updateMyPath();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (action == 1) {
                    // moving
                    coors[0] -= lastX - e.getX(index);
                    coors[1] -= lastY - e.getY(index);
                    coors[2] -= lastX - e.getX(index);
                    coors[3] -= lastY - e.getY(index);
                } else if (action == 2) {
                    // rotating
                    rotateAngle = (float) Calculator.ANGLE_BETWEEN(
                            (coors[0] + coors[2]) / 2, (coors[1] + coors[3]) / 2,
                            e.getX(index), e.getY(index));
                    // snap to angle
                    rotateAngle = Calculator.SNAP_ANGLE(rotateAngle);
                } else {
                    // resizing
                    for (int i : idMap.keySet()) {
                        if (e.findPointerIndex(i) != -1) {
                            // resize it
                            coors[idMap.get(i)] = e.getX(e.findPointerIndex(i));
                            coors[idMap.get(i) + 1] = e.getY(e.findPointerIndex(i));
                        }
                    }
                }
                // snap
                updateMyIPs();
                if (!deltaSnap()) {
                    lastX = e.getX(index);
                    lastY = e.getY(index);
                }
                updateMyIPs();
                updateMyPath();
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (e.getPointerCount() == 1) {
                    // last finger up
                    if (currentState == ActionState.STARTED) {
                        currentState = ActionState.FINISHED;
                    }
                    deltaSnap();
                    updateMyIPs();
                    removeAllInterestingPoints();
                    addAllInterestingPoints();
                }
                updateMyPath();
                invalidate();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void addAllInterestingPoints() {
        super.addAllInterestingPoints();
        for (Point p : myIPs) {
            interestingPoints.addPoint(this, p.x, p.y);
        }
    }

    Matrix rotationMatrix;
    void updateMyPath() {
        if (rotationMatrix == null) {
            rotationMatrix = new Matrix();
        }
        rotationMatrix.setRotate(-rotateAngle, (coors[0] + coors[2]) / 2f, (coors[1] + coors[3]) / 2f);
        myPath.rewind();
        myPath.addRect(
                Math.min(coors[0], coors[2]),
                Math.min(coors[1], coors[3]),
                Math.max(coors[0], coors[2]),
                Math.max(coors[1], coors[3]), Path.Direction.CW);
        myPath.transform(rotationMatrix);
    }

    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        myColor = p.getColor();
        myWidth = p.getStrokeWidth();
        myStyle = p.getStyle();
    }


    float time = 0;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        onDraw2(canvas);

        // show editing
        conditionalDrawHighlight(canvas);
    }

    void onDraw2(Canvas canvas) {

        paint.setColor(myColor);
        paint.setStrokeWidth(myWidth);
        paint.setStyle(myStyle);

        canvas.drawPath(myPath, paint);


        if (rotateAngle != 0) {
            canvas.save();
            canvas.translate((coors[0] + coors[2]) / 2, (coors[1] + coors[3]) / 2);
            canvas.rotate(-rotateAngle);
            canvas.translate(-(coors[0] + coors[2]) / 2, -(coors[1] + coors[3]) / 2);
        }


        canvas.drawRect(
                Math.min(coors[0], coors[2]),
                Math.min(coors[1], coors[3]),
                Math.max(coors[0], coors[2]),
                Math.max(coors[1], coors[3]),
                paint);

        if (rotateAngle != 0) {
            // rotate back
            canvas.restore();
        }
    }


    void conditionalDrawHighlight(Canvas canvas) {
        if (currentState == ActionState.REVISING || currentState == ActionState.STARTED) {
            drawHighlight(canvas, paint, (coors[0] + coors[2]) / 2f, (coors[1] + coors[3]) / 2f, myColor);
        }
    }


    @Override
    public AbstractPaintActionExtendsView duplicateImp() {
        ActionRectangle act2 = new ActionRectangle(getContext());
        act2.currentState = ActionState.FINISHED;
        for (int i = 0; i < coors.length; i++) {
            act2.coors[i] = coors[i] + DUPLICATE_OFFSET; // shifted
        }
        act2.myColor = myColor;
        act2.myWidth = myWidth;
        act2.myStyle = myStyle;
        act2.rotateAngle = rotateAngle;
        act2.updateMyPath(); // since no actual touch is on the new path,
                                // manually add to path
        return act2;
    }
}
