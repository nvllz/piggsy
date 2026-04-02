package com.nvllz.piggsy.util;

import android.content.Context;

import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;

import org.json.JSONArray;
import org.json.JSONObject;

public class BackupJsonExporter {

    public static JSONObject export(Context context) throws Exception {
        SavingRepository savingRepo = new SavingRepository(context);
        TransactionRepository transactionRepo = new TransactionRepository(context);

        JSONArray savingsArray = new JSONArray();
        JSONArray transactionsArray = new JSONArray();

        for (Saving saving : savingRepo.getAllSavings()) {
            JSONObject obj = new JSONObject();
            obj.put(Database.COLUMN_SAVING_ID, saving.getID());
            obj.put(Database.COLUMN_SAVING_NAME, saving.getName());
            obj.put(Database.COLUMN_SAVING_CURRENT_SAVING, saving.getCurrentSaving());
            obj.put(Database.COLUMN_SAVING_GOAL, saving.getGoal());
            obj.put(Database.COLUMN_SAVING_DESCRIPTION, saving.getDescription());
            obj.put(Database.COLUMN_SAVING_IS_ARCHIVED, saving.getIsArchived());
            obj.put(Database.COLUMN_SAVING_DEADLINE, saving.getDeadline());
            obj.put(Database.COLUMN_SAVING_CURRENCY, saving.getCurrency());
            savingsArray.put(obj);
        }

        for (Transaction t : transactionRepo.getAll()) {
            JSONObject obj = new JSONObject();
            obj.put(Database.COLUMN_TRANSACTION_ID, t.getID());
            obj.put(Database.COLUMN_TRANSACTION_SAVING_ID, t.getSavingID());
            obj.put(Database.COLUMN_TRANSACTION_AMOUNT, t.getAmount());
            obj.put(Database.COLUMN_TRANSACTION_TYPE, t.getType());
            obj.put(Database.COLUMN_TRANSACTION_DATE, t.getDate());
            obj.put(Database.COLUMN_TRANSACTION_NOTE, t.getNote());
            transactionsArray.put(obj);
        }

        JSONObject root = new JSONObject();
        root.put(Database.TABLE_SAVING, savingsArray);
        root.put(Database.TABLE_TRANSACTION, transactionsArray);

        return root;
    }
}
