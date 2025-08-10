package com.nvllz.piggsy.ui.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.util.DateUtil;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Transaction> transactions;
    private final String currency;

    public TransactionAdapter(Context context, ArrayList<Transaction> transactions, String currency) {
        this.context = context;
        this.transactions = transactions;
        this.currency = currency;
    }

    @NonNull
    @Override
    public TransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View transactionView = LayoutInflater.from(context).inflate(R.layout.recycler_transaction, parent, false);
        return new ViewHolder(transactionView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionAdapter.ViewHolder holder, int position) {
        Transaction currentTransaction = transactions.get(position);
        holder.bind(currentTransaction, currency, context);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView amount, date, note;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.transaction_amount);
            date = itemView.findViewById(R.id.transaction_date);
            note = itemView.findViewById(R.id.transaction_note);
        }

        public void bind(Transaction currentTransaction, String selectedCurrencySymbol, Context context) {
            date.setText(DateUtil.getStringDateTime(currentTransaction.getDate(), context));

            String transactionNote = currentTransaction.getNote();
            if (!TextUtils.isEmpty(transactionNote) && !transactionNote.trim().isEmpty()) {
                note.setText(transactionNote);
                note.setVisibility(View.VISIBLE);
            } else {
                note.setVisibility(View.GONE);
            }

            if (currentTransaction.getType().equals(TransactionType.DEPOSIT.VALUE)) {
                amount.setTextColor(context.getResources().getColor(R.color.md_theme_secondary, itemView.getContext().getTheme()));
                amount.setText(String.format("+%s", Currency.formatAmount(selectedCurrencySymbol, currentTransaction.getAmount())));
            } else if (currentTransaction.getType().equals(TransactionType.CREATED.VALUE)) {
                amount.setTextColor(context.getResources().getColor(R.color.md_theme_secondary, itemView.getContext().getTheme()));
                amount.setText(Currency.formatAmount(selectedCurrencySymbol, currentTransaction.getAmount()));
            } else if (currentTransaction.getType().equals(TransactionType.WITHDRAW.VALUE)) {
                amount.setTextColor(context.getResources().getColor(R.color.md_theme_error, itemView.getContext().getTheme()));
                amount.setText(String.format("-%s", Currency.formatAmount(selectedCurrencySymbol, currentTransaction.getAmount())));
            }
        }

    }
}