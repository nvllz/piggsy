package com.nvllz.piggsy.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.DateFormat;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.data.saving.SavingSort;

import java.util.Locale;

public class PreferenceUtil {

    private final SharedPreferences sharedPreferences;

    public PreferenceUtil(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isDynamicColors() {
        return sharedPreferences.getBoolean("dynamic_colors", false);
    }

    public void setDynamicColors(boolean value) {
        sharedPreferences.edit().putBoolean("dynamic_colors", value).apply();
    }

    public String getTheme() {
        return sharedPreferences.getString("theme", Theme.SYSTEM.VALUE);
    }

    public void setTheme(String value) {
        sharedPreferences.edit().putString("theme", value).apply();
    }

    public String getCurrency() {
        return sharedPreferences.getString("currency", getDefaultCurrencyByLocale());
    }

    public void setCurrency(String value) {
        sharedPreferences.edit().putString("currency", value).apply();
    }

    private String getDefaultCurrencyByLocale() {
        try {
            Locale currentLocale = Locale.getDefault();

            java.util.Currency localeCurrency = java.util.Currency.getInstance(currentLocale);
            String currencyCode = localeCurrency.getCurrencyCode();

            if (isCurrencySupported(currencyCode)) {
                return currencyCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Currency.UNITED_STATES_DOLLAR.CODE;
    }

    private boolean isCurrencySupported(String currencyCode) {
        for (Currency currency : Currency.values()) {
            if (currency.CODE.equals(currencyCode)) {
                return true;
            }
        }
        return false;
    }

    public void setSortCriteria(String value) {
        sharedPreferences.edit().putString("sort_criteria", value).apply();
    }

    public String getSortCriteria() {
        return sharedPreferences.getString("sort_criteria", SavingSort.NAME.SORT);
    }

    public void setSortOrder(boolean value) {
        sharedPreferences.edit().putBoolean("sort_order", value).apply();
    }

    public boolean getSortOrder() {
        return sharedPreferences.getBoolean("sort_order", true);
    }

    public void setDateFormat(String value) {
        sharedPreferences.edit().putString("date_format", value).apply();
    }

    public String getDateFormat() {
        return sharedPreferences.getString("date_format", DateFormat.YYYY_MM_DD_ISO.PATTERN);
    }

    public void setScreenPrivacy(boolean value) {
        sharedPreferences.edit().putBoolean("screen_privacy", value).apply();
    }

    public boolean isScreenPrivacyEnabled() {
        return sharedPreferences.getBoolean("screen_privacy", false);
    }
}