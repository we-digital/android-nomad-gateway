package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class ForwardingRulesAdapter extends RecyclerView.Adapter<ForwardingRulesAdapter.ViewHolder> {

    private final Context context;
    private List<ForwardingConfig> rules;
    private List<ForwardingConfig> filteredRules;
    private final OnRuleActionListener listener;
    private int expandedPosition = -1;
    private ForwardingConfig.ActivityType currentFilter = null; // null means show all

    public interface OnRuleActionListener {
        void onRuleEdit(ForwardingConfig config);

        void onRuleDelete(ForwardingConfig config);

        void onRuleToggle(ForwardingConfig config, boolean enabled);
    }

    public ForwardingRulesAdapter(Context context, List<ForwardingConfig> rules, OnRuleActionListener listener) {
        this.context = context;
        this.rules = rules != null ? rules : new ArrayList<>();
        this.filteredRules = new ArrayList<>(this.rules);
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForwardingConfig config = filteredRules.get(position);
        boolean isExpanded = position == expandedPosition;

        // Set sender info
        holder.textSender.setText(config.sender);

        // Set sender initial
        String initial = config.sender.isEmpty() ? "?"
                : config.sender.equals("*") ? "*" : config.sender.substring(0, 1).toUpperCase();
        holder.senderInitial.setText(initial);

        // Set activity type chip
        setActivityTypeChip(holder.chipActivityType, config.getActivityType());

        // Set URL
        holder.textUrl.setText(config.url);

        // Set template
        holder.textTemplate.setText(config.getJsonTemplate());

        // Set headers
        holder.textHeaders.setText(config.headers);

        // Set switch state without triggering listener
        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(config.isOn);
        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onRuleToggle(config, isChecked);
            }
        });

        // Handle expansion
        holder.detailsSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Handle actions
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRuleEdit(config);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_record)
                    .setMessage(context.getString(R.string.confirm_delete, config.sender))
                    .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                        if (listener != null) {
                            listener.onRuleDelete(config);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });

        // Card click triggers expand/collapse
        holder.cardView.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;

            if (previousExpanded != -1) {
                notifyItemChanged(previousExpanded);
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredRules.size();
    }

    @Override
    public long getItemId(int position) {
        return filteredRules.get(position).id;
    }

    private void setActivityTypeChip(Chip chip, ForwardingConfig.ActivityType activityType) {
        switch (activityType) {
            case SMS:
                chip.setText("SMS");
                chip.setChipIcon(context.getDrawable(R.drawable.ic_sms));
                break;
            case PUSH:
                chip.setText("Push");
                chip.setChipIcon(context.getDrawable(R.drawable.ic_notifications));
                break;
            case CALL:
                chip.setText("Calls");
                chip.setChipIcon(context.getDrawable(R.drawable.ic_call));
                break;
            default:
                chip.setText("SMS");
                chip.setChipIcon(context.getDrawable(R.drawable.ic_sms));
                break;
        }
    }

    public void setFilter(ForwardingConfig.ActivityType activityType) {
        this.currentFilter = activityType;
        applyFilter();
    }

    public void clearFilter() {
        this.currentFilter = null;
        applyFilter();
    }

    private void applyFilter() {
        filteredRules.clear();

        if (currentFilter == null) {
            // Show all rules
            filteredRules.addAll(rules);
        } else {
            // Filter by activity type
            for (ForwardingConfig config : rules) {
                if (config.getActivityType() == currentFilter) {
                    filteredRules.add(config);
                }
            }
        }

        // Reset expanded position when filtering
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    public void updateRules(List<ForwardingConfig> newRules) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return rules.size();
            }

            @Override
            public int getNewListSize() {
                return newRules.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return rules.get(oldItemPosition).id == newRules.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ForwardingConfig oldConfig = rules.get(oldItemPosition);
                ForwardingConfig newConfig = newRules.get(newItemPosition);
                return oldConfig.sender.equals(newConfig.sender) &&
                        oldConfig.url.equals(newConfig.url) &&
                        oldConfig.isOn == newConfig.isOn &&
                        oldConfig.getActivityType() == newConfig.getActivityType() &&
                        oldConfig.getJsonTemplate().equals(newConfig.getJsonTemplate()) &&
                        oldConfig.headers.equals(newConfig.headers);
            }
        });

        rules = newRules;
        applyFilter(); // Reapply current filter with new rules
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView senderInitial;
        final TextView textSender;
        final TextView textUrl;
        final TextView textTemplate;
        final TextView textHeaders;
        final MaterialSwitch switchEnabled;
        final View detailsSection;
        final MaterialButton editButton;
        final MaterialButton deleteButton;
        final Chip chipActivityType;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.list_item_card);
            senderInitial = itemView.findViewById(R.id.sender_initial);
            textSender = itemView.findViewById(R.id.text_sender);
            textUrl = itemView.findViewById(R.id.text_url);
            textTemplate = itemView.findViewById(R.id.text_template);
            textHeaders = itemView.findViewById(R.id.text_headers);
            switchEnabled = itemView.findViewById(R.id.switch_sms_on_off);
            detailsSection = itemView.findViewById(R.id.details_section);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            chipActivityType = itemView.findViewById(R.id.chip_activity_type);
        }
    }
}