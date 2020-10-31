package com.oreo.paint.settings;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.oreo.paint.R;
import com.oreo.paint.help.Calculator;

public class PaperStates extends AbstractSetting {

    RectF[] states;
//    static final String[] icons = new String[]{"✎", "⬚", "❒", "⛶"}; // ි
    static final String[] icons = new String[]{"✎", "◌", "⊠", "⎙"}; // ි
    public PaperStates() {
        states = new RectF[icons.length];
        for (int i = 0; i < icons.length; i++) {
            states[i] = new RectF();
        }
    }

    PathEffect dashPath;
    @Override
    void privateInit() {
        dashPath = new DashPathEffect(new float[]{10, 5}, 0);
        paint.setTextSize(iW);
        paint.setTextAlign(Paint.Align.CENTER);
        // position states
        float maxX = mW / states.length;
        float boxW = maxX * 0.6f;
        float startX = (maxX - boxW) / 2;
        for (RectF state : states) {
            state.set(startX, iTop, startX + boxW, iTop + iH);
            startX += maxX;
        }
    }

    // copy ⧉
    // select ⬚
    // clear ⮽
    // moving ✋
    @Override
    public void drawIcon(Canvas canvas) {
        super.drawIcon(canvas);
        paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
        paint.setStyle(Paint.Style.FILL);
        if (paper.isErasing()) {
            canvas.drawText(icons[2], iLeft + iW / 2f,
                    iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2,
                    paint);
        } else if (paper.isPanning()) {
//            canvas.drawText(icons[1], iLeft + iW / 2f,
//                    iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2,
//                    paint);
            drawPanningIcon(canvas, iLeft + iW / 2f, iTop + iH / 2f, paint.getTextSize() / 2.8f);
        } else {
            canvas.drawText(icons[0], iLeft + iW / 2f,
                    iTop + iH / 2f + paint.getTextSize() / 2 - paint.descent() / 2,
                    paint);
        }
    }

    void drawPanningIcon(Canvas c, float x, float y, float radius) {
        paint.setPathEffect(dashPath);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        c.drawCircle(x, y, radius, paint);
        paint.setPathEffect(null);
    }

    @Override
    public boolean handleQuickEvent(MotionEvent e) {
        super.handleQuickEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (inIcon(e.getX(), e.getY())) {
                    START_MAIN_ACTION.run();
                } else {
                    END_MAIN_ACTION.run();
                }
        }
        return true;
    }

    @Override
    public void drawMain(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < states.length; i++) {
            RectF box = states[i];
            paint.setColor(Calculator.CONTRAST_COLOR(paper.getBackgroundColor()));
            paint.setAlpha(220);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(box, 10, 10, paint);
            paint.setColor(Calculator.CONTRAST_COLOR(paint.getColor()));
            if (i == 1) {
                drawPanningIcon(canvas, box.centerX(), box.centerY(), box.height() / 2.8f);
            } else {
                canvas.drawText(icons[i], box.centerX(), box.centerY() + paint.getTextSize() / 2 - paint.descent() / 2, paint);
            }
        }
    }

    @Override
    public boolean handleMainEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                for (int i = 0; i < states.length; i++) {
                    if (states[i].contains(e.getX() - iW, e.getY())) {
                        performAction(i);
                        break;
                    }
                }
                END_MAIN_ACTION.run();
        }
        return true;
    }

    void performAction(int iconIndex) {
        if (iconIndex == 2) {
            paper.toggleEraseMode();
        } else if (iconIndex == 1) {
            paper.togglePanningMode();
        } else if (iconIndex == 0) {
            if (paper.isPanning())
                paper.togglePanningMode();
            if (paper.isErasing())
                paper.toggleEraseMode();
        } else if (iconIndex == 3) {
            // save to jpg
            paper.clearPaperStates();
            saveCurrentPaper();
        }
    }

    // save to jpg
    void saveCurrentPaper() {
        Bitmap bitmap = Bitmap.createBitmap(paper.getWidth(), paper.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paper.drawSelf(canvas);
        String fileName = paper.getContext()
                .getResources()
                .getString(R.string.app_name) +
                (System.currentTimeMillis() / 1000 % 100000) + ".png";
        try {
            File dir = new File(paper.getContext().getExternalMediaDirs()[0].getAbsolutePath() + "/images");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Toast.makeText(paper.getContext(),"making directory failed", Toast.LENGTH_LONG).show();
                }
            }
            File outFile = new File(dir.getAbsolutePath() + "/" + fileName);
            outFile.createNewFile();
            FileOutputStream out = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            MediaStore.Images.Media.insertImage(
                    paper.getContext().getContentResolver(),
                    outFile.getAbsolutePath(),
                    fileName,
                    "saved drawing");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(paper.getContext(),"save failed", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(paper.getContext(),"saved to Images", Toast.LENGTH_LONG).show();
    }
}
