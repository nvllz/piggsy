package com.nvllz.piggsy.util;

import android.content.Context;

import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupJsonExporter {

    public static final String KEY_TRANSACTIONS = "transactions";
    public static final String KEY_SETTINGS = "settings";

    public static JSONObject export(Context context) throws Exception {
        SavingRepository savingRepo = new SavingRepository(context);
        TransactionRepository transactionRepo = new TransactionRepository(context);

        PreferenceUtil prefs = new PreferenceUtil(context);

        JSONObject settings = new JSONObject();
        settings.put("date_format", prefs.getDateFormat());
        settings.put("theme", prefs.getTheme());
        settings.put("dynamic_colors", prefs.isDynamicColors());
        settings.put("screen_privacy", prefs.isScreenPrivacyEnabled());
        settings.put("sort_criteria", prefs.getSortCriteria());
        settings.put("sort_order", prefs.getSortOrder());

        List<Saving> savings = savingRepo.getAllSavings();
        List<Transaction> transactions = transactionRepo.getAll();

        Map<String, JSONArray> transactionsBySaving = new HashMap<>();
        for (Transaction t : transactions) {
            JSONObject obj = new JSONObject();
            obj.put(Database.COLUMN_TRANSACTION_AMOUNT, t.getAmount());
            obj.put(Database.COLUMN_TRANSACTION_TYPE, t.getType());
            obj.put(Database.COLUMN_TRANSACTION_DATE, t.getDate());

            String note = t.getNote();
            if (note != null && !note.isEmpty()) {
                obj.put(Database.COLUMN_TRANSACTION_NOTE, note);
            }

            transactionsBySaving
                    .computeIfAbsent(t.getSavingID(), k -> new JSONArray())
                    .put(obj);
        }

        JSONArray savingsArray = new JSONArray();
        for (Saving saving : savings) {
            JSONObject obj = new JSONObject();
            obj.put(Database.COLUMN_SAVING_ID, saving.getID());
            obj.put(Database.COLUMN_SAVING_NAME, saving.getName());
            obj.put(Database.COLUMN_SAVING_CURRENT_SAVING, saving.getCurrentSaving());
            obj.put(Database.COLUMN_SAVING_GOAL, saving.getGoal());
            obj.put(Database.COLUMN_SAVING_DESCRIPTION, saving.getDescription());
            obj.put(Database.COLUMN_SAVING_IS_ARCHIVED, saving.getIsArchived());
            obj.put(Database.COLUMN_SAVING_DEADLINE, saving.getDeadline());
            obj.put(Database.COLUMN_SAVING_CURRENCY, saving.getCurrency());

            JSONArray ownTransactions = transactionsBySaving.get(saving.getID());
            obj.put(KEY_TRANSACTIONS, ownTransactions != null ? ownTransactions : new JSONArray());

            savingsArray.put(obj);
        }

        JSONObject root = new JSONObject();
        root.put(Database.TABLE_SAVING, savingsArray);
        root.put(KEY_SETTINGS, settings);
        return root;
    }
}