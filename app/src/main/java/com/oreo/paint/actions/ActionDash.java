package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;

public class ActionDash extends ActionStroke {
    PathEffect pathEffect;
    public ActionDash(Context context) {
        super(context);
        pathEffect = new DashPathEffect(new float[]{thisWidth, thisWidth * 2}, 0);
    }

    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        pathEffect = new DashPathEffect(new float[]{thisWidth, thisWidth * 2}, 0);
    }

    @Override
    void onDraw2(Canvas canvas) {
        applyStrokeStyles();
        paint.setStrokeCap(Paint.Cap.SQUARE);

        paint.setPathEffect(pathEffect);
        canvas.drawPath(myPath, paint);
        paint.setPathEffect(null);
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {
        ActionDash re = new ActionDash(getContext());
        duplicateWork(re);
        re.pathEffect = new DashPathEffect(new float[]{thisWidth, thisWidth * 2}, 0);
        return re;
    }
}
