package com.eipna.centsation.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eipna.centsation.data.Database;
import com.eipna.centsation.data.saving.Saving;
import com.eipna.centsation.data.saving.SavingRepository;
import com.eipna.centsation.data.transaction.Transaction;
import com.eipna.centsation.data.transaction.TransactionRepository;
import com.eipna.centsation.databinding.ActivityHistoryBinding;
import com.eipna.centsation.ui.adapters.TransactionAdapter;

import java.util.ArrayList;
import java.util.Collections;

public class HistoryActivity extends BaseActivity {

    private ActivityHistoryBinding binding;

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

        String selectedSavingID = getIntent().getStringExtra(Database.COLUMN_SAVING_ID);
        String currency;
        String piggyBankName;

        ArrayList<Transaction> transactions;
        try (TransactionRepository transactionRepository = new TransactionRepository(this)) {
            transactions = transactionRepository.get(selectedSavingID);
            Collections.reverse(transactions);
        }

        try (SavingRepository savingRepository = new SavingRepository(this)) {
            Saving saving = savingRepository.getSaving(selectedSavingID);
            currency = saving.getCurrency();
            piggyBankName = saving.getName();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(piggyBankName);
        }

        TransactionAdapter transactionAdapter = new TransactionAdapter(this, transactions, currency);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(transactionAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return true;
    }
}