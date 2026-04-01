package com.nvllz.piggsy.ui.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.nvllz.piggsy.R;

public class SwipeToActionCallback extends ItemTouchHelper.SimpleCallback {

    private final SwipeActionListener listener;
    private final Context context;

    public interface SwipeActionListener {
        void onSwipeLeft(int position);
        void onSwipeRight(int position);
        boolean isTransactionDeletable(int position);
        boolean isSavingArchived(int position);
    }

    public SwipeToActionCallback(Context context, SwipeActionListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();

        if (listener.isSavingArchived(position)) {
            return 0;
        }

        boolean canDelete = listener.isTransactionDeletable(position);

        if (canDelete) {
            return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        } else {
            return ItemTouchHelper.RIGHT;
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeLeft(position);
        } else if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipeRight(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;

            if (dX > 0) {
                drawEditBackground(c, itemView, dX);
            } else if (dX < 0) {
                drawDeleteBackground(c, itemView, dX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawEditBackground(Canvas c, View itemView, float dX) {
        int backgroundColor = ContextCompat.getColor(context, R.color.md_theme_secondary);
        c.drawRect(itemView.getLeft(), itemView.getTop(),
                itemView.getLeft() + dX, itemView.getBottom(),
                createPaint(backgroundColor));

        Drawable editIcon = ContextCompat.getDrawable(context, R.drawable.ic_pencil_hero);
        if (editIcon != null) {
            int iconMargin = (itemView.getHeight() - editIcon.getIntrinsicHeight()) / 2;
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + editIcon.getIntrinsicWidth();
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + editIcon.getIntrinsicHeight();

            editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            editIcon.setTint(ContextCompat.getColor(context, R.color.md_theme_onSecondary));
            editIcon.draw(c);
        }
    }

    private void drawDeleteBackground(Canvas c, View itemView, float dX) {
        int backgroundColor = ContextCompat.getColor(context, R.color.md_theme_error);
        c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                itemView.getRight(), itemView.getBottom(),
                createPaint(backgroundColor));

        Drawable deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_trash_hero);
        if (deleteIcon != null) {
            int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconRight = itemView.getRight() - iconMargin;
            int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.setTint(ContextCompat.getColor(context, R.color.md_theme_onError));
            deleteIcon.draw(c);
        }
    }

    private android.graphics.Paint createPaint(int color) {
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(color);
        return paint;
    }
}