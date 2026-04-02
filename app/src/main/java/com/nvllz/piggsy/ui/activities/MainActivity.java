package com.nvllz.piggsy.ui.activities;

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

import com.google.android.material.snackbar.Snackbar;
import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingOperation;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.saving.SavingSort;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.databinding.ActivityMainBinding;
import com.nvllz.piggsy.ui.adapters.SavingAdapter;
import com.nvllz.piggsy.util.AlarmUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nvllz.piggsy.util.BackupScheduler;
import com.nvllz.piggsy.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class MainActivity extends BaseActivity implements SavingAdapter.Listener {

    private ActivityMainBinding binding;
    private SavingRepository savingRepository;
    private SavingAdapter savingAdapter;
    private ArrayList<Saving> savings;

    private String sortCriteria;
    private boolean isSortAscending;
    private boolean skipNextRefresh = false;

    private final ActivityResultLauncher<Intent> createSavingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshList();
                }
            });

    private final ActivityResultLauncher<Intent> editSavingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && "delete".equals(data.getStringExtra("action"))) {
                        String deletedId = data.getStringExtra(Database.COLUMN_SAVING_ID);
                        Saving deletedSaving = savings.stream()
                                .filter(s -> s.getID().equals(deletedId))
                                .findFirst().orElse(null);

                        if (deletedSaving != null) {
                            skipNextRefresh = true;
                            savings.remove(deletedSaving);
                            savingAdapter.notifyDataSetChanged();
                            binding.emptyIndicator.setVisibility(savings.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.savingList.setVisibility(savings.isEmpty() ? View.GONE : View.VISIBLE);

                            Snackbar.make(binding.getRoot(), getString(R.string.snackbar_piggy_bank_deleted), Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.undo), v -> {
                                        savings.add(deletedSaving);
                                        sortSavings(sortCriteria);
                                        binding.emptyIndicator.setVisibility(View.GONE);
                                        binding.savingList.setVisibility(View.VISIBLE);
                                    })
                                    .addCallback(new Snackbar.Callback() {
                                        @Override
                                        public void onDismissed(Snackbar snackbar, int event) {
                                            if (event != DISMISS_EVENT_ACTION) {
                                                savingRepository.delete(deletedSaving.getID());
                                            }
                                            skipNextRefresh = false;
                                        }
                                    })
                                    .show();
                        }
                    } else {
                        refreshList();
                    }
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
    protected void onResume() {
        super.onResume();
        if (!skipNextRefresh) {
            refreshList();
        }
        PreferenceUtil prefs = new PreferenceUtil(this);
        BackupScheduler.checkAndRunIfDue(this, prefs);
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
        if (item.getItemId() == R.id.backup) startActivity(new Intent(this, BackupActivity.class));
        if (item.getItemId() == R.id.archive) startActivity(new Intent(this, ArchiveActivity.class));
        if (item.getItemId() == R.id.settings) startActivity(new Intent(this, SettingsActivity.class));
        if (item.getItemId() == R.id.about) startActivity(new Intent(this, AboutActivity.class));

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

    private void archiveSaving(Saving saving) {
        AlarmUtil.cancel(this, saving);

        saving.setIsArchived(Saving.IS_ARCHIVE);
        savingRepository.edit(saving);

        savings.remove(saving);
        savingAdapter.notifyDataSetChanged();
        binding.emptyIndicator.setVisibility(savings.isEmpty() ? View.VISIBLE : View.GONE);
        binding.savingList.setVisibility(savings.isEmpty() ? View.GONE : View.VISIBLE);

        Snackbar.make(binding.getRoot(), getString(R.string.snackbar_piggy_bank_archived), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo), v -> {
                    saving.setIsArchived(Saving.NOT_ARCHIVE);
                    savingRepository.edit(saving);
                    savings.add(saving);
                    sortSavings(sortCriteria);
                    binding.emptyIndicator.setVisibility(View.GONE);
                    binding.savingList.setVisibility(View.VISIBLE);
                })
                .show();
    }

    private void showDeleteDialog(Saving saving) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_delete_saving)
                .setMessage(R.string.dialog_message_delete_saving)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_delete, (dialogInterface, i) -> {
                    AlarmUtil.cancel(this, saving);

                    savings.remove(saving);
                    savingAdapter.notifyDataSetChanged();
                    binding.emptyIndicator.setVisibility(savings.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.savingList.setVisibility(savings.isEmpty() ? View.GONE : View.VISIBLE);

                    Snackbar.make(binding.getRoot(), getString(R.string.snackbar_piggy_bank_deleted), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.undo), v -> {
                                savings.add(saving);
                                sortSavings(sortCriteria);
                                binding.emptyIndicator.setVisibility(View.GONE);
                                binding.savingList.setVisibility(View.VISIBLE);
                            })
                            .addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    if (event != DISMISS_EVENT_ACTION) {
                                        savingRepository.delete(saving.getID());
                                    }
                                }
                            })
                            .show();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTransactionDialog(Saving selectedSaving) {
        View transactionDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saving_transaction, null, false);
        String currentCurrencySymbol = Currency.getSymbol(selectedSaving.getCurrency());

        TextInputLayout amountLayout = transactionDialogView.findViewById(R.id.field_saving_amount_layout);
        TextInputEditText amountInput = transactionDialogView.findViewById(R.id.field_saving_amount_text);

        TextInputEditText noteInput = transactionDialogView.findViewById(R.id.field_transaction_note_text);

        MaterialButton depositOption = transactionDialogView.findViewById(R.id.button_deposit);
        MaterialButton withdrawOption = transactionDialogView.findViewById(R.id.button_withdraw);

        depositOption.setChecked(true);

        amountLayout.setPrefixText(currentCurrencySymbol + "  ");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_create_transaction)
                .setView(transactionDialogView)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_submit, null);

        AlertDialog dialog = builder.create();

        amountInput.requestFocus();
        amountInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(amountInput, InputMethodManager.SHOW_IMPLICIT);
        }, 300);

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String amountText = Objects.requireNonNull(amountInput.getText()).toString();
                    String noteText = Objects.requireNonNull(noteInput.getText()).toString().trim();

                    if (amountText.isEmpty()) {
                        amountLayout.setError(getString(R.string.field_error_required));
                        return;
                    }

                    try {
                        double amount = Double.parseDouble(amountText);

                        if (amount <= 0) {
                            amountInput.setError(getString(R.string.error_amount_positive));
                            return;
                        }

                        if (depositOption.isChecked()) {
                            double addedSaving = selectedSaving.getCurrentSaving() + amount;
                            selectedSaving.setCurrentSaving(addedSaving);
                            savingRepository.makeTransaction(selectedSaving, amount, TransactionType.DEPOSIT, noteText);

                            refreshList();
                            dialog.dismiss();
                        } else if (withdrawOption.isChecked()) {
                            double deductedSaving = selectedSaving.getCurrentSaving() - amount;
                            selectedSaving.setCurrentSaving(deductedSaving);
                            savingRepository.makeTransaction(selectedSaving, amount, TransactionType.WITHDRAW, noteText);

                            refreshList();
                            dialog.dismiss();
                        }
                    } catch (NumberFormatException e) {
                        amountLayout.setError("Invalid amount");
                    }
                }));
        dialog.show();
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
        if (operation.equals(SavingOperation.TRANSACTION)) showTransactionDialog(selectedSaving);
        if (operation.equals(SavingOperation.ARCHIVE)) archiveSaving(selectedSaving);
        if (operation.equals(SavingOperation.HISTORY)) showHistoryActivity(selectedSaving);
    }

    private void showHistoryActivity(Saving saving) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra(Database.COLUMN_SAVING_ID, saving.getID());
        startActivity(historyIntent);
    }
}