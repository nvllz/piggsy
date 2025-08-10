package com.nvllz.piggsy.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "piggsy.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_SAVING = "savings";
    public static final String COLUMN_SAVING_ID = "id";
    public static final String COLUMN_SAVING_NAME = "name";
    public static final String COLUMN_SAVING_CURRENT_SAVING = "current_saving";
    public static final String COLUMN_SAVING_GOAL = "goal";
    public static final String COLUMN_SAVING_DESCRIPTION = "description";
    public static final String COLUMN_SAVING_IS_ARCHIVED = "is_archived";
    public static final String COLUMN_SAVING_DEADLINE = "deadline";
    public static final String COLUMN_SAVING_CURRENCY = "currency";

    public static final String TABLE_TRANSACTION = "transactions";
    public static final String COLUMN_TRANSACTION_ID = "id";
    public static final String COLUMN_TRANSACTION_SAVING_ID = "saving_id";
    public static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_TRANSACTION_TYPE = "type";
    public static final String COLUMN_TRANSACTION_DATE = "date";
    public static final String COLUMN_TRANSACTION_NOTE = "note";

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createSavingTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SAVING + "(" +
                COLUMN_SAVING_ID + " TEXT PRIMARY KEY, " +
                COLUMN_SAVING_NAME + " TEXT NOT NULL, " +
                COLUMN_SAVING_CURRENT_SAVING+ " REAL NOT NULL, " +
                COLUMN_SAVING_GOAL + " REAL NOT NULL, " +
                COLUMN_SAVING_DESCRIPTION + " TEXT, " +
                COLUMN_SAVING_IS_ARCHIVED + " INTEGER NOT NULL, " +
                COLUMN_SAVING_DEADLINE + " INTEGER, " +
                COLUMN_SAVING_CURRENCY + " TEXT NOT NULL DEFAULT 'USD');";

        String createTransactionTable = "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTION + "(" +
                COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TRANSACTION_SAVING_ID + " TEXT NOT NULL, " +
                COLUMN_TRANSACTION_AMOUNT + " REAL NOT NULL, " +
                COLUMN_TRANSACTION_TYPE + " TEXT NOT NULL," +
                COLUMN_TRANSACTION_DATE + " INTEGER NOT NULL, " +
                COLUMN_TRANSACTION_NOTE + " TEXT, " +
                "FOREIGN KEY (" + COLUMN_TRANSACTION_SAVING_ID + ") REFERENCES " + TABLE_SAVING + "(" + COLUMN_SAVING_ID + ") ON DELETE CASCADE);";

        sqLiteDatabase.execSQL(createSavingTable);
        sqLiteDatabase.execSQL(createTransactionTable);
    }

    @Override
    public void onOpen(SQLiteDatabase sqLiteDatabase) {
        super.onOpen(sqLiteDatabase);
        sqLiteDatabase.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_NOTE + " TEXT");
        }
        if (oldVersion < 3) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_SAVING + " ADD COLUMN " + COLUMN_SAVING_CURRENCY + " TEXT NOT NULL DEFAULT 'USD'");
        }
    }
}