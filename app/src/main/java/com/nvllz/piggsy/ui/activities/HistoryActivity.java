package com.nvllz.piggsy.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.ui.ComposeHistoryView;

import java.util.ArrayList;

public class HistoryActivity extends BaseActivity implements ComposeHistoryView.Listener {

    private ComposeHistoryView historyView;
    private String selectedSavingID;
    private String currency;
    private ArrayList<Transaction> transactions;
    private TransactionRepository transactionRepository;
    private SavingRepository savingRepository;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transactionRepository != null) {
            transactionRepository.close();
        }
        if (savingRepository != null) {
            savingRepository.close();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        selectedSavingID = getIntent().getStringExtra(Database.COLUMN_SAVING_ID);

        transactionRepository = new TransactionRepository(this);
        savingRepository = new SavingRepository(this);

        transactions = transactionRepository.get(selectedSavingID);

        Saving saving = savingRepository.getSaving(selectedSavingID);
        if (saving == null) {
            finish();
            return;
        }

        currency = saving.getCurrency();

        historyView = new ComposeHistoryView(this, this);
        historyView.updateData(saving.getName(), currency, transactions,
                saving.getIsArchived() != Saving.IS_ARCHIVE);
        setContentView(historyView);
    }

    @Override
    public void onBack() {
        finish();
    }

    @Override
    public void onEditTransaction(Transaction transaction) {
        historyView.showEditDialog(transaction);
    }

    @Override
    public void onDeleteTransaction(Transaction transaction) {
        historyView.showDeleteDialog(transaction);
    }

    @Override
    public void onDeleteConfirmed(Transaction transaction) {
        deleteTransaction(transaction);
    }

    @Override
    public void onTransactionUpdated(Transaction original, double amount, String type, String note) {
        updateTransaction(original, amount, type, note);
    }

    @Override
    public void onTransactionDateTimeEdit(Transaction transaction) {
        historyView.showDateTimeDialog(transaction);
    }

    @Override
    public void onTransactionDateTimeChanged(Transaction transaction, long millis) {
        changeTransactionDate(transaction, millis);
    }

    private void changeTransactionDate(Transaction transaction, long newDate) {
        int position = positionOf(transaction);
        if (position < 0) return;

        long oldDate = transaction.getDate();
        boolean success = transactionRepository.updateDate(transaction.getID(), newDate);

        if (success) {
            transaction.setDate(newDate);


            transactions = transactionRepository.get(selectedSavingID);
            historyView.updateTransactions(transactions);

            historyView.showUndoToast(
                    getString(R.string.transaction_updated),
                    getString(R.string.undo),
                    () -> {
                        transactionRepository.updateDate(transaction.getID(), oldDate);
                        transaction.setDate(oldDate);

                        transactions = transactionRepository.get(selectedSavingID);
                        historyView.updateTransactions(transactions);
                    },
                    null);
        } else {
            Toast.makeText(this, getString(R.string.error_updating_transaction), Toast.LENGTH_SHORT).show();
        }
    }

    private int positionOf(Transaction transaction) {
        return transactions.indexOf(transaction);
    }

    private void deleteTransaction(Transaction transaction) {
        int position = positionOf(transaction);
        if (position < 0) return;

        boolean success = transactionRepository.delete(transaction.getID());

        if (success) {
            transactions.remove(position);
            historyView.updateTransactions(transactions);

            updateSavingAmount(transaction, true);

            historyView.showUndoToast(
                    getString(R.string.transaction_deleted),
                    getString(R.string.undo),
                    () -> restoreTransaction(position, transaction),
                    null);
        } else {
            Toast.makeText(this, getString(R.string.error_deleting_transaction), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreTransaction(int position, Transaction transaction) {
        long newId = transactionRepository.insert(transaction.getSavingID(),
                transaction.getAmount(), transaction.getType(),
                transaction.getDate(), transaction.getNote());

        if (newId != -1) {
            transaction.setID((int) newId);

            transactions.add(position, transaction);
            historyView.updateTransactions(transactions);

            updateSavingAmount(transaction, false);
        } else {
            Toast.makeText(this, getString(R.string.error_restoring_transaction), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTransaction(Transaction originalTransaction,
                                   double newAmount, String newType, String newNote) {

        int position = positionOf(originalTransaction);
        if (position < 0) return;

        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setID(originalTransaction.getID());
        updatedTransaction.setSavingID(originalTransaction.getSavingID());
        updatedTransaction.setAmount(newAmount);
        updatedTransaction.setType(newType);
        updatedTransaction.setDate(originalTransaction.getDate());
        updatedTransaction.setNote(newNote.isEmpty() ? null : newNote);

        boolean success = transactionRepository.update(updatedTransaction.getID(),
                newAmount, newType, newNote);

        if (success) {
            double originalAmount = originalTransaction.getAmount();
            double originalValue = originalTransaction.getType().equals(TransactionType.WITHDRAW.VALUE)
                    ? -originalAmount : originalAmount;

            double newValue = newType.equals(TransactionType.WITHDRAW.VALUE)
                    ? -newAmount : newAmount;

            double difference = newValue - originalValue;

            if (difference != 0) {
                updateSavingAmountByDifference(difference);
            }

            transactions.set(position, updatedTransaction);
            historyView.updateTransactions(transactions);

            historyView.showUndoToast(
                    getString(R.string.transaction_updated),
                    getString(R.string.undo),
                    () -> {
                        transactionRepository.update(originalTransaction.getID(),
                                originalTransaction.getAmount(),
                                originalTransaction.getType(),
                                originalTransaction.getNote() != null ? originalTransaction.getNote() : "");

                        if (difference != 0) {
                            updateSavingAmountByDifference(-difference);
                        }

                        int currentPosition = positionOf(updatedTransaction);
                        if (currentPosition >= 0) {
                            transactions.set(currentPosition, originalTransaction);
                            historyView.updateTransactions(transactions);
                        }
                    },
                    null);
        } else {
            Toast.makeText(this, getString(R.string.error_updating_transaction), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSavingAmount(Transaction transaction, boolean isDelete) {
        double amount = transaction.getAmount();
        double valueChange = 0;

        if (transaction.getType().equals(TransactionType.DEPOSIT.VALUE)) {
            valueChange = isDelete ? -amount : amount;
        } else if (transaction.getType().equals(TransactionType.WITHDRAW.VALUE)) {
            valueChange = isDelete ? amount : -amount;
        }

        if (valueChange != 0) {
            updateSavingAmountByDifference(valueChange);
        }
    }

    private void updateSavingAmountByDifference(double difference) {
        Saving saving = savingRepository.getSaving(selectedSavingID);
        if (saving != null) {
            double newCurrentSaving = saving.getCurrentSaving() + difference;
            savingRepository.updateCurrentSaving(selectedSavingID, newCurrentSaving);
        }
    }
}
