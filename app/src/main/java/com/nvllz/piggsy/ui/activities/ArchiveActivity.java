package com.nvllz.piggsy.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.nvllz.piggsy.R;import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingOperation;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.ui.ComposeArchiveView;
import com.nvllz.piggsy.util.AlarmUtil;

import java.util.ArrayList;

public class ArchiveActivity extends BaseActivity implements ComposeArchiveView.Listener {

    private ComposeArchiveView archiveView;
    private SavingRepository savingRepository;
    private ArrayList<Saving> savings;

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

        savingRepository = new SavingRepository(this);
        savings = new ArrayList<>(savingRepository.getSavings(Saving.IS_ARCHIVE));
        sortSavings();

        archiveView = new ComposeArchiveView(this, this);
        archiveView.updateSavings(savings);
        setContentView(archiveView);
    }

    @Override
    public void onBack() {
        finish();
    }

    @Override
    public void onEdit(Saving saving) {
        Intent editIntent = new Intent(this, EditActivity.class);
        editIntent.putExtra(Database.COLUMN_SAVING_ID, saving.getID());
        editSavingLauncher.launch(editIntent);
    }

    @Override
    public void onSavingDetails(Saving saving) {
        TransactionRepository transactionRepository = new TransactionRepository(this);
        ArrayList<Transaction> transactions = transactionRepository.get(saving.getID());
        archiveView.showSavingDetails(saving, transactions);
    }

    @Override
    public void onOperation(SavingOperation operation, Saving saving) {
        if (operation.equals(SavingOperation.UNARCHIVE)) unarchiveSaving(saving);
        if (operation.equals(SavingOperation.DELETE)) archiveView.showDeleteDialog(saving);
        if (operation.equals(SavingOperation.HISTORY)) showHistoryActivity(saving);
    }

    @Override
    public void onDeleteConfirmed(Saving saving) {
        savings.remove(saving);
        archiveView.updateSavings(savings);

        archiveView.showUndoToast(
                getString(R.string.snackbar_piggy_bank_deleted),
                getString(R.string.undo),
                () -> {
                    savings.add(saving);
                    sortSavings();
                    archiveView.updateSavings(savings);
                },
                () -> savingRepository.delete(saving.getID()));
    }

    private void unarchiveSaving(Saving selectedSaving) {
        selectedSaving.setIsArchived(Saving.NOT_ARCHIVE);
        selectedSaving.setArchivedAt(0);
        savingRepository.edit(selectedSaving);

        AlarmUtil.set(this, selectedSaving);
        archiveView.showToast(getString(R.string.snackbar_piggy_bank_unarchived));
        refreshList();
    }

    private void refreshList() {
        savings.clear();
        savings.addAll(savingRepository.getSavings(Saving.IS_ARCHIVE));
        sortSavings();
        archiveView.updateSavings(savings);
    }

    private void sortSavings() {
        savings.sort((a, b) -> {
            long aArchived = a.getArchivedAt();
            long bArchived = b.getArchivedAt();
            if ((aArchived == 0) != (bArchived == 0)) {
                return aArchived == 0 ? -1 : 1;
            }
            return Long.compare(aArchived, bArchived);
        });
    }

    private void showHistoryActivity(Saving saving) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra(Database.COLUMN_SAVING_ID, saving.getID());
        startActivity(historyIntent);
    }
}
