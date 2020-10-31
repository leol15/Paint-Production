package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.oreo.paint.help.Calculator;

public class ActionNumbers extends ActionStroke {

    StringBuilder stringBuilder;
    int nextIntToAdd = 0;
    float CHAR_WIDTH;

    float thisTextSize = 150;
    public ActionNumbers(Context context) {
        super(context);
        CHAR_WIDTH = paint.measureText("0  ") / 3f + 3; // tune width :(
        stringBuilder = new StringBuilder();
    }



    float lX, lY, distMoved;
    @Override
    public boolean handleTouch(MotionEvent e) {
        boolean re = super.handleTouch(e);

        if (currentState == ActionState.FINISHED) {
            lastTimeStamp -= 10000; // hack so just draw one continuos path instead of many
        }
        if (currentState != ActionState.STARTED) return re;

        // only add numbers when drawing, not revising
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) {
            // threshold to bypass multitouch
            distMoved += Calculator.DIST(lX, lY, e.getX(), e.getY());
        }
        lX = e.getX();
        lY = e.getY();
        while (stringBuilder.length() * CHAR_WIDTH < distMoved) {
            addThingToString();
        }
        return re;
    }

    void addThingToString() {
        stringBuilder.append(nextIntToAdd++).append("  ");
    }

    @Override
    void onDraw2(Canvas canvas) {

        paint.setColor(thisColor);
        paint.setStrokeCap(thisCap);
        paint.setStrokeWidth(thisWidth);
        paint.setStrokeJoin(thisJoin);

        if (currentState == ActionState.REVISING || currentState == ActionState.STARTED) {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(myPath, paint);
        }

        // add texts
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(thisTextSize);
        canvas.drawTextOnPath(stringBuilder.toString(), myPath, 0, 0, paint);
    }

    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        thisTextSize = p.getStrokeWidth() * 8;
        paint.setTextSize(thisTextSize);
        CHAR_WIDTH = paint.measureText("0  ") / 3f + 3; // tune width :(
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {

        ActionNumbers re = new ActionNumbers(getContext());
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

        re.thisTextSize = thisTextSize;
        re.CHAR_WIDTH = CHAR_WIDTH;
        re.nextIntToAdd = nextIntToAdd;
        re.stringBuilder.append(stringBuilder.toString());
        return re;
    }
}
