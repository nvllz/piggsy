package com.nvllz.piggsy.ui.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
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
import java.util.Objects;

public class HistoryActivity extends BaseActivity implements SwipeToActionCallback.SwipeActionListener {

    private ActivityHistoryBinding binding;
    private TransactionAdapter transactionAdapter;
    private String selectedSavingID;
    private String currency;
    private TransactionRepository transactionRepository;
    private SavingRepository savingRepository;
    private SwipeToActionCallback swipeCallback;
    private boolean isHintAnimationRunning = false;
    private android.animation.AnimatorSet hintAnimatorSet;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hintAnimatorSet != null) {
            hintAnimatorSet.cancel();
            hintAnimatorSet = null;
        }
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

        swipeCallback = new SwipeToActionCallback(this, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerView);
    }

    private void loadData() {
        ArrayList<Transaction> transactions = transactionRepository.get(selectedSavingID);

        Saving saving = savingRepository.getSaving(selectedSavingID);
        currency = saving.getCurrency();
        String piggyBankName = saving.getName();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(piggyBankName);
        }

        transactionAdapter = new TransactionAdapter(this, transactions, currency);
        binding.recyclerView.setAdapter(transactionAdapter);

        showSwipeHint();
    }

    private void showSwipeHint() {
        if (binding == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("piggsy_prefs", MODE_PRIVATE);
        boolean hintShown = prefs.getBoolean("swipe_hint_shown", false);

        if (!hintShown && transactionAdapter.getItemCount() > 0) {
            binding.recyclerView.postDelayed(this::playSwipeHintAnimation, 500);
            prefs.edit().putBoolean("swipe_hint_shown", true).apply();
        }
    }

    private void playSwipeHintAnimation() {
        if (binding == null) {
            isHintAnimationRunning = false;
            return;
        }

        androidx.recyclerview.widget.RecyclerView.ViewHolder vh =
                binding.recyclerView.findViewHolderForAdapterPosition(0);
        if (vh == null) {
            isHintAnimationRunning = false;
            return;
        }

        View itemView = vh.itemView;
        boolean canDelete = !TransactionType.CREATED.VALUE.equals(
                transactionAdapter.getTransactionAt(0).getType());
        float editPeek = itemView.getWidth() * 0.22f;
        float deletePeek = -itemView.getWidth() * 0.22f;

        androidx.recyclerview.widget.RecyclerView.ItemDecoration hintDecoration =
                new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                    @Override
                    public void onDraw(@NonNull android.graphics.Canvas c,
                                       @NonNull androidx.recyclerview.widget.RecyclerView parent,
                                       @NonNull androidx.recyclerview.widget.RecyclerView.State state) {
                        swipeCallback.drawSwipeBackground(c, itemView, itemView.getTranslationX());
                    }
                };
        binding.recyclerView.addItemDecoration(hintDecoration);

        java.util.ArrayList<android.animation.Animator> sequence = new java.util.ArrayList<>();

        sequence.add(makeSlideAnimator(itemView, 0f, editPeek, 250));
        sequence.add(makePause(700));
        sequence.add(makeSlideAnimator(itemView, editPeek, 0f, 250));
        sequence.add(makePause(100));

        if (canDelete) {
            sequence.add(makeSlideAnimator(itemView, 0f, deletePeek, 250));
            sequence.add(makePause(700));
            sequence.add(makeSlideAnimator(itemView, deletePeek, 0f, 250));
        }

        android.animation.AnimatorSet fullSequence = new android.animation.AnimatorSet();
        fullSequence.playSequentially(sequence);
        fullSequence.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                itemView.setTranslationX(0f);
                if (binding != null) {
                    binding.recyclerView.removeItemDecoration(hintDecoration);
                    binding.recyclerView.invalidateItemDecorations();
                }
                isHintAnimationRunning = false;
            }
        });
        hintAnimatorSet = fullSequence;
        fullSequence.start();
    }

    private android.animation.ValueAnimator makeSlideAnimator(View itemView, float from, float to, long duration) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            itemView.setTranslationX((float) anim.getAnimatedValue());
            if (binding != null) {
                binding.recyclerView.invalidateItemDecorations();
            }
        });
        return animator;
    }

    private android.animation.ValueAnimator makePause(long duration) {
        android.animation.ValueAnimator pause = android.animation.ValueAnimator.ofFloat(0f, 0f);
        pause.setDuration(duration);
        return pause;
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
            transactionAdapter.refreshSubtotals();

            updateSavingAmount(transaction, true);

            Snackbar.make(binding.recyclerView,
                            getString(R.string.transaction_deleted),
                            Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo), v -> {
                        restoreTransaction(position, transaction);
                        transactionAdapter.refreshSubtotals();
                    })
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
            transactionAdapter.refreshSubtotals();

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
                amountField.setError(getString(R.string.field_error_required));
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
            transactionAdapter.refreshSubtotals();

            Snackbar.make(binding.recyclerView, getString(R.string.transaction_updated), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo), v -> {
                        transactionRepository.update(originalTransaction.getID(),
                                originalTransaction.getAmount(),
                                originalTransaction.getType(),
                                originalTransaction.getNote() != null ? originalTransaction.getNote() : "");

                        if (difference != 0) {
                            updateSavingAmountByDifference(-difference);
                        }

                        transactionAdapter.updateItem(position, originalTransaction);
                        transactionAdapter.refreshSubtotals();
                    })
                    .show();
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
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_swipe_hint) {
            triggerSwipeHint();
            return true;
        }
        return true;
    }

    @Override
    public boolean isSavingArchived(int position) {
        Saving saving = savingRepository.getSaving(selectedSavingID);
        return saving != null && saving.getIsArchived() == Saving.IS_ARCHIVE;
    }

    private void triggerSwipeHint() {
        if (isHintAnimationRunning) return;
        if (binding == null || transactionAdapter == null || transactionAdapter.getItemCount() == 0) return;

        isHintAnimationRunning = true;
        playSwipeHintAnimation();

        binding.recyclerView.postDelayed(() -> isHintAnimationRunning = false, 3000);
    }
}