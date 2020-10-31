package com.oreo.paint.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;

public class ActionPen extends ActionStroke {

    PathEffect pathEffect;
    public ActionPen(Context context) {
        super(context);
        setPathEffect();
    }

    void setPathEffect() {
        Path temp = new Path();
        temp.moveTo(0, -thisWidth / 2);
        temp.lineTo((float) (thisWidth / 1.5 / Math.sqrt(3)), -thisWidth / 2);
        temp.lineTo((float) - (thisWidth / 1.5 / Math.sqrt(3)),  thisWidth / 2);
        temp.lineTo((float) - (thisWidth / 0.75 / Math.sqrt(3)),  thisWidth / 2);
        temp.close();
        //        temp.addOval(-2, thisWidth / -2, 3, thisWidth / 2, Path.Direction.CW);
//        temp.addRect(-2, thisWidth / -2, 3, thisWidth / 2, Path.Direction.CW);
        pathEffect = new PathDashPathEffect(temp, Math.max(thisWidth / 16, 2), 0, PathDashPathEffect.Style.TRANSLATE);
        invalidate();
    }

    @Override
    public void setStyle(Paint p) {
        super.setStyle(p);
        setPathEffect();
    }

    @Override
    void onDraw2(Canvas canvas) {
        applyStrokeStyles();
//        if (currentState == ActionState.FINISHED) {
        paint.setPathEffect(pathEffect);
//        }
        canvas.drawPath(myPath, paint);
        paint.setPathEffect(null);
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {
        ActionPen re = new ActionPen(getContext());
        duplicateWork(re);
        re.setPathEffect();
        return re;
    }
}
