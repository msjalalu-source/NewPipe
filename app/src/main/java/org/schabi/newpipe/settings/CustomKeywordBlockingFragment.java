package org.schabi.newpipe.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CustomKeywordBlockingFragment extends BasePreferenceFragment {
    public static final String KEY_BLOCKED_KEYWORDS = "custom_blocked_keywords";

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();

        final EditTextPreference addKeywordPref = findPreference("add_keyword_pref_key");
        if (addKeywordPref != null) {
            addKeywordPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof String) {
                    final String keyword = ((String) newValue).trim();
                    if (!TextUtils.isEmpty(keyword)) {
                        final Set<String> existing = getBlockedKeywords(requireContext());
                        boolean alreadyExists = false;
                        for (final String existingKw : existing) {
                            if (existingKw.equalsIgnoreCase(keyword)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (alreadyExists) {
                            Toast.makeText(requireContext(),
                                    R.string.keyword_already_blocked,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            addBlockedKeyword(requireContext(), keyword);
                            updateKeywordsList();
                        }
                    }
                }
                return false;
            });
        }

        updateKeywordsList();
    }

    private void updateKeywordsList() {
        final PreferenceCategory category = findPreference("blocked_keywords_category");
        if (category == null) {
            return;
        }

        category.removeAll();
        final Set<String> keywords = getBlockedKeywords(requireContext());
        if (keywords.isEmpty()) {
            final Preference emptyPref = new Preference(requireContext());
            emptyPref.setTitle(R.string.no_blocked_keywords_yet);
            emptyPref.setSelectable(false);
            emptyPref.setIconSpaceReserved(false);
            category.addPreference(emptyPref);
        } else {
            final List<String> sortedKeywords = new ArrayList<>(keywords);
            Collections.sort(sortedKeywords, String.CASE_INSENSITIVE_ORDER);
            for (final String kw : sortedKeywords) {
                final Preference pref = new Preference(requireContext());
                pref.setTitle(kw);
                pref.setSelectable(false);
                pref.setIconSpaceReserved(false);
                category.addPreference(pref);
            }
        }
    }

    @NonNull
    public static Set<String> getBlockedKeywords(final Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> set = sp.getStringSet(KEY_BLOCKED_KEYWORDS, Collections.emptySet());
        return set != null ? set : Collections.emptySet();
    }

    public static void addBlockedKeyword(final Context context, final String keyword) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> current = new HashSet<>(getBlockedKeywords(context));
        current.add(keyword.trim());
        sp.edit().putStringSet(KEY_BLOCKED_KEYWORDS, current).apply();
    }

    public static boolean isSearchQueryBlocked(final Context context, final String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        final Set<String> blockedKeywords = getBlockedKeywords(context);
        if (blockedKeywords.isEmpty()) {
            return false;
        }
        final String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (final String keyword : blockedKeywords) {
            if (!TextUtils.isEmpty(keyword)
                    && lowerQuery.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
