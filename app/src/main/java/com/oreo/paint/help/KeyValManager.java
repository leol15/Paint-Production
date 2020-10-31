package com.oreo.paint.help;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class KeyValManager {
    // can save and retrieve key val pairs

    static final String DEFAULT_STORAGE_NAME = "oreo.paint.DEFAULT_STORAGE";

    static Context context;
    public static void setContext(Context c) {
        context = c;
    }

    // setter
    public static <V> void put(String key, V value) {
        SharedPreferences sp = context.getSharedPreferences(DEFAULT_STORAGE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String inClassName = value.getClass().getName();
        if (inClassName.equals(Boolean.class.getName())) {
            editor.putBoolean(key, (Boolean) value);
        } else if (inClassName.equals(Integer.class.getName())) {
            editor.putInt(key, (Integer) value);
        } else if (inClassName.equals(Float.class.getName())) {
            editor.putFloat(key, (Float) value);
        } else if (inClassName.equals(Long.class.getName())) {
            editor.putLong(key, (Long) value);
        } else if (inClassName.equals(String.class.getName())) {
            editor.putString(key, (String) value);
        } else {
            Log.e(DEFAULT_STORAGE_NAME, "put: cannot put value " + value);
        }
        editor.apply();
    }

    // getter
    public static <T> Object get(String key, T classType) {
        SharedPreferences sp = context.getSharedPreferences(DEFAULT_STORAGE_NAME, Context.MODE_PRIVATE);
        String className = classType.getClass().getName();
        if (className.equals(Boolean.class.getName())) {
            return sp.getBoolean(key, false);
        } else if (className.equals(Integer.class.getName())) {
            return sp.getInt(key, -1);
        } else if (className.equals(Float.class.getName())) {
            return sp.getFloat(key, -1);
        } else if (className.equals(Long.class.getName())) {
            return sp.getLong(key, -1);
        } else if (className.equals(String.class.getName())) {
            return sp.getString(key, "");
        } else {
            Log.e(DEFAULT_STORAGE_NAME, "put: cannot get class type " + className);
        }
        return null;
    }
}
