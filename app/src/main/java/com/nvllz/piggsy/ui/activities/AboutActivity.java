package com.nvllz.piggsy.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.databinding.ActivityAboutBinding;
import com.nvllz.piggsy.databinding.ItemAboutRowBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutActivity extends BaseActivity {

    private ActivityAboutBinding binding;
    private int easterEggCounter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        Drawable appBarDrawable = MaterialShapeDrawable.createWithElevationOverlay(this);
        binding.appBar.setStatusBarForeground(appBarDrawable);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupVersionChip();
        setupRows();
    }

    private void setupVersionChip() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            binding.appVersionChip.setText(getString(R.string.preference_app_version) + " " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        binding.appVersionChip.setOnClickListener(v -> {
            easterEggCounter++;
            if (easterEggCounter == 7) {
                Toast.makeText(this, R.string.app_easter_egg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRows() {
        bindRow(binding.rowLicense, R.drawable.ic_round_description,
                getString(R.string.preference_app_license), getString(R.string.app_license),
                this::showLicenseDialog);

        bindRow(binding.rowSourceCode, R.drawable.ic_round_code,
                getString(R.string.preference_app_source_code_title),
                getString(R.string.preference_app_source_code_summary),
                () -> openUrl(getString(R.string.link_app_source_code)));

        bindRow(binding.rowAuthor, R.drawable.ic_round_person,
                getString(R.string.preference_app_author), getString(R.string.app_author),
                () -> openUrl(getString(R.string.link_app_author)));

        bindRow(binding.rowOgCreator, R.drawable.ic_round_person,
                getString(R.string.preference_app_og_creator), getString(R.string.app_og_creator),
                () -> openUrl(getString(R.string.link_app_og_creator)));
    }

    private void bindRow(ItemAboutRowBinding row, int iconRes, String title, String subtitle, Runnable onClick) {
        row.rowIcon.setImageResource(iconRes);
        row.rowTitle.setText(title);
        row.rowSubtitle.setText(subtitle);
        row.getRoot().setOnClickListener(v -> onClick.run());
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showLicenseDialog() {
        Dialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.preference_app_license)
                .setMessage(readLicenseFromAssets())
                .setPositiveButton(R.string.dialog_button_close, null)
                .create();
        dialog.show();
    }

    private String readLicenseFromAssets() {
        StringBuilder stringBuilder = new StringBuilder();
        AssetManager assetManager = getAssets();
        try (InputStream inputStream = assetManager.open("license.txt")) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return stringBuilder.toString();
    }
}