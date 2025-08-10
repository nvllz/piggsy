package com.eipna.centsation.ui.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eipna.centsation.R;
import com.eipna.centsation.data.Currency;
import com.eipna.centsation.data.Database;
import com.eipna.centsation.data.saving.Saving;
import com.eipna.centsation.data.saving.SavingRepository;
import com.eipna.centsation.databinding.ActivityEditBinding;
import com.eipna.centsation.util.AlarmUtil;
import com.eipna.centsation.util.DateUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.Objects;

public class EditActivity extends BaseActivity {

    private ActivityEditBinding binding;
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
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            ViewGroup.MarginLayoutParams scrollParams =
                    (ViewGroup.MarginLayoutParams) binding.scrollView.getLayoutParams();
            scrollParams.bottomMargin = imeInsets.bottom;
            binding.scrollView.setLayoutParams(scrollParams);

            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String savingIDExtra = getIntent().getStringExtra(Database.COLUMN_SAVING_ID);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        savingRepository = new SavingRepository(this);
        currentSaving = savingRepository.getSaving(savingIDExtra);

        String selectedCurrencySymbol = Currency.getSymbol(preferences.getCurrency());
        binding.fieldSavingGoalLayout.setPrefixText(selectedCurrencySymbol);

        binding.fieldSavingNameText.setText(currentSaving.getName());
        binding.fieldSavingGoalText.setText(String.format(Locale.getDefault(), "%.2f", currentSaving.getGoal()));
        binding.fieldSavingDescriptionText.setText(currentSaving.getDescription());
        binding.fieldSavingDeadlineLayout.setEndIconVisible(false);

        if (currentSaving.getDeadline() != AlarmUtil.NO_ALARM) {
            String deadlineFormat = preferences.getDateFormat();
            binding.fieldSavingDeadlineLayout.setEndIconVisible(true);
            binding.fieldSavingDeadlineText.setText(DateUtil.getStringDate(currentSaving.getDeadline(), deadlineFormat));
        }

        binding.fieldSavingDeadlineText.setOnClickListener(v -> hasNotificationPermission());
        binding.fieldSavingDeadlineLayout.setEndIconOnClickListener(v -> {
            binding.fieldSavingDeadlineText.setText("");
            binding.fieldSavingDeadlineLayout.setEndIconVisible(false);
        });

        setupAutoScroll();
    }

    private void setupAutoScroll() {
        View.OnFocusChangeListener scrollToFocused = (v, hasFocus) -> {
            if (hasFocus) {
                v.postDelayed(() -> {
                    View parentLayout = (View) v.getParent().getParent();

                    int[] location = new int[2];
                    parentLayout.getLocationInWindow(location);

                    int toolbarHeight = binding.toolbar.getHeight();

                    int scrollOffset = v.getId() == R.id.field_saving_description_text ? 50 : 100;
                    int targetY = Math.max(0, location[1] - toolbarHeight - scrollOffset);

                    if (targetY <= 0 && v.getId() == R.id.field_saving_name_text) {
                        binding.scrollView.smoothScrollTo(0, 0);
                    } else {
                        binding.scrollView.smoothScrollTo(0, targetY);
                    }
                }, 200);
            }
        };

        binding.fieldSavingNameText.setOnFocusChangeListener(scrollToFocused);
        binding.fieldSavingGoalText.setOnFocusChangeListener(scrollToFocused);
        binding.fieldSavingDescriptionText.setOnFocusChangeListener(scrollToFocused);
    }

    private void hasAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                showDeadlineDialog();
            } else {
                showAlarmPermissionDialog();
            }
        } else {
            showDeadlineDialog();
        }
    }

    private void showAlarmPermissionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_request_alarm_permission)
                .setMessage(R.string.dialog_message_request_alarm_permission)
                .setIcon(R.drawable.ic_alarm)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_grant, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                hasAlarmPermission();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                Snackbar.make(binding.getRoot(), getString(R.string.snack_bar_permission_notifications), Snackbar.LENGTH_SHORT)
                        .setAction(R.string.dialog_button_grant, v -> {
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

    private void showDeadlineDialog() {
        CalendarConstraints.Builder calendarConstraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(calendarConstraints.build())
                .setTitleText("Select deadline date")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            calendar.set(java.util.Calendar.MINUTE, 0);
            calendar.set(java.util.Calendar.SECOND, 0);
            calendar.set(java.util.Calendar.MILLISECOND, 0);

            currentSaving.setDeadline(calendar.getTimeInMillis());
            String deadlineFormat = preferences.getDateFormat();

            binding.fieldSavingDeadlineText.setText(DateUtil.getStringDate(selection, deadlineFormat));
            binding.fieldSavingDeadlineLayout.setEndIconVisible(true);
        });
        datePicker.show(getSupportFragmentManager(), null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        if (item.getItemId() == R.id.save) editSaving();
        return true;
    }

    private void editSaving() {
        String nameText = Objects.requireNonNull(binding.fieldSavingNameText.getText()).toString();
        String goalText = Objects.requireNonNull(binding.fieldSavingGoalText.getText()).toString();
        String descriptionText = Objects.requireNonNull(binding.fieldSavingDescriptionText.getText()).toString();
        String deadlineText = Objects.requireNonNull(binding.fieldSavingDeadlineText.getText()).toString();

        if (!nameText.isEmpty() && !goalText.isEmpty()) {
            double goal = Double.parseDouble(goalText);

            Saving editedSaving = new Saving();
            editedSaving.setID(currentSaving.getID());
            editedSaving.setName(nameText);
            editedSaving.setCurrentSaving(currentSaving.getCurrentSaving());
            editedSaving.setGoal(goal);
            editedSaving.setDescription(descriptionText);
            editedSaving.setIsArchived(currentSaving.getIsArchived());
            editedSaving.setDeadline(currentSaving.getDeadline());

            if (deadlineText.isEmpty()) {
                AlarmUtil.cancel(this, editedSaving);
                editedSaving.setDeadline(AlarmUtil.NO_ALARM);
            } else {
                AlarmUtil.set(this, editedSaving);
            }

            savingRepository.edit(editedSaving);
            setResult(RESULT_OK);
            finish();
        }

        binding.fieldSavingNameLayout.setError(nameText.isEmpty() ? getString(R.string.field_error_required) : null);
        binding.fieldSavingGoalLayout.setError(goalText.isEmpty() ? getString(R.string.field_error_required) : null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saving, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}