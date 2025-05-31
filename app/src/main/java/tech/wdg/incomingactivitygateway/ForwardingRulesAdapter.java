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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class ForwardingRulesAdapter extends RecyclerView.Adapter<ForwardingRulesAdapter.ViewHolder> {

    private final Context context;
    private List<ForwardingConfig> rules;
    private final OnRuleActionListener listener;
    private int expandedPosition = -1;

    public interface OnRuleActionListener {
        void onRuleEdit(ForwardingConfig config);

        void onRuleDelete(ForwardingConfig config);

        void onRuleToggle(ForwardingConfig config, boolean enabled);
    }

    public ForwardingRulesAdapter(Context context, List<ForwardingConfig> rules, OnRuleActionListener listener) {
        this.context = context;
        this.rules = rules != null ? rules : new ArrayList<>();
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
        ForwardingConfig config = rules.get(position);
        boolean isExpanded = position == expandedPosition;

        // Set sender info
        holder.textSender.setText(config.sender);

        // Set sender initial
        String initial = config.sender.isEmpty() ? "?"
                : config.sender.equals("*") ? "*" : config.sender.substring(0, 1).toUpperCase();
        holder.senderInitial.setText(initial);

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
        holder.expandButton.setText(isExpanded ? "Hide Details" : "Show Details");
        holder.expandButton.setIconResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);

        holder.expandButton.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;

            if (previousExpanded != -1) {
                notifyItemChanged(previousExpanded);
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition);
            }
        });

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

        // Add ripple effect on card click
        holder.cardView.setOnClickListener(v -> {
            holder.expandButton.performClick();
        });
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    @Override
    public long getItemId(int position) {
        return rules.get(position).id;
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
                        oldConfig.getJsonTemplate().equals(newConfig.getJsonTemplate()) &&
                        oldConfig.headers.equals(newConfig.headers);
            }
        });

        rules = newRules;
        diffResult.dispatchUpdatesTo(this);
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
        final MaterialButton expandButton;
        final MaterialButton editButton;
        final MaterialButton deleteButton;

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
            expandButton = itemView.findViewById(R.id.expand_button);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}