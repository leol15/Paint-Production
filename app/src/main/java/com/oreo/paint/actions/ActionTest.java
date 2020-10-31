package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;

public class ActionTest extends ActionStroke {
    PathEffect pathEffect;
    public ActionTest(Context context) {
        super(context);
        setPathEffect();
    }

    void setPathEffect() {
        Path path = new Path();
        path.addCircle(0,0,thisWidth / 2, Path.Direction.CW);
        pathEffect = new PathDashPathEffect(path, thisWidth * 2.5f, thisWidth / 2, PathDashPathEffect.Style.TRANSLATE);
    }

    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        setPathEffect();
    }

    @Override
    void onDraw2(Canvas canvas) {
        super.applyStrokeStyles();
        paint.setPathEffect(pathEffect);
        canvas.drawPath(myPath, paint);
        paint.setPathEffect(null);
    }
}
