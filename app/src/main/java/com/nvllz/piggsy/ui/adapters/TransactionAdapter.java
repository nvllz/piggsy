package com.nvllz.piggsy.ui.adapters;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionType;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.textview.MaterialTextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public static final String PAYLOAD_SUBTOTAL = "payload_subtotal";

    private final Context context;
    private final ArrayList<Transaction> transactions;
    private final String currency;
    private final ArrayList<Double> subtotals = new ArrayList<>();

    private final SimpleDateFormat dateFormat;
    private final Date reusedDate = new Date();
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    private final String amountPrefix;
    private final String amountSuffix;
    private final int colorDeposit;
    private final int colorWithdraw;
    private final int colorOnSurface;

    public TransactionAdapter(Context context, ArrayList<Transaction> transactions, String currency) {
        this.context = context;
        this.transactions = transactions;
        this.currency = currency;
        recomputeSubtotals();

        String datePattern = new PreferenceUtil(context).getDateFormat();
        dateFormat = new SimpleDateFormat(datePattern + " HH:mm", Locale.getDefault());

        String symbol = Currency.getSymbol(currency);
        if (Currency.isSuffixCurrency(currency)) {
            amountPrefix = "";
            amountSuffix = symbol != null ? " " + symbol : "";
        } else {
            amountPrefix = symbol != null ? symbol : "";
            amountSuffix = "";
        }

        colorDeposit = ContextCompat.getColor(context, R.color.colorDeposit);
        colorWithdraw = ContextCompat.getColor(context, R.color.colorWithdraw);
        colorOnSurface = ContextCompat.getColor(context, R.color.colorOnSurface);
    }

    private void recomputeSubtotals() {
        subtotals.clear();

        double balance = 0;
        for (Transaction t : transactions) {
            if (TransactionType.CREATED.VALUE.equals(t.getType())) {
                balance = t.getAmount();
                break;
            }
        }

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

        reusedDate.setTime(currentTransaction.getDate());
        holder.bind(currentTransaction,
                dateFormat.format(reusedDate),
                formatAmount(currentTransaction.getAmount()),
                colorFor(currentTransaction.getType()),
                subtotal != null ? formatAmount(subtotal) : null);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionAdapter.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_SUBTOTAL)) {
            Double subtotal = position < subtotals.size() ? subtotals.get(position) : null;
            holder.bindSubtotal(subtotal != null ? formatAmount(subtotal) : null);
            return;
        }

        onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void removeItem(int position) {
        if (position < 0 || position >= transactions.size()) return;

        transactions.remove(position);
        recomputeSubtotals();

        notifyItemRemoved(position);
        notifyItemsAboveOf(position);
    }

    public void updateItem(int position, Transaction updatedTransaction) {
        if (position < 0 || position >= transactions.size()) return;

        transactions.set(position, updatedTransaction);
        recomputeSubtotals();

        notifyItemChanged(position);
        notifyItemsAboveOf(position);
    }

    public void restoreItem(int position, Transaction transaction) {
        transactions.add(position, transaction);
        recomputeSubtotals();

        notifyItemInserted(position);
        notifyItemsAboveOf(position);
    }

    private void notifyItemsAboveOf(int position) {
        if (position > 0) {
            notifyItemRangeChanged(0, position, PAYLOAD_SUBTOTAL);
        }
    }

    public Transaction getTransactionAt(int position) {
        if (position >= 0 && position < transactions.size()) {
            return transactions.get(position);
        }
        return null;
    }

    private String formatAmount(double amount) {
        return amountPrefix + numberFormat.format(amount) + amountSuffix;
    }

    private int colorFor(String type) {
        if (TransactionType.DEPOSIT.VALUE.equals(type)) {
            return colorDeposit;
        } else if (TransactionType.WITHDRAW.VALUE.equals(type)) {
            return colorWithdraw;
        }
        return colorOnSurface;
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

        public void bind(Transaction currentTransaction,
                         String formattedDate,
                         String formattedAmount,
                         int amountColor,
                         @Nullable String subtotalValue) {
            date.setText(formattedDate);

            String transactionNote = currentTransaction.getNote();
            if (!TextUtils.isEmpty(transactionNote) && !transactionNote.trim().isEmpty()) {
                note.setText(transactionNote);
                note.setVisibility(View.VISIBLE);
            } else {
                note.setVisibility(View.GONE);
            }

            if (TransactionType.DEPOSIT.VALUE.equals(currentTransaction.getType())) {
                amount.setText(String.format("+%s", formattedAmount));
            } else if (TransactionType.WITHDRAW.VALUE.equals(currentTransaction.getType())) {
                amount.setText(String.format("-%s", formattedAmount));
            } else {
                amount.setText(formattedAmount);
            }
            amount.setTextColor(amountColor);

            bindSubtotal(subtotalValue);
        }

        public void bindSubtotal(@Nullable String subtotalValue) {
            if (subtotalValue != null) {
                subtotal.setText(subtotalValue);
                subtotal.setVisibility(View.VISIBLE);
            } else {
                subtotal.setVisibility(View.GONE);
            }
        }
    }
}

