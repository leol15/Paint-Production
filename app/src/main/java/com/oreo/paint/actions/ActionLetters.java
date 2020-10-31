package com.oreo.paint.actions;

import android.content.Context;
import android.util.Log;

import java.util.Random;

public class ActionLetters extends ActionNumbers {

    Random random;

    public ActionLetters(Context context) {
        super(context);
        random = new Random();
    }



    int letter = 'A';
    @Override
    void addThingToString() {
//        stringBuilder.append((char) letter++).append("  ");

        // some fum todo
        try {
            stringBuilder.append((char) (0x2000 + random.nextInt(0x2e77 - 0x2000)));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "addThingToString: failed");
        }
        stringBuilder.append("  ");
    }

    @Override
    AbstractPaintActionExtendsView duplicateImp() {
        ActionLetters re = new ActionLetters(getContext());
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
