package com.eipna.centsation.ui.adapters;

import android.content.Context;
import android.icu.text.NumberFormat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eipna.centsation.R;
import com.eipna.centsation.data.Currency;
import com.eipna.centsation.data.saving.Saving;
import com.eipna.centsation.data.saving.SavingOperation;
import com.eipna.centsation.util.AlarmUtil;
import com.eipna.centsation.util.DateUtil;
import com.eipna.centsation.util.PreferenceUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

public class SavingAdapter extends RecyclerView.Adapter<SavingAdapter.ViewHolder> {

    private final Context context;
    private final Listener listener;
    private final PreferenceUtil preferences;
    private final ArrayList<Saving> savings;

    public SavingAdapter(Context context, Listener listener, ArrayList<Saving> savings) {
        this.context = context;
        this.listener = listener;
        this.savings = savings;
        this.preferences = new PreferenceUtil(context);
    }

    public interface Listener {
        void OnClick(int position);
        void OnOperationClick(SavingOperation operation, int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View savingView = LayoutInflater.from(context).inflate(R.layout.recycler_saving, parent, false);
        return new ViewHolder(savingView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Saving currentSaving = savings.get(position);
        holder.bind(currentSaving, preferences);

        holder.itemView.setOnClickListener(view -> listener.OnClick(position));
        holder.delete.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.DELETE, position));
        holder.share.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.SHARE, position));
        holder.update.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.TRANSACTION, position));
        holder.archive.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.ARCHIVE, position));
        holder.unarchive.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.UNARCHIVE, position));
        holder.history.setOnClickListener(view -> listener.OnOperationClick(SavingOperation.HISTORY, position));
    }

    @Override
    public int getItemCount() {
        return savings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView parent;
        MaterialTextView name, saving, goal, percent, deadline, outOfText;
        MaterialButton update, history, archive, unarchive, delete, share;

        LinearLayout description;
        LinearProgressIndicator progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.saving_parent);
            name = itemView.findViewById(R.id.saving_name);
            saving = itemView.findViewById(R.id.saving_current_saving);
            goal = itemView.findViewById(R.id.saving_goal);
            percent = itemView.findViewById(R.id.saving_percent);
            outOfText = itemView.findViewById(R.id.saving_out_of_text);
            description = itemView.findViewById(R.id.saving_description);
            progress = itemView.findViewById(R.id.saving_progress);
            deadline = itemView.findViewById(R.id.saving_deadline);

            update = itemView.findViewById(R.id.saving_update);
            history = itemView.findViewById(R.id.saving_history);
            archive = itemView.findViewById(R.id.saving_archive);
            unarchive = itemView.findViewById(R.id.saving_unarchive);
            delete = itemView.findViewById(R.id.saving_delete);
            share = itemView.findViewById(R.id.saving_share);
        }

        public void bind(Saving currentSaving, PreferenceUtil preferences) {
            String currencySymbol = Currency.getSymbol(preferences.getCurrency());
            String deadlineFormat = preferences.getDeadlineFormat();
            int percentValue = (int) ((currentSaving.getCurrentSaving() / currentSaving.getGoal()) * 100);

            if (Currency.isRTLCurrency(preferences.getCurrency())) {
                description.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                saving.setTextDirection(View.TEXT_DIRECTION_RTL);
                goal.setTextDirection(View.TEXT_DIRECTION_RTL);
            } else {
                description.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                saving.setTextDirection(View.TEXT_DIRECTION_LTR);
                goal.setTextDirection(View.TEXT_DIRECTION_LTR);
            }

            if (currentSaving.getIsArchived() == Saving.IS_ARCHIVE) {
                archive.setVisibility(View.GONE);
                update.setVisibility(View.GONE);
                name.setAlpha(0.6f);

                SpannableString spannableString = new SpannableString(currentSaving.getName());
                spannableString.setSpan(new StrikethroughSpan(), 0, currentSaving.getName().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                name.setText(spannableString);
            } else {
                unarchive.setVisibility(View.GONE);
                name.setText(currentSaving.getName());
            }

            share.setVisibility(currentSaving.getNotes().isEmpty() ? View.GONE : View.VISIBLE);

            deadline.setVisibility(currentSaving.getDeadline() == AlarmUtil.NO_ALARM ? View.GONE : View.VISIBLE);
            deadline.setText(String.format("Deadline: %s", DateUtil.getStringDate(currentSaving.getDeadline(), deadlineFormat)));

            if (currentSaving.getGoal() <= 0) {
                goal.setVisibility(View.GONE);
                percent.setVisibility(View.GONE);
                outOfText.setVisibility(View.GONE);
                percentValue = 0;
            } else {
                goal.setVisibility(View.VISIBLE);
                percent.setVisibility(View.VISIBLE);
                outOfText.setVisibility(View.VISIBLE);
                percent.setText(String.format("(%s%c)", percentValue, '%'));
                goal.setText(String.format("%s%s", currencySymbol, NumberFormat.getInstance().format(currentSaving.getGoal())));
            }

            percent.setText(String.format("(%s%c)", percentValue, '%'));
            if (currentSaving.getGoal() != 0) {
                parent.setChecked(currentSaving.getCurrentSaving() >= currentSaving.getGoal());
            }
            saving.setText(String.format("%s%s", currencySymbol, NumberFormat.getInstance().format(currentSaving.getCurrentSaving())));
            goal.setText(String.format("%s%s", currencySymbol, NumberFormat.getInstance().format(currentSaving.getGoal())));
            progress.setProgress(percentValue, true);
        }
    }
}