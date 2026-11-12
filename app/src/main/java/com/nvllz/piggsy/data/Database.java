package com.nvllz.piggsy.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.util.AlarmUtil;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "piggsy.db";
    private static final int DATABASE_VERSION = 7;

    private final Context appContext;

    public static final String TABLE_SAVING = "savings";
    public static final String COLUMN_SAVING_ID = "id";
    public static final String COLUMN_SAVING_NAME = "name";
    public static final String COLUMN_SAVING_CURRENT_SAVING = "current_saving";
    public static final String COLUMN_SAVING_GOAL = "goal";
    public static final String COLUMN_SAVING_DESCRIPTION = "description";
    public static final String COLUMN_SAVING_IS_ARCHIVED = "is_archived";
    public static final String COLUMN_SAVING_ARCHIVED_AT = "archived_at";
    public static final String COLUMN_SAVING_DEADLINE = "deadline";
    public static final String COLUMN_SAVING_CURRENCY = "currency";
    public static final String COLUMN_SAVING_ARCHIVE_WHEN_COMPLETED = "archive_when_completed";

    public static final String TABLE_TRANSACTION = "transactions";
    public static final String COLUMN_TRANSACTION_ID = "id";
    public static final String COLUMN_TRANSACTION_SAVING_ID = "saving_id";
    public static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_TRANSACTION_TYPE = "type";
    public static final String COLUMN_TRANSACTION_DATE = "date";
    public static final String COLUMN_TRANSACTION_NOTE = "note";

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        appContext = context.getApplicationContext();
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
                COLUMN_SAVING_ARCHIVED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                COLUMN_SAVING_DEADLINE + " INTEGER, " +
                COLUMN_SAVING_CURRENCY + " TEXT NOT NULL DEFAULT 'USD', " +
                COLUMN_SAVING_ARCHIVE_WHEN_COMPLETED + " INTEGER NOT NULL DEFAULT 0);";

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
        if (oldVersion < 4) {
            sqLiteDatabase.execSQL(
                    "UPDATE " + TABLE_SAVING + " SET " +
                            COLUMN_SAVING_CURRENT_SAVING + " = ROUND(" + COLUMN_SAVING_CURRENT_SAVING + ", 2), " +
                            COLUMN_SAVING_GOAL + " = ROUND(" + COLUMN_SAVING_GOAL + ", 2);"
            );
            sqLiteDatabase.execSQL(
                    "UPDATE " + TABLE_TRANSACTION + " SET " +
                            COLUMN_TRANSACTION_AMOUNT + " = ROUND(" + COLUMN_TRANSACTION_AMOUNT + ", 2);"
            );
        }
        if (oldVersion < 5) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_SAVING + " ADD COLUMN " +
                    COLUMN_SAVING_ARCHIVED_AT + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 6) {

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            android.database.Cursor cursor = sqLiteDatabase.query(
                    TABLE_SAVING, null, COLUMN_SAVING_DEADLINE + " > 0", null, null, null, null);
            while (cursor.moveToNext()) {
                long deadline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SAVING_DEADLINE));
                calendar.setTimeInMillis(deadline);
                if (calendar.get(java.util.Calendar.HOUR_OF_DAY) != 0
                        || calendar.get(java.util.Calendar.MINUTE) != 0
                        || calendar.get(java.util.Calendar.SECOND) != 0
                        || calendar.get(java.util.Calendar.MILLISECOND) != 0) {
                    continue;
                }
                calendar.add(java.util.Calendar.HOUR_OF_DAY, 12);

                Saving saving = new Saving();
                saving.setID(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_ID)));
                saving.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVING_NAME)));
                saving.setIsArchived(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAVING_IS_ARCHIVED)));
                saving.setDeadline(calendar.getTimeInMillis());

                sqLiteDatabase.execSQL(
                        "UPDATE " + TABLE_SAVING + " SET " + COLUMN_SAVING_DEADLINE + " = ? WHERE " +
                                COLUMN_SAVING_ID + " = ?",
                        new Object[]{calendar.getTimeInMillis(), saving.getID()});
                AlarmUtil.set(appContext, saving);
            }
            cursor.close();
        }
        if (oldVersion < 7) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_SAVING + " ADD COLUMN " +
                    COLUMN_SAVING_ARCHIVE_WHEN_COMPLETED + " INTEGER NOT NULL DEFAULT 0");
        }
    }
}