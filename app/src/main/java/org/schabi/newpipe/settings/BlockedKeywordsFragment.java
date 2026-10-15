package org.schabi.newpipe.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import org.schabi.newpipe.R;
import org.schabi.newpipe.databinding.DialogEditTextBinding;
import org.schabi.newpipe.util.SearchKeywordFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Settings screen for viewing predefined blocked search keywords/video IDs
 * and managing user-defined custom keywords.
 */
public class BlockedKeywordsFragment extends BasePreferenceFragment {

    private PreferenceCategory customCategory;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        final Context context = requireContext();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        screen.setTitle(R.string.blocked_keywords_title);
        setPreferenceScreen(screen);

        // Predefined Protected Keywords Category
        final PreferenceCategory predefinedCategory = new PreferenceCategory(context);
        predefinedCategory.setTitle(R.string.blocked_keywords_predefined_category);
        screen.addPreference(predefinedCategory);

        final Preference predefinedInfoPref = new Preference(context);
        predefinedInfoPref.setTitle(R.string.blocked_keywords_predefined_summary);
        predefinedInfoPref.setSelectable(false);
        predefinedCategory.addPreference(predefinedInfoPref);

        for (final String keyword : SearchKeywordFilter.PREDEFINED_KEYWORDS) {
            final Preference pref = new Preference(context);
            pref.setTitle(keyword);
            pref.setSummary(R.string.blocked_keywords_predefined_immutable);
            pref.setOnPreferenceClickListener(p -> {
                Toast.makeText(context, R.string.blocked_keywords_predefined_immutable,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            predefinedCategory.addPreference(pref);
        }

        for (final String videoId : SearchKeywordFilter.PREDEFINED_VIDEO_IDS) {
            final Preference pref = new Preference(context);
            pref.setTitle(getString(R.string.blocked_keywords_exact_video_id, videoId));
            pref.setSummary(R.string.blocked_keywords_predefined_immutable);
            pref.setOnPreferenceClickListener(p -> {
                Toast.makeText(context, R.string.blocked_keywords_predefined_immutable,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            predefinedCategory.addPreference(pref);
        }

        // Custom Protected Keywords Category
        customCategory = new PreferenceCategory(context);
        customCategory.setTitle(R.string.blocked_keywords_custom_category);
        screen.addPreference(customCategory);

        final Preference addPref = new Preference(context);
        addPref.setTitle(R.string.blocked_keywords_add_title);
        addPref.setSummary(R.string.blocked_keywords_add_summary);
        addPref.setIcon(R.drawable.ic_add);
        addPref.setOnPreferenceClickListener(p -> {
            showAddKeywordDialog();
            return true;
        });
        customCategory.addPreference(addPref);

        populateCustomKeywords();
    }

    private void populateCustomKeywords() {
        if (customCategory == null || getContext() == null) {
            return;
        }

        while (customCategory.getPreferenceCount() > 1) {
            customCategory.removePreference(customCategory.getPreference(1));
        }

        final Context context = requireContext();
        final Set<String> customKeywords = SearchKeywordFilter.getCustomKeywords(context);

        if (customKeywords.isEmpty()) {
            final Preference emptyPref = new Preference(context);
            emptyPref.setTitle(R.string.blocked_keywords_no_custom);
            emptyPref.setSelectable(false);
            customCategory.addPreference(emptyPref);
        } else {
            final List<String> sorted = new ArrayList<>(customKeywords);
            Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
            for (final String kw : sorted) {
                final Preference itemPref = new Preference(context);
                itemPref.setTitle(kw);
                itemPref.setSummary(R.string.blocked_keywords_custom_item_summary);
                itemPref.setIcon(R.drawable.ic_delete);
                itemPref.setOnPreferenceClickListener(p -> {
                    showDeleteConfirmDialog(kw);
                    return true;
                });
                customCategory.addPreference(itemPref);
            }
        }
    }

    private void showAddKeywordDialog() {
        final Context context = requireContext();
        final DialogEditTextBinding dialogBinding =
                DialogEditTextBinding.inflate(getLayoutInflater());
        dialogBinding.dialogEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        dialogBinding.dialogEditText.setHint(R.string.blocked_keywords_add_hint);

        new AlertDialog.Builder(context)
                .setTitle(R.string.blocked_keywords_add_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    final String text = dialogBinding.dialogEditText.getText().toString();
                    final String normalized = SearchKeywordFilter.normalizeKeyword(text);
                    if (normalized.isEmpty()) {
                        Toast.makeText(context, R.string.blocked_keywords_add_empty,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (SearchKeywordFilter.addCustomKeyword(context, normalized)) {
                        populateCustomKeywords();
                        Toast.makeText(context, R.string.blocked_keywords_added,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, R.string.blocked_keywords_add_exists,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showDeleteConfirmDialog(final String keyword) {
        final Context context = requireContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.blocked_keywords_delete_confirm_title)
                .setMessage(getString(R.string.blocked_keywords_delete_confirm_message, keyword))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (SearchKeywordFilter.deleteCustomKeyword(context, keyword)) {
                        populateCustomKeywords();
                        Toast.makeText(context, R.string.blocked_keywords_deleted,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
