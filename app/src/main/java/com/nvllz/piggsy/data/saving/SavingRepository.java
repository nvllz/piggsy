package com.nvllz.piggsy.data.saving;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.data.transaction.TransactionType;

import java.util.ArrayList;

public class SavingRepository extends Database {

    private final TransactionRepository transactionRepository;

    public SavingRepository(@Nullable Context context) {
        super(context);
        transactionRepository = new TransactionRepository(context);
    }

    public void create(Saving createdSaving) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = getContentValues(createdSaving);
        database.insert(TABLE_SAVING, null, values);

        Transaction initialTransaction = new Transaction();
        initialTransaction.setSavingID(createdSaving.getID());
        initialTransaction.setAmount(createdSaving.getCurrentSaving());
        initialTransaction.setType(TransactionType.CREATED.VALUE);
        transactionRepository.create(initialTransaction);
        database.close();
    }

    @NonNull
    private static ContentValues getContentValues(Saving createdSaving) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_SAVING_ID, createdSaving.getID());
        values.put(COLUMN_SAVING_NAME, createdSaving.getName());
        values.put(COLUMN_SAVING_CURRENT_SAVING, createdSaving.getCurrentSaving());
        values.put(COLUMN_SAVING_GOAL, createdSaving.getGoal());
        values.put(COLUMN_SAVING_DESCRIPTION, createdSaving.getDescription());
        values.put(COLUMN_SAVING_IS_ARCHIVED, createdSaving.getIsArchived());
        values.put(COLUMN_SAVING_DEADLINE, createdSaving.getDeadline());
        values.put(COLUMN_SAVING_CURRENCY, createdSaving.getCurrency());
        return values;
    }

    public void edit(Saving editedSaving) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SAVING_NAME, editedSaving.getName());
        values.put(COLUMN_SAVING_CURRENT_SAVING, editedSaving.getCurrentSaving());
        values.put(COLUMN_SAVING_GOAL, editedSaving.getGoal());
        values.put(COLUMN_SAVING_DESCRIPTION, editedSaving.getDescription());
        values.put(COLUMN_SAVING_IS_ARCHIVED, editedSaving.getIsArchived());
        values.put(COLUMN_SAVING_DEADLINE, editedSaving.getDeadline());
        values.put(COLUMN_SAVING_CURRENCY, editedSaving.getCurrency());

        database.update(TABLE_SAVING, values, COLUMN_SAVING_ID + " = ?", new String[]{editedSaving.getID()});
        database.close();
    }

    public void delete(String savingID) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_SAVING, COLUMN_SAVING_ID + " = ?", new String[]{savingID});
        database.close();
    }

    public void makeTransaction(Saving updatedSaving, double amount, TransactionType type, String note) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SAVING_CURRENT_SAVING, updatedSaving.getCurrentSaving());
        database.update(TABLE_SAVING, values, COLUMN_SAVING_ID + " = ?", new String[]{updatedSaving.getID()});

        Transaction transaction = new Transaction();
        transaction.setSavingID(updatedSaving.getID());
        transaction.setAmount(Math.abs(amount));
        transaction.setType(type.VALUE);
        transaction.setDate(System.currentTimeMillis());
        transaction.setNote(note);
        transactionRepository.create(transaction);
        database.close();
    }

    public ArrayList<Saving> getSavings(int isArchive) {
        ArrayList<Saving> list = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SAVING + " WHERE " + COLUMN_SAVING_IS_ARCHIVED + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(isArchive)});

        if (cursor.moveToFirst()) {
            do {
                Saving queriedSaving = new Saving();
                queriedSaving.setID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_ID)));
                queriedSaving.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_NAME)));
                queriedSaving.setCurrentSaving(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_CURRENT_SAVING)));
                queriedSaving.setGoal(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_GOAL)));
                queriedSaving.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DESCRIPTION)));
                queriedSaving.setIsArchived(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAVING_IS_ARCHIVED)));
                queriedSaving.setDeadline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DEADLINE)));
                queriedSaving.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(Database.COLUMN_SAVING_CURRENCY)));
                list.add(queriedSaving);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return list;
    }

    public ArrayList<Saving> getAllSavings() {
        ArrayList<Saving> list = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_SAVING, null);

        if (cursor.moveToFirst()) {
            do {
                Saving queriedSaving = new Saving();
                queriedSaving.setID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_ID)));
                queriedSaving.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_NAME)));
                queriedSaving.setCurrentSaving(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_CURRENT_SAVING)));
                queriedSaving.setGoal(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_GOAL)));
                queriedSaving.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DESCRIPTION)));
                queriedSaving.setIsArchived(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAVING_IS_ARCHIVED)));
                queriedSaving.setDeadline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DEADLINE)));
                queriedSaving.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_CURRENCY)));
                list.add(queriedSaving);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return list;
    }

    public Saving getSaving(String savingID) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_SAVING + " WHERE " + Database.COLUMN_SAVING_ID + " = ?", new String[]{savingID});

        if (cursor.moveToFirst()) {
            Saving queriedSaving = new Saving();
            queriedSaving.setID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_ID)));
            queriedSaving.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_NAME)));
            queriedSaving.setCurrentSaving(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_CURRENT_SAVING)));
            queriedSaving.setGoal(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVING_GOAL)));
            queriedSaving.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DESCRIPTION)));
            queriedSaving.setIsArchived(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAVING_IS_ARCHIVED)));
            queriedSaving.setDeadline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DEADLINE)));
            queriedSaving.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(Database.COLUMN_SAVING_CURRENCY)));

            cursor.close();
            database.close();
            return queriedSaving;
        }
        cursor.close();
        database.close();
        return null;
    }
}