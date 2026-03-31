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
    private final ArrayList<Double> subtotals = new ArrayList<>();

    public TransactionAdapter(Context context, ArrayList<Transaction> transactions, String currency) {
        this.context = context;
        this.transactions = transactions;
        this.currency = currency;
        recomputeSubtotals();
    }

    private void recomputeSubtotals() {
        subtotals.clear();

        // Find initial balance from the CREATED transaction (if any)
        double balance = 0;
        for (Transaction t : transactions) {
            if (TransactionType.CREATED.VALUE.equals(t.getType())) {
                balance = t.getAmount();
                break;
            }
        }

        // Traverse from end to start to compute running balances
        double[] computed = new double[transactions.size()];
        boolean[] hasSubtotal = new boolean[transactions.size()];

        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction t = transactions.get(i);

            if (TransactionType.CREATED.VALUE.equals(t.getType())) {
                balance = t.getAmount();
                hasSubtotal[i] = false;
            } else if (TransactionType.DEPOSIT.VALUE.equals(t.getType())) {
                balance += t.getAmount();
                computed[i] = balance;
                hasSubtotal[i] = true;
            } else if (TransactionType.WITHDRAW.VALUE.equals(t.getType())) {
                balance -= t.getAmount();
                computed[i] = balance;
                hasSubtotal[i] = true;
            } else {
                hasSubtotal[i] = false;
            }
        }

        for (int i = 0; i < transactions.size(); i++) {
            subtotals.add(hasSubtotal[i] ? computed[i] : null);
        }
    }

    public void refreshSubtotals() {
        recomputeSubtotals();
        notifyDataSetChanged();
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
        Double subtotal = position < subtotals.size() ? subtotals.get(position) : null;
        holder.bind(currentTransaction, currency, context, subtotal);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < transactions.size()) {
            transactions.remove(position);
            notifyItemRemoved(position);
            recomputeSubtotals();
            notifyItemRangeChanged(position, transactions.size() - position);
        }
    }

    public void updateItem(int position, Transaction updatedTransaction) {
        if (position >= 0 && position < transactions.size()) {
            transactions.set(position, updatedTransaction);
            recomputeSubtotals();
            notifyItemRangeChanged(position, transactions.size() - position);
        }
    }

    public Transaction getTransactionAt(int position) {
        if (position >= 0 && position < transactions.size()) {
            return transactions.get(position);
        }
        return null;
    }

    public void restoreItem(int position, Transaction transaction) {
        transactions.add(position, transaction);
        notifyItemInserted(position);
        recomputeSubtotals();
        notifyItemRangeChanged(position, transactions.size() - position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView amount, date, note, subtotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.transaction_amount);
            date = itemView.findViewById(R.id.transaction_date);
            note = itemView.findViewById(R.id.transaction_note);
            subtotal = itemView.findViewById(R.id.transaction_subtotal);
        }

        public void bind(Transaction currentTransaction, String selectedCurrencySymbol, Context context, Double subtotalValue) {
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
                amount.setTextColor(context.getResources().getColor(R.color.colorOnSurface, itemView.getContext().getTheme()));
                amount.setText(Currency.formatAmount(selectedCurrencySymbol, currentTransaction.getAmount()));
            } else if (currentTransaction.getType().equals(TransactionType.WITHDRAW.VALUE)) {
                amount.setTextColor(context.getResources().getColor(R.color.md_theme_error, itemView.getContext().getTheme()));
                amount.setText(String.format("-%s", Currency.formatAmount(selectedCurrencySymbol, currentTransaction.getAmount())));
            }

            if (subtotalValue != null) {
                subtotal.setText(Currency.formatAmount(selectedCurrencySymbol, subtotalValue));
                subtotal.setVisibility(View.VISIBLE);
            } else {
                subtotal.setVisibility(View.GONE);
            }
        }
    }
}