package com.eipna.centsation.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eipna.centsation.R;
import com.eipna.centsation.data.Currency;
import com.eipna.centsation.data.Database;
import com.eipna.centsation.data.saving.Saving;
import com.eipna.centsation.data.saving.SavingOperation;
import com.eipna.centsation.data.saving.SavingRepository;
import com.eipna.centsation.data.saving.SavingSort;
import com.eipna.centsation.data.transaction.Transaction;
import com.eipna.centsation.data.transaction.TransactionRepository;
import com.eipna.centsation.data.transaction.TransactionType;
import com.eipna.centsation.databinding.ActivityMainBinding;
import com.eipna.centsation.ui.adapters.SavingAdapter;
import com.eipna.centsation.ui.adapters.TransactionAdapter;
import com.eipna.centsation.util.AlarmUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class MainActivity extends BaseActivity implements SavingAdapter.Listener {

    private ActivityMainBinding binding;
    private SavingRepository savingRepository;
    private TransactionRepository transactionRepository;
    private SavingAdapter savingAdapter;
    private ArrayList<Saving> savings;

    private String sortCriteria;
    private boolean isSortAscending;

    private final ActivityResultLauncher<Intent> createSavingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshList();
                }
            });

    private final ActivityResultLauncher<Intent> editSavingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshList();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        Drawable appBarDrawable = MaterialShapeDrawable.createWithElevationOverlay(this);
        binding.appBar.setStatusBarForeground(appBarDrawable);

        savings = new ArrayList<>();
        savingRepository = new SavingRepository(this);
        transactionRepository = new TransactionRepository(this);

        sortCriteria = preferences.getSortCriteria();
        isSortAscending = preferences.getSortOrder();

        savings.addAll(savingRepository.getSavings(Saving.NOT_ARCHIVE));
        savingAdapter = new SavingAdapter(this, this, savings);
        sortSavings(sortCriteria);

        binding.emptyIndicator.setVisibility(savings.isEmpty() ? View.VISIBLE : View.GONE);
        binding.savingList.setVisibility(savings.isEmpty() ? View.GONE : View.VISIBLE);

        binding.savingList.setLayoutManager(new LinearLayoutManager(this));
        binding.savingList.setAdapter(savingAdapter);

        binding.createSaving.setOnClickListener(v -> createSavingLauncher.launch(new Intent(MainActivity.this, CreateActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshList() {
        savings.clear();
        savings.addAll(savingRepository.getSavings(Saving.NOT_ARCHIVE));
        sortSavings(sortCriteria);

        binding.emptyIndicator.setVisibility(savings.isEmpty() ? View.VISIBLE : View.GONE);
        binding.savingList.setVisibility(savings.isEmpty() ? View.GONE : View.VISIBLE);
        savingAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);
        loadSortingMenu(menu);

        return true;
    }

    private void loadSortingMenu(Menu menu) {
        if (sortCriteria.equals(SavingSort.NAME.SORT)) {
            menu.findItem(R.id.sort_name).setChecked(true);
        } else if (sortCriteria.equals(SavingSort.VALUE.SORT)) {
            menu.findItem(R.id.sort_value).setChecked(true);
        } else if (sortCriteria.equals(SavingSort.GOAL.SORT)) {
            menu.findItem(R.id.sort_goal).setChecked(true);
        } else if (sortCriteria.equals(SavingSort.DEADLINE.SORT)) {
            menu.findItem(R.id.sort_deadline).setChecked(true);
        }

        if (isSortAscending) {
            menu.findItem(R.id.sort_ascending).setChecked(true);
        } else {
            menu.findItem(R.id.sort_descending).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.archive) startActivity(new Intent(this, ArchiveActivity.class));
        if (item.getItemId() == R.id.settings) startActivity(new Intent(this, SettingsActivity.class));

        if (item.getItemId() == R.id.sort_name) {
            sortCriteria = SavingSort.NAME.SORT;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }

        if (item.getItemId() == R.id.sort_value) {
            sortCriteria = SavingSort.VALUE.SORT;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }

        if (item.getItemId() == R.id.sort_goal) {
            sortCriteria = SavingSort.GOAL.SORT;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }

        if (item.getItemId() == R.id.sort_deadline) {
            sortCriteria = SavingSort.DEADLINE.SORT;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }

        if (item.getItemId() == R.id.sort_ascending) {
            isSortAscending = true;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }

        if (item.getItemId() == R.id.sort_descending) {
            isSortAscending = false;
            item.setChecked(true);
            sortSavings(sortCriteria);
        }
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortSavings(String criteria) {
        Comparator<Saving> savingComparator = null;

        if (criteria.equals(SavingSort.NAME.SORT)) {
            savingComparator = Saving.SORT_NAME;
        } else if (criteria.equals(SavingSort.VALUE.SORT)) {
            savingComparator = Saving.SORT_VALUE;
        } else if (criteria.equals(SavingSort.GOAL.SORT)) {
            savingComparator = Saving.SORT_GOAL;
        } else if (criteria.equals(SavingSort.DEADLINE.SORT)) {
            savingComparator = Saving.SORT_DEADLINE;
        }

        if (savingComparator != null) {
            if (!isSortAscending) {
                savingComparator = savingComparator.reversed();
            }
        }

        savings.sort(savingComparator);
        savingAdapter.notifyDataSetChanged();

        preferences.setSortCriteria(sortCriteria);
        preferences.setSortOrder(isSortAscending);
    }

    private void showHistoryDialog(Saving selectedSaving) {
        View historyDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saving_history, null, false);

        ArrayList<Transaction> transactions = transactionRepository.get(selectedSaving.getID());
        TransactionAdapter transactionAdapter = new TransactionAdapter(this, transactions);

        RecyclerView historyList = historyDialogView.findViewById(R.id.transaction_list);
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setAdapter(transactionAdapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_history_saving)
                .setIcon(R.drawable.ic_history)
                .setView(historyDialogView)
                .setPositiveButton(R.string.dialog_button_close, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteDialog(Saving saving) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_delete_saving)
                .setIcon(R.drawable.ic_delete_forever)
                .setMessage(R.string.dialog_message_delete_saving)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_delete, (dialogInterface, i) -> {
                    AlarmUtil.cancel(this, saving);
                    savingRepository.delete(saving.getID());
                    refreshList();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showShareIntent(String notes) {
        Intent sendIntent = new Intent();
        sendIntent.setType("text/plain");
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, notes);

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void showTransactionDialog(Saving selectedSaving) {
        View transactionDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saving_transaction, null, false);
        String currentCurrencySymbol = Currency.getSymbol(preferences.getCurrency());

        TextInputLayout amountLayout = transactionDialogView.findViewById(R.id.field_saving_amount_layout);
        TextInputEditText amountInput = transactionDialogView.findViewById(R.id.field_saving_amount_text);

        MaterialRadioButton depositOption = transactionDialogView.findViewById(R.id.radio_button_deposit);
        MaterialRadioButton withdrawOption = transactionDialogView.findViewById(R.id.radio_button_withdraw);

        amountLayout.setPrefixText(currentCurrencySymbol);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_create_transaction)
                .setView(transactionDialogView)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_submit, null);

        AlertDialog dialog = builder.create();

        amountInput.requestFocus();
        amountInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(amountInput, InputMethodManager.SHOW_FORCED);
        }, 100);

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountText = Objects.requireNonNull(amountInput.getText()).toString();

            if (amountText.isEmpty()) {
                amountLayout.setError(getString(R.string.field_error_required));
                return;
            }

            if (depositOption.isChecked()) {
                double addedSaving = selectedSaving.getCurrentSaving() + Double.parseDouble(amountText);
                double amount = Double.parseDouble(amountText);

                selectedSaving.setCurrentSaving(addedSaving);
                savingRepository.makeTransaction(selectedSaving, amount, TransactionType.DEPOSIT);

                refreshList();
                dialog.dismiss();
            } else if (withdrawOption.isChecked()) {
                double deductedSaving = selectedSaving.getCurrentSaving() - Double.parseDouble(amountText);
                double amount = Double.parseDouble(amountText);
                if (deductedSaving < 0) {
                    amountLayout.setError(getString(R.string.field_error_negative_saving));
                    return;
                }

                selectedSaving.setCurrentSaving(deductedSaving);
                savingRepository.makeTransaction(selectedSaving, amount, TransactionType.WITHDRAW);

                refreshList();
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private void archiveSaving(Saving selectedSaving) {
        selectedSaving.setIsArchived(Saving.IS_ARCHIVE);
        savingRepository.edit(selectedSaving);
        refreshList();
    }

    @Override
    public void OnClick(int position) {
        Saving selectedSaving = savings.get(position);
        Intent editIntent = new Intent(MainActivity.this, EditActivity.class);
        editIntent.putExtra(Database.COLUMN_SAVING_ID, selectedSaving.getID());
        editSavingLauncher.launch(editIntent);
    }

    @Override
    public void OnOperationClick(SavingOperation operation, int position) {
        Saving selectedSaving = savings.get(position);
        if (operation.equals(SavingOperation.DELETE)) showDeleteDialog(selectedSaving);
        if (operation.equals(SavingOperation.SHARE)) showShareIntent(selectedSaving.getNotes());
        if (operation.equals(SavingOperation.TRANSACTION)) showTransactionDialog(selectedSaving);
        if (operation.equals(SavingOperation.ARCHIVE)) archiveSaving(selectedSaving);
        if (operation.equals(SavingOperation.HISTORY)) showHistoryDialog(selectedSaving);
    }
}