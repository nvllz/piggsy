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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.databinding.ActivityCreateBinding;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.DateUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

public class CreateActivity extends BaseActivity {

    private ActivityCreateBinding binding;
    private SavingRepository savingRepository;
    private AlarmManager alarmManager;
    private long selectedDeadline;
    private String selectedCurrency;

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
        binding = ActivityCreateBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        binding.fieldSavingNameText.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.fieldSavingNameText, InputMethodManager.SHOW_IMPLICIT);

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

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        savingRepository = new SavingRepository(this);
        selectedDeadline = AlarmUtil.NO_ALARM;

        setupCurrencyDropdown();
        setupDeadlineField();
        setupAutoScroll();
    }

    private void setupCurrencyDropdown() {
        String[] currencyNames = Currency.getNames();
        String[] currencyCodes = Currency.getCodes();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, currencyNames);

        AutoCompleteTextView currencyDropdown = binding.fieldSavingCurrencyText;
        currencyDropdown.setAdapter(adapter);

        String defaultCurrency = preferences.getCurrency();
        String defaultCurrencyName = Currency.getName(defaultCurrency);
        currencyDropdown.setText(defaultCurrencyName, false);
        selectedCurrency = defaultCurrency;

        currencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedCurrency = currencyCodes[position];
            updateCurrencySymbols();
        });

        updateCurrencySymbols();
    }

    private void updateCurrencySymbols() {
        String currencySymbol = Currency.getSymbol(selectedCurrency);
        binding.fieldSavingCurrentSavingLayout.setPrefixText(currencySymbol);
        binding.fieldSavingGoalLayout.setPrefixText(currencySymbol);
    }

    private void setupDeadlineField() {
        binding.fieldSavingDeadlineLayout.setEndIconVisible(false);
        binding.fieldSavingDeadlineText.setOnClickListener(v -> hasNotificationPermission());
        binding.fieldSavingDeadlineLayout.setEndIconOnClickListener(v -> {
            binding.fieldSavingDeadlineText.setText("");
            binding.fieldSavingDeadlineLayout.setEndIconVisible(false);
        });
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
        binding.fieldSavingCurrentSavingText.setOnFocusChangeListener(scrollToFocused);
        binding.fieldSavingGoalText.setOnFocusChangeListener(scrollToFocused);
        binding.fieldSavingDescriptionText.setOnFocusChangeListener(scrollToFocused);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        if (item.getItemId() == R.id.save) createSaving();
        return true;
    }

    private void createSaving() {
        String nameText = Objects.requireNonNull(binding.fieldSavingNameText.getText()).toString();
        String currentSavingText = Objects.requireNonNull(binding.fieldSavingCurrentSavingText.getText()).toString();
        String goalText = Objects.requireNonNull(binding.fieldSavingGoalText.getText()).toString();
        String notesText = Objects.requireNonNull(binding.fieldSavingDescriptionText.getText()).toString();
        String deadlineText = Objects.requireNonNull(binding.fieldSavingDeadlineText.getText()).toString();

        if (!nameText.isEmpty() && selectedCurrency != null) {
            double currentSaving = currentSavingText.isEmpty() ? 0.0 : Double.parseDouble(currentSavingText);
            double goal = goalText.isEmpty() ? 0.0 : Double.parseDouble(goalText);

            Saving createdSaving = new Saving();
            createdSaving.setID(UUID.randomUUID().toString());
            createdSaving.setName(nameText);
            createdSaving.setCurrentSaving(currentSaving);
            createdSaving.setGoal(goal);
            createdSaving.setDescription(notesText);
            createdSaving.setIsArchived(Saving.NOT_ARCHIVE);
            createdSaving.setDeadline(selectedDeadline);
            createdSaving.setCurrency(selectedCurrency);

            if (!deadlineText.isEmpty()) {
                AlarmUtil.set(this, createdSaving);
            }

            savingRepository.create(createdSaving);
            setResult(RESULT_OK);
            finish();
        }

        binding.fieldSavingNameLayout.setError(nameText.isEmpty() ? getString(R.string.field_error_required) : null);
        binding.fieldSavingCurrencyLayout.setError(selectedCurrency == null ? getString(R.string.field_error_required) : null);
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
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(intent);
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
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            selectedDeadline = calendar.getTimeInMillis();
            String deadlineFormat = preferences.getDateFormat();

            binding.fieldSavingDeadlineText.setText(DateUtil.getStringDate(selection, deadlineFormat));
            binding.fieldSavingDeadlineLayout.setEndIconVisible(true);
        });
        datePicker.show(getSupportFragmentManager(), null);
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