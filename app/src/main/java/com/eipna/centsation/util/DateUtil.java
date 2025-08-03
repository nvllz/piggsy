package com.eipna.centsation.util;

import android.icu.text.SimpleDateFormat;
import android.content.Context;

import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static String getStringDateTime(long timeStamp, Context context) {
        PreferenceUtil preferenceUtil = new PreferenceUtil(context);
        String datePattern = preferenceUtil.getDateFormat();
        String fullPattern = datePattern + " HH:mm";
        SimpleDateFormat dateFormat = new SimpleDateFormat(fullPattern, Locale.getDefault());
        return dateFormat.format(new Date(timeStamp));
    }

    public static String getStringDate(long timeStamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return dateFormat.format(new Date(timeStamp));
    }
}