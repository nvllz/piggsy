package com.nvllz.piggsy.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Currency;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingOperation;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.DateUtil;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Date;

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
        MaterialButton update, history, note, archive, unarchive, delete;
        MaterialCardView percentContainer, deadlineContainer;

        LinearLayout description;
        LinearProgressIndicator progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.saving_parent);
            name = itemView.findViewById(R.id.saving_name);
            saving = itemView.findViewById(R.id.saving_current_saving);
            outOfText = itemView.findViewById(R.id.saving_out_of_text);
            goal = itemView.findViewById(R.id.saving_goal);
            percent = itemView.findViewById(R.id.saving_percent);
            percentContainer = itemView.findViewById(R.id.saving_percent_container);
            description = itemView.findViewById(R.id.saving_description);
            progress = itemView.findViewById(R.id.saving_progress);
            deadline = itemView.findViewById(R.id.saving_deadline);
            deadlineContainer = itemView.findViewById(R.id.saving_deadline_container);

            update = itemView.findViewById(R.id.saving_update);
            history = itemView.findViewById(R.id.saving_history);
            note = itemView.findViewById(R.id.saving_note);
            archive = itemView.findViewById(R.id.saving_archive);
            unarchive = itemView.findViewById(R.id.saving_unarchive);
            delete = itemView.findViewById(R.id.saving_delete);
        }

        public void bind(Saving currentSaving, PreferenceUtil preferences) {
            int percentValue = (int) ((currentSaving.getCurrentSaving() / currentSaving.getGoal()) * 100);
            percentValue = Math.max(0, Math.min(100, percentValue));

            configureTextDirection(currentSaving.getCurrency());
            configureArchivedState(currentSaving);

            boolean isDeadlineDue = configureDeadline(currentSaving, preferences.getDateFormat(), percentValue);
            configureGoalAndProgress(currentSaving, percentValue);

            configureParentCardState(currentSaving, isDeadlineDue);

            saving.setText(Currency.formatAmount(currentSaving.getCurrency(), currentSaving.getCurrentSaving()));

            boolean hasDescription = currentSaving.getDescription() != null
                    && !currentSaving.getDescription().trim().isEmpty();
            note.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            TooltipCompat.setTooltipText(note, hasDescription ? currentSaving.getDescription() : null);
            note.setOnClickListener(View::performLongClick);
        }

        private void configureTextDirection(String currency) {
            boolean isRTL = Currency.isRTLCurrency(currency);
            int layoutDirection = isRTL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
            int textDirection = isRTL ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;

            description.setLayoutDirection(layoutDirection);
            saving.setTextDirection(textDirection);
            goal.setTextDirection(textDirection);
        }

        private void configureArchivedState(Saving currentSaving) {
            name.setText(currentSaving.getName());

            if (currentSaving.getIsArchived() == Saving.IS_ARCHIVE) {
                archive.setVisibility(View.GONE);
                update.setVisibility(View.GONE);

                View[] archivedElements = {name, saving, goal, percent, percentContainer,
                        deadlineContainer, deadline,
                        progress, outOfText};
                for (View element : archivedElements) {
                    element.setAlpha(0.6f);
                }
            } else {
                unarchive.setVisibility(View.GONE);
                delete.setVisibility(View.GONE);
            }
        }

        private boolean configureDeadline(Saving currentSaving, String deadlineFormat, Integer percentValue) {
            boolean isDeadlineDue = false;
            Context ctx = itemView.getContext();

            int defaultProgressColor = getThemeColor(ctx, android.R.attr.colorPrimary);
            int defaultPercentBg = getThemeColor(ctx, com.google.android.material.R.attr.colorPrimaryContainer);
            int defaultPercentText = getThemeColor(ctx, com.google.android.material.R.attr.colorOnPrimaryContainer);

            if (currentSaving.getDeadline() == AlarmUtil.NO_ALARM) {
                deadlineContainer.setVisibility(View.GONE);
                progress.setIndicatorColor(defaultProgressColor);
                percentContainer.setCardBackgroundColor(defaultPercentBg);
                percent.setTextColor(defaultPercentText);
            } else {
                deadlineContainer.setVisibility(View.VISIBLE);
                deadline.setText(DateUtil.getStringDate(currentSaving.getDeadline(), deadlineFormat));

                Date today = DateUtil.getTodayWithoutTime();
                Date deadlineDate = DateUtil.getDateWithoutTime(currentSaving.getDeadline());

                if (!deadlineDate.after(today)) {
                    int errorBg = getThemeColor(ctx, com.google.android.material.R.attr.colorErrorContainer);
                    int errorText = getThemeColor(ctx, com.google.android.material.R.attr.colorOnErrorContainer);
                    deadlineContainer.setCardBackgroundColor(errorBg);
                    deadline.setTextColor(errorText);
                    if (percentValue < 100) {
                        progress.setIndicatorColor(errorBg);
                        percentContainer.setCardBackgroundColor(errorBg);
                        percent.setTextColor(errorText);
                    }
                    isDeadlineDue = true;
                } else {
                    deadlineContainer.setCardBackgroundColor(getThemeColor(ctx, com.google.android.material.R.attr.colorSecondaryContainer));
                    deadline.setTextColor(getThemeColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer));
                    progress.setIndicatorColor(defaultProgressColor);
                    percentContainer.setCardBackgroundColor(defaultPercentBg);
                    percent.setTextColor(defaultPercentText);
                }
            }

            if (percentValue >= 100) {
                percentContainer.setCardBackgroundColor(getThemeColor(ctx, android.R.attr.colorPrimary));
                percent.setTextColor(getThemeColor(ctx, com.google.android.material.R.attr.colorOnPrimary));
            }

            return isDeadlineDue;
        }

        private void configureGoalAndProgress(Saving currentSaving, int percentValue) {
            boolean hasGoal = currentSaving.getGoal() > 0;
            int visibility = hasGoal ? View.VISIBLE : View.GONE;

            View[] goalElements = {goal, percent, outOfText, progress};
            for (View element : goalElements) {
                element.setVisibility(visibility);
            }

            if (hasGoal) {
                progress.setProgress(percentValue, true);
                percent.setText(String.format("%s%c", percentValue, '%'));
                goal.setText(Currency.formatAmount(currentSaving.getCurrency(), currentSaving.getGoal()));
            }
        }

        private void configureParentCardState(Saving currentSaving, boolean isDeadlineDue) {
            if (currentSaving.getCurrentSaving() >= currentSaving.getGoal() && !(currentSaving.getGoal() == 0)) {
                parent.setStrokeWidth((int) (2 * itemView.getContext().getResources().getDisplayMetrics().density));
                parent.setStrokeColor(ColorStateList.valueOf(getThemeColor(itemView.getContext(), android.R.attr.colorPrimary)));
            } else if (isDeadlineDue) {
                parent.setStrokeWidth((int) (2 * itemView.getContext().getResources().getDisplayMetrics().density));
                parent.setStrokeColor(ColorStateList.valueOf(getThemeColor(itemView.getContext(), android.R.attr.colorError)));
            } else {
                parent.setStrokeWidth(0);
                parent.setStrokeColor(ColorStateList.valueOf(0));
            }
        }

        private static int getThemeColor(Context context, int attrResId) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(attrResId, typedValue, true);
            return typedValue.data;
        }
    }
}