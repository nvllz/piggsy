package com.nvllz.piggsy.util;

import android.icu.text.SimpleDateFormat;
import android.content.Context;

import java.util.Calendar;
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

    public static Date getTodayWithoutTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getDateWithoutTime(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}