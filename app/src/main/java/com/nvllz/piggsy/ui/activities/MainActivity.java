package com.nvllz.piggsy.ui.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingOperation;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.saving.SavingSort;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.ui.ComposeMainView;
import com.nvllz.piggsy.ui.MainDestination;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.BackupScheduler;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends BaseActivity implements ComposeMainView.Listener {

    private ComposeMainView mainView;
    private SavingRepository savingRepository;
    private TransactionRepository transactionRepository;
    private ArrayList<Saving> savings;

    private String sortCriteria;
    private boolean isSortAscending;
    private boolean skipNextRefresh = false;


    private Saving pendingDeadlineSaving;

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
                            pushList();

                            mainView.showUndoToast(
                                    getString(R.string.snackbar_piggy_bank_deleted),
                                    getString(R.string.undo),
                                    () -> {
                                        savings.add(deletedSaving);
                                        sortSavings(sortCriteria);
                                        skipNextRefresh = false;
                                    },
                                    () -> {
                                        savingRepository.delete(deletedSaving.getID());
                                        skipNextRefresh = false;
                                    });
                        }
                    } else {
                        refreshList();
                    }
                }
            });

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    hasAlarmPermission();
                } else {
                    Toast.makeText(this, R.string.toast_notification_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        savings = new ArrayList<>();
        savingRepository = new SavingRepository(this);
        transactionRepository = new TransactionRepository(this);

        sortCriteria = preferences.getSortCriteria();
        isSortAscending = preferences.getSortOrder();

        savings.addAll(savingRepository.getSavings(Saving.NOT_ARCHIVE));

        mainView = new ComposeMainView(this, this);
        setContentView(mainView);

        sortSavings(sortCriteria);

        openSavingFromNotification(getIntent());
    }


    private void openSavingFromNotification(Intent intent) {
        if (intent == null) return;
        String savingID = intent.getStringExtra(Database.COLUMN_SAVING_ID);
        if (savingID == null) return;

        Saving saving = savingRepository.getSaving(savingID);
        if (saving != null) {
            mainView.showSavingDetails(saving, transactionRepository.get(savingID));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!skipNextRefresh) {
            refreshList();
        }
        BackupScheduler.checkAndRunIfDue(this, new PreferenceUtil(this));
    }

    private void refreshList() {
        savings.clear();
        savings.addAll(savingRepository.getSavings(Saving.NOT_ARCHIVE));
        sortSavings(sortCriteria);
    }

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
        } else if (criteria.equals(SavingSort.CREATION.SORT)) {


            HashMap<String, Long> creationDates = transactionRepository.getCreationDates();
            savingComparator = Comparator.comparingLong(
                    saving -> creationDates.getOrDefault(saving.getID(), 0L));
        }

        if (savingComparator != null) {
            if (!isSortAscending) {
                savingComparator = savingComparator.reversed();
            }
        }

        savings.sort(savingComparator);
        pushList();

        mainView.updateSort(sortCriteria, isSortAscending);
        preferences.setSortCriteria(sortCriteria);
        preferences.setSortOrder(isSortAscending);
    }

    private void pushList() {
        mainView.updateSavings(new ArrayList<>(savings));
    }

    private void archiveSaving(Saving saving) {
        AlarmUtil.cancel(this, saving);

        saving.setIsArchived(Saving.IS_ARCHIVE);
        saving.setArchivedAt(System.currentTimeMillis());
        savingRepository.edit(saving);

        savings.remove(saving);
        pushList();

        mainView.showUndoToast(
                getString(R.string.snackbar_piggy_bank_archived),
                getString(R.string.undo),
                () -> {
                    saving.setIsArchived(Saving.NOT_ARCHIVE);
                    saving.setArchivedAt(0);
                    savingRepository.edit(saving);


                    AlarmUtil.set(this, saving);
                    savings.add(saving);
                    sortSavings(sortCriteria);
                },
                null);
    }

    private void showTransactionDialog(Saving selectedSaving) {
        mainView.showTransactionDialog(selectedSaving);
    }

    @Override
    public void onItemClick(Saving saving) {
        Intent editIntent = new Intent(MainActivity.this, EditActivity.class);
        editIntent.putExtra(Database.COLUMN_SAVING_ID, saving.getID());
        editSavingLauncher.launch(editIntent);
    }

    @Override
    public void onSavingDetails(Saving saving) {
        ArrayList<Transaction> transactions = transactionRepository.get(saving.getID());
        mainView.showSavingDetails(saving, transactions);
    }

    @Override
    public void onSavingDeadlineEdit(Saving saving) {
        pendingDeadlineSaving = saving;
        hasNotificationPermission();
    }

    @Override
    public void onSavingDeadlineChanged(Saving saving, long deadlineMillis) {
        saving.setDeadline(deadlineMillis);
        savingRepository.edit(saving);

        AlarmUtil.set(this, saving);
        refreshList();
    }

    @Override
    public void onOpenHistory(Saving saving) {
        showHistoryActivity(saving);
    }

    @Override
    public void onGrantAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }


    private void hasAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
                mainView.requestDeadlinePicker(pendingDeadlineSaving);
            } else {
                mainView.requestAlarmPermissionDialog();
            }
        } else {
            mainView.requestDeadlinePicker(pendingDeadlineSaving);
        }
    }

    private void hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                hasAlarmPermission();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.snack_bar_permission_notifications), Snackbar.LENGTH_SHORT)
                        .setAction(R.string.grant, (View v) -> {
                            Intent notificationSettingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            notificationSettingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(notificationSettingsIntent);
                        }).show();
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            hasAlarmPermission();
        }
    }

    @Override
    public void onOperation(SavingOperation operation, Saving saving) {
        if (operation.equals(SavingOperation.DELETE)) mainView.showDeleteDialog(saving);
        if (operation.equals(SavingOperation.TRANSACTION)) showTransactionDialog(saving);
        if (operation.equals(SavingOperation.ARCHIVE)) archiveSaving(saving);
        if (operation.equals(SavingOperation.HISTORY)) showHistoryActivity(saving);
    }

    @Override
    public void onDeleteConfirmed(Saving saving) {
        AlarmUtil.cancel(this, saving);

        savings.remove(saving);
        pushList();

        mainView.showUndoToast(
                getString(R.string.snackbar_piggy_bank_deleted),
                getString(R.string.undo),
                () -> {
                    savings.add(saving);
                    sortSavings(sortCriteria);
                },
                () -> savingRepository.delete(saving.getID()));
    }

    @Override
    public void onTransactionConfirmed(Saving saving, double amount, String type, String note) {
        TransactionType typeEnum = TransactionType.WITHDRAW.VALUE.equals(type)
                ? TransactionType.WITHDRAW : TransactionType.DEPOSIT;

        double newAmount = TransactionType.WITHDRAW.VALUE.equals(type)
                ? saving.getCurrentSaving() - amount
                : saving.getCurrentSaving() + amount;
        saving.setCurrentSaving(newAmount);
        savingRepository.makeTransaction(saving, amount, typeEnum, note);

        if (saving.getArchiveWhenCompleted()
                && typeEnum.equals(TransactionType.DEPOSIT)
                && saving.getGoal() > 0
                && saving.getCurrentSaving() >= saving.getGoal()) {
            archiveSaving(saving);
            return;
        }

        refreshList();
    }

    @Override
    public void onSortCriteria(String criteria) {
        sortCriteria = criteria;
        sortSavings(sortCriteria);
    }

    @Override
    public void onSortOrder(boolean ascending) {
        isSortAscending = ascending;
        sortSavings(sortCriteria);
    }

    @Override
    public void onDestination(MainDestination destination) {
        switch (destination) {
            case BACKUP:
                startActivity(new Intent(this, BackupActivity.class));
                break;
            case ARCHIVE:
                startActivity(new Intent(this, ArchiveActivity.class));
                break;
            case SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case ABOUT:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
    }

    @Override
    public void onCreateSaving() {
        createSavingLauncher.launch(new Intent(MainActivity.this, CreateActivity.class));
    }

    private void showHistoryActivity(Saving saving) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra(Database.COLUMN_SAVING_ID, saving.getID());
        startActivity(historyIntent);
    }
}
