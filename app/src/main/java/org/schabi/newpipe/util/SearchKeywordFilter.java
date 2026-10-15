package org.schabi.newpipe.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.App;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for managing and checking restricted search keywords and video IDs.
 */
public final class SearchKeywordFilter {

    public static final String PREF_CUSTOM_BLOCKED_KEYWORDS =
            "custom_blocked_keywords_key";

    public static final List<String> PREDEFINED_KEYWORDS =
            Collections.unmodifiableList(Arrays.asList(
                    "Aashiq Banaya",
                    "adult",
                    "porn",
                    "sex",
                    "xxx",
                    "18+",
                    "intimate",
                    "kiss",
                    "hot"
            ));

    public static final List<String> PREDEFINED_VIDEO_IDS =
            Collections.unmodifiableList(Arrays.asList(
                    "0bAVd9jJE2Q",
                    "19E65tOn3tI",
                    "kbObKrBoIjI",
                    "3QCgD4R4Ly8",
                    "RlbGLrMuZ5I",
                    "7vBsI0Hv5L0"
            ));

    private static final List<Pattern> PREDEFINED_PATTERNS;

    static {
        final List<Pattern> patterns = new ArrayList<>();
        for (final String videoId : PREDEFINED_VIDEO_IDS) {
            patterns.add(buildVideoIdPattern(videoId));
        }
        for (final String keyword : PREDEFINED_KEYWORDS) {
            patterns.add(buildKeywordPattern(keyword));
        }
        PREDEFINED_PATTERNS = Collections.unmodifiableList(patterns);
    }

    private SearchKeywordFilter() {
        // Utility class
    }

    /**
     * Normalizes a keyword string by trimming outer whitespace and collapsing
     * internal sequences of whitespace into a single space.
     *
     * @param input Raw keyword string
     * @return Normalized string, or empty string if input is null
     */
    @NonNull
    public static String normalizeKeyword(@Nullable final String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Builds a regex pattern for matching a video ID as a complete identifier rather
     * than an arbitrary substring.
     *
     * @param videoId The video ID to match
     * @return Compiled regex pattern
     */
    @NonNull
    public static Pattern buildVideoIdPattern(@NonNull final String videoId) {
        final String regex = "(?<![a-zA-Z0-9_-])"
                + Pattern.quote(videoId.trim())
                + "(?![a-zA-Z0-9_-])";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Builds a regex pattern for matching a keyword or phrase with sensible word boundaries
     * to avoid accidental substring matches.
     *
     * @param keyword The keyword or phrase to match
     * @return Compiled regex pattern
     */
    @NonNull
    public static Pattern buildKeywordPattern(@NonNull final String keyword) {
        final String normalized = normalizeKeyword(keyword);
        final String[] tokens = normalized.split(" ");
        final StringBuilder patternBuilder = new StringBuilder();

        // Preceding boundary: ensure previous char is not alphanumeric
        if (!normalized.isEmpty() && Character.isLetterOrDigit(normalized.charAt(0))) {
            patternBuilder.append("(?<![\\p{L}\\p{N}])");
        }

        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                patternBuilder.append("\\s+");
            }
            patternBuilder.append(Pattern.quote(tokens[i]));
        }

        // Succeeding boundary: ensure next char is not alphanumeric
        if (!normalized.isEmpty()) {
            patternBuilder.append("(?![\\p{L}\\p{N}])");
        }

        return Pattern.compile(patternBuilder.toString(),
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Checks if a given query matches any predefined or custom blocked keywords/video IDs.
     *
     * @param query The search query to check
     * @param customKeywords Collection of custom keywords (optional)
     * @return true if the query is blocked, false otherwise
     */
    public static boolean isQueryBlocked(@Nullable final String query,
                                         @Nullable final Collection<String> customKeywords) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        // Check predefined patterns first
        for (final Pattern pattern : PREDEFINED_PATTERNS) {
            if (pattern.matcher(query).find()) {
                return true;
            }
        }

        // Check custom keywords if present
        if (customKeywords != null) {
            for (final String custom : customKeywords) {
                final String normalized = normalizeKeyword(custom);
                if (!normalized.isEmpty()) {
                    final Pattern pattern = buildKeywordPattern(normalized);
                    if (pattern.matcher(query).find()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a search query is blocked using SharedPreferences for custom keywords.
     *
     * @param context Application or activity context
     * @param query The query to test
     * @return true if blocked
     */
    public static boolean isSearchBlocked(@NonNull final Context context,
                                          @Nullable final String query) {
        final Set<String> customKeywords = getCustomKeywords(context);
        return isQueryBlocked(query, customKeywords);
    }

    /**
     * Checks if a search query is blocked using the global application context if available.
     *
     * @param query The query to test
     * @return true if blocked
     */
    public static boolean isSearchBlocked(@Nullable final String query) {
        try {
            if (App.getInstance() != null) {
                return isSearchBlocked(App.getInstance(), query);
            }
        } catch (final Throwable ignored) {
            // App instance not yet initialized, fallback to predefined patterns
        }
        return isQueryBlocked(query, null);
    }

    /**
     * Retrieves the set of custom blocked keywords stored in SharedPreferences.
     *
     * @param context Application or activity context
     * @return Set of custom keywords
     */
    @NonNull
    public static Set<String> getCustomKeywords(@NonNull final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> set = prefs.getStringSet(PREF_CUSTOM_BLOCKED_KEYWORDS, null);
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    /**
     * Attempts to add a custom keyword.
     * Normalizes whitespace and checks for duplicates case-insensitively against both
     * predefined entries and existing custom entries.
     *
     * @param context Application or activity context
     * @param keyword The keyword to add
     * @return true if added, false if invalid or duplicate
     */
    public static boolean addCustomKeyword(@NonNull final Context context,
                                           @Nullable final String keyword) {
        final String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return false;
        }

        final String lower = normalized.toLowerCase(Locale.ROOT);

        // Check against predefined keywords
        for (final String predefined : PREDEFINED_KEYWORDS) {
            if (predefined.toLowerCase(Locale.ROOT).equals(lower)) {
                return false;
            }
        }

        // Check against predefined video IDs
        for (final String videoId : PREDEFINED_VIDEO_IDS) {
            if (videoId.toLowerCase(Locale.ROOT).equals(lower)) {
                return false;
            }
        }

        // Check against existing custom keywords
        final Set<String> existing = getCustomKeywords(context);
        for (final String item : existing) {
            if (item.toLowerCase(Locale.ROOT).equals(lower)) {
                return false;
            }
        }

        existing.add(normalized);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(PREF_CUSTOM_BLOCKED_KEYWORDS, existing).apply();
        return true;
    }

    /**
     * Deletes a custom keyword. Predefined entries can never be deleted.
     *
     * @param context Application or activity context
     * @param keyword The custom keyword to delete
     * @return true if removed, false otherwise
     */
    public static boolean deleteCustomKeyword(@NonNull final Context context,
                                              @Nullable final String keyword) {
        final String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return false;
        }

        final Set<String> existing = getCustomKeywords(context);
        String toRemove = null;
        for (final String item : existing) {
            if (item.equalsIgnoreCase(normalized)) {
                toRemove = item;
                break;
            }
        }

        if (toRemove != null) {
            existing.remove(toRemove);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putStringSet(PREF_CUSTOM_BLOCKED_KEYWORDS, existing).apply();
            return true;
        }

        return false;
    }

    /**
     * Checks whether a keyword is one of the immutable predefined entries.
     *
     * @param keyword Keyword to check
     * @return true if predefined
     */
    public static boolean isPredefined(@Nullable final String keyword) {
        if (keyword == null) {
            return false;
        }
        final String normalized = normalizeKeyword(keyword);
        for (final String item : PREDEFINED_KEYWORDS) {
            if (item.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        for (final String id : PREDEFINED_VIDEO_IDS) {
            if (id.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
