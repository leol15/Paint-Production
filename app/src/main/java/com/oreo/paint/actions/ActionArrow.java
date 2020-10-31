package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Arrays;

import com.oreo.paint.help.Calculator;

public class ActionArrow extends ActionStraightLine {
    static final String TAG = "-=-= ActionArrow";

    float[] triangle;
    int[] colors;

    public ActionArrow(Context context) {
        super(context);
        // x1,y1, x2...
        triangle = new float[]{0, 30, 0, -30, 50, 0};
        colors = new int[]{Color.RED, Color.RED, Color.RED, Color.RED, Color.RED, Color.RED};
    }


    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        // change size of triangle
        triangle[1] = thisWidth * 2.5f;
        triangle[3] = - thisWidth * 2.5f;
        triangle[4] = thisWidth * 5.5f;
        // color
        Arrays.fill(colors, thisColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // highlight by super
        super.onDraw(canvas);
        // also draw triangle
        // calculate the triangle position
        // translate and rotate?
        paint.setColor(thisColor);
        canvas.translate(coors[2], coors[3]);
        canvas.rotate(180f + (float) Calculator.ANGLE_BETWEEN(coors[1], coors[0], coors[3], coors[2]));
        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, 6, triangle, 0, null, 0,
                colors, 0, null, 0, 0, paint);
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {
        ActionArrow re = new ActionArrow(getContext());
        for (int i = 0; i < coors.length; i++) {
            re.coors[i] = coors[i] + DUPLICATE_OFFSET;
        }
        re.thisColor = thisColor;
        re.thisWidth = thisWidth;
        for (int i = 0; i < triangle.length; i++) {
            re.triangle[i] = triangle[i];
        }
        for (int i = 0; i < colors.length; i++) {
            re.colors[i] = colors[i];
        }
        re.updateMyPath();
        re.currentState = ActionState.FINISHED;
        return re;
    }
}
