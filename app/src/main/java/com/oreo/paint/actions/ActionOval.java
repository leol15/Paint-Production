package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;

public class ActionOval extends ActionRectangle {

    public ActionOval(Context context) {
        super(context);
    }

    @Override
    void onDraw2(Canvas canvas) {
        paint.setStyle(myStyle);
        paint.setColor(myColor);
        paint.setStrokeWidth(myWidth);
        if (rotateAngle != 0) {
            canvas.translate((coors[0] + coors[2]) / 2, (coors[1] + coors[3]) / 2);
            canvas.rotate(-rotateAngle);
            canvas.translate(-(coors[0] + coors[2]) / 2, -(coors[1] + coors[3]) / 2);
        }

        canvas.drawOval(
                Math.min(coors[0], coors[2]),
                Math.min(coors[1], coors[3]),
                Math.max(coors[0], coors[2]),
                Math.max(coors[1], coors[3]),
                paint);
    }

    @Override
    void updateMyPath() {
        if (rotationMatrix == null) {
            rotationMatrix = new Matrix();
        }
        rotationMatrix.setRotate(-rotateAngle, (coors[0] + coors[2]) / 2f, (coors[1] + coors[3]) / 2f);
        myPath.rewind();
        myPath.addOval(
                Math.min(coors[0], coors[2]),
                Math.min(coors[1], coors[3]),
                Math.max(coors[0], coors[2]),
                Math.max(coors[1], coors[3]), Path.Direction.CW);
        myPath.transform(rotationMatrix);
    }

    @Override
    public AbstractPaintActionExtendsView duplicateImp() {
        ActionOval act2 = new ActionOval(getContext());
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
