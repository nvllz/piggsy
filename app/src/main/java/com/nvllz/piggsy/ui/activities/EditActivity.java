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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.ui.ComposeSavingFormView;
import com.nvllz.piggsy.util.AlarmUtil;
import com.google.android.material.snackbar.Snackbar;

public class EditActivity extends BaseActivity implements ComposeSavingFormView.Listener {

    private ComposeSavingFormView formView;
    private SavingRepository savingRepository;
    private AlarmManager alarmManager;
    private Saving currentSaving;

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

        String savingIDExtra = getIntent().getStringExtra(Database.COLUMN_SAVING_ID);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        savingRepository = new SavingRepository(this);
        currentSaving = savingRepository.getSaving(savingIDExtra);

        formView = new ComposeSavingFormView(this, true, currentSaving, this);
        setContentView(formView);
    }

    @Override
    public void onSave(String name, String currencyCode, String current, String goal, String description, boolean archiveWhenCompleted) {
        Saving editedSaving = new Saving();
        editedSaving.setID(currentSaving.getID());
        editedSaving.setName(name);
        editedSaving.setCurrentSaving(currentSaving.getCurrentSaving());
        editedSaving.setGoal(goal.isEmpty() ? 0.0 : Double.parseDouble(goal));
        editedSaving.setDescription(description);
        editedSaving.setIsArchived(currentSaving.getIsArchived());
        editedSaving.setDeadline(formView.getDeadline());
        editedSaving.setCurrency(currencyCode);
        editedSaving.setArchiveWhenCompleted(archiveWhenCompleted);

        if (formView.getDeadline() == AlarmUtil.NO_ALARM) {
            AlarmUtil.cancel(this, editedSaving);
            editedSaving.setDeadline(AlarmUtil.NO_ALARM);
        } else {
            AlarmUtil.set(this, editedSaving);
        }

        savingRepository.edit(editedSaving);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onDeadlineFieldClick() {
        hasNotificationPermission();
    }

    @Override
    public void onBack() {
        finish();
    }

    @Override
    public void onDelete() {
        formView.requestDeleteDialog();
    }

    @Override
    public void onDeleteConfirmed() {
        AlarmUtil.cancel(this, currentSaving);
        Intent result = new Intent();
        result.putExtra("action", "delete");
        result.putExtra(Database.COLUMN_SAVING_ID, currentSaving.getID());
        setResult(RESULT_OK, result);
        finish();
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
            if (alarmManager.canScheduleExactAlarms()) {
                formView.requestDeadlinePicker();
            } else {
                formView.requestAlarmPermissionDialog();
            }
        } else {
            formView.requestDeadlinePicker();
        }
    }

    private void hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                hasAlarmPermission();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.snack_bar_permission_notifications), Snackbar.LENGTH_SHORT)
                        .setAction(R.string.grant, v -> {
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
}

