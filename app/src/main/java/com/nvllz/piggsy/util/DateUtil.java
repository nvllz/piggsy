package com.nvllz.piggsy.util;

import android.icu.text.SimpleDateFormat;
import android.content.Context;
import android.text.format.DateFormat;

import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static String getStringDateTime(long timeStamp, Context context) {
        PreferenceUtil preferenceUtil = new PreferenceUtil(context);
        String datePattern = preferenceUtil.getDateFormat();
        return getStringDateTime(timeStamp, datePattern, context);
    }

    public static String getStringDate(long timeStamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return dateFormat.format(new Date(timeStamp));
    }


    public static String getStringDateTime(long timeStamp, String pattern, Context context) {
        String timePattern = DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern + " · " + timePattern, Locale.getDefault());
        return dateFormat.format(new Date(timeStamp));
    }

}