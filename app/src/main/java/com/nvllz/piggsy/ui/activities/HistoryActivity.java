package com.nvllz.piggsy.ui.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.databinding.ActivityHistoryBinding;
import com.nvllz.piggsy.ui.adapters.TransactionAdapter;
import com.nvllz.piggsy.ui.helpers.SwipeToActionCallback;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class HistoryActivity extends BaseActivity implements SwipeToActionCallback.SwipeActionListener {

    private ActivityHistoryBinding binding;
    private TransactionAdapter transactionAdapter;
    private String selectedSavingID;
    private String currency;
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
        binding = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        selectedSavingID = getIntent().getStringExtra(Database.COLUMN_SAVING_ID);

        transactionRepository = new TransactionRepository(this);
        savingRepository = new SavingRepository(this);

        setupRecyclerView();
        loadData();
    }

    private void setupRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SwipeToActionCallback swipeCallback = new SwipeToActionCallback(this, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerView);
    }

    private void loadData() {
        ArrayList<Transaction> transactions = transactionRepository.get(selectedSavingID);
        Collections.reverse(transactions);

        Saving saving = savingRepository.getSaving(selectedSavingID);
        currency = saving.getCurrency();
        String piggyBankName = saving.getName();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(piggyBankName);
        }

        transactionAdapter = new TransactionAdapter(this, transactions, currency);
        binding.recyclerView.setAdapter(transactionAdapter);
    }

    @Override
    public void onSwipeLeft(int position) {
        showDeleteConfirmationDialog(position);
    }

    @Override
    public void onSwipeRight(int position) {
        showEditTransactionDialog(position);
    }

    @Override
    public boolean isTransactionDeletable(int position) {
        Transaction transaction = transactionAdapter.getTransactionAt(position);
        if (transaction == null) return false;

        return !TransactionType.CREATED.VALUE.equals(transaction.getType());
    }

    private void showDeleteConfirmationDialog(int position) {
        Transaction transaction = transactionAdapter.getTransactionAt(position);
        if (transaction == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_delete_transaction_title))
                .setMessage(getString(R.string.dialog_delete_transaction_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) ->
                        deleteTransaction(position))
                .setNegativeButton(getString(R.string.cancel), (dialog, which) ->
                        transactionAdapter.notifyItemChanged(position))
                .setOnCancelListener(dialog ->
                        transactionAdapter.notifyItemChanged(position))
                .show();
    }

    private void deleteTransaction(int position) {
        Transaction transaction = transactionAdapter.getTransactionAt(position);
        if (transaction == null) return;

        boolean success = transactionRepository.delete(transaction.getID());

        if (success) {
            transactionAdapter.removeItem(position);

            updateSavingAmount(transaction, true);

            Snackbar.make(binding.recyclerView,
                            getString(R.string.transaction_deleted),
                            Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo), v ->
                            restoreTransaction(position, transaction))
                    .show();
        } else {
            Toast.makeText(this, getString(R.string.error_deleting_transaction), Toast.LENGTH_SHORT).show();
            transactionAdapter.notifyItemChanged(position);
        }
    }

    private void restoreTransaction(int position, Transaction transaction) {
        long newId = transactionRepository.insert(transaction.getSavingID(),
                transaction.getAmount(), transaction.getType(),
                transaction.getDate(), transaction.getNote());

        if (newId != -1) {
            transaction.setID((int) newId);

            transactionAdapter.restoreItem(position, transaction);

            updateSavingAmount(transaction, false);
        } else {
            Toast.makeText(this, getString(R.string.error_restoring_transaction), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditTransactionDialog(int position) {
        Transaction transaction = transactionAdapter.getTransactionAt(position);
        if (transaction == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saving_transaction, null);

        TextInputEditText amountField = dialogView.findViewById(R.id.field_saving_amount_text);
        TextInputEditText noteField = dialogView.findViewById(R.id.field_transaction_note_text);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggle_group_transaction_type);

        com.google.android.material.textfield.TextInputLayout amountLayout =
                dialogView.findViewById(R.id.field_saving_amount_layout);
        String currencySymbol = com.nvllz.piggsy.data.Currency.getSymbol(currency);
        amountLayout.setPrefixText(currencySymbol + "  ");

        double transaction_amount = transaction.getAmount();
        String formattedAmount;
        if (transaction_amount == (long) transaction_amount) {
            formattedAmount = String.valueOf((long) transaction_amount);
        } else {
            formattedAmount = String.valueOf(transaction_amount);
        }
        amountField.setText(formattedAmount);
        noteField.setText(transaction.getNote());

        amountField.requestFocus();
        amountField.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(amountField, InputMethodManager.SHOW_IMPLICIT);
        }, 300);

        if (transaction.getType().equals(TransactionType.DEPOSIT.VALUE)) {
            toggleGroup.check(R.id.button_deposit);
        } else if (transaction.getType().equals(TransactionType.WITHDRAW.VALUE)) {
            toggleGroup.check(R.id.button_withdraw);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_edit_transaction_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), (d, which) ->
                        transactionAdapter.notifyItemChanged(position))
                .setOnCancelListener(d ->
                        transactionAdapter.notifyItemChanged(position))
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountStr = Objects.requireNonNull(amountField.getText()).toString().trim();
            String note = Objects.requireNonNull(noteField.getText()).toString().trim();

            if (amountStr.isEmpty()) {
                amountField.setError(getString(R.string.error_amount_required));
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount < 0 && !transaction.getType().equals(TransactionType.CREATED.VALUE)) {
                    amountField.setError(getString(R.string.error_amount_non_negative));
                    return;
                } else if (amount == 0 && !transaction.getType().equals(TransactionType.CREATED.VALUE)) {
                    amountField.setError(getString(R.string.error_amount_positive));
                    return;
                }

                String transactionType;
                if (transaction.getType().equals(TransactionType.CREATED.VALUE)) {
                    transactionType = TransactionType.CREATED.VALUE;
                } else if (toggleGroup.getCheckedButtonId() == R.id.button_withdraw) {
                    transactionType = TransactionType.WITHDRAW.VALUE;
                } else {
                    transactionType = TransactionType.DEPOSIT.VALUE;
                }

                updateTransaction(position, transaction, amount, transactionType, note);
                dialog.dismiss();

            } catch (NumberFormatException e) {
                amountField.setError(getString(R.string.error_invalid_amount));
            }
        });
    }

    private void updateTransaction(int position, Transaction originalTransaction,
                                   double newAmount, String newType, String newNote) {

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

            transactionAdapter.updateItem(position, updatedTransaction);

            Toast.makeText(this, getString(R.string.transaction_updated), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.error_updating_transaction), Toast.LENGTH_SHORT).show();
            transactionAdapter.notifyItemChanged(position);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return true;
    }
}