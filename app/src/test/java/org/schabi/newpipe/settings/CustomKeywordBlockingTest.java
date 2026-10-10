package org.schabi.newpipe.settings;

import android.content.ContextWrapper;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomKeywordBlockingTest {

    private FakeContext fakeContext;
    private FakeSharedPreferences fakePreferences;

    @Before
    public void setUp() {
        fakePreferences = new FakeSharedPreferences();
        fakeContext = new FakeContext(fakePreferences);
    }

    @Test
    public void testInitializationContainsAllDefaultKeywords() {
        final Set<String> keywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        assertEquals(15, keywords.size());
        assertTrue(keywords.contains("Aashiq Banaya"));
        assertTrue(keywords.contains("adult"));
        assertTrue(keywords.contains("porn"));
        assertTrue(keywords.contains("sex"));
        assertTrue(keywords.contains("xxx"));
        assertTrue(keywords.contains("18+"));
        assertTrue(keywords.contains("intimate"));
        assertTrue(keywords.contains("kiss"));
        assertTrue(keywords.contains("0bAVd9jJE2Q&vl=en"));
        assertTrue(keywords.contains("19E65tOn3tI"));
        assertTrue(keywords.contains("kbObKrBoIjI"));
        assertTrue(keywords.contains("3QCgD4R4Ly8"));
        assertTrue(keywords.contains("RlbGLrMuZ5I"));
        assertTrue(keywords.contains("7vBsI0Hv5L0"));
        assertTrue(keywords.contains("Hot"));
    }

    @Test
    public void testExistingCustomKeywordsPreservedDuringMigration() {
        // Pre-populate with existing user custom keywords
        final Set<String> existingKeywords = new HashSet<>(Arrays.asList("violence", "gambling"));
        fakePreferences.edit()
                .putStringSet(CustomKeywordBlockingFragment.KEY_BLOCKED_KEYWORDS, existingKeywords)
                .apply();

        final Set<String> keywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        // All 15 defaults plus the 2 existing user custom keywords = 17 total
        assertEquals(17, keywords.size());
        assertTrue(keywords.contains("violence"));
        assertTrue(keywords.contains("gambling"));
        for (final String defaultKw : CustomKeywordBlockingFragment.DEFAULT_BLOCKED_KEYWORDS) {
            assertTrue(keywords.contains(defaultKw));
        }
    }

    @Test
    public void testInitializationDoesNotDuplicateKeywordsCaseInsensitively() {
        // User already had uppercase "PORN" and "SEX"
        final Set<String> existingKeywords = new HashSet<>(Arrays.asList("PORN", "SEX", "my_keyword"));
        fakePreferences.edit()
                .putStringSet(CustomKeywordBlockingFragment.KEY_BLOCKED_KEYWORDS, existingKeywords)
                .apply();

        final Set<String> keywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        // 3 original (PORN, SEX, my_keyword) + 13 remaining defaults (excluding porn and sex) = 16
        assertEquals(16, keywords.size());
        assertTrue(keywords.contains("my_keyword"));
        assertTrue(keywords.contains("PORN"));
        assertTrue(keywords.contains("SEX"));
        assertFalse(keywords.contains("porn")); // not duplicated with lowercase
        assertFalse(keywords.contains("sex"));
    }

    @Test
    public void testRemovedKeywordIsNotRestoredOnSubsequentCalls() {
        // First access initializes defaults
        final Set<String> initialKeywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);
        assertEquals(15, initialKeywords.size());

        // Simulate an existing stored preference where a keyword was absent/removed
        final Set<String> modified = new HashSet<>(initialKeywords);
        modified.remove("kiss");
        fakePreferences.edit()
                .putStringSet(CustomKeywordBlockingFragment.KEY_BLOCKED_KEYWORDS, modified)
                .apply();

        final Set<String> afterRemoval = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);
        assertEquals(14, afterRemoval.size());
        assertFalse(afterRemoval.contains("kiss"));

        // Subsequent call must NOT re-add the removed default
        final Set<String> subsequentKeywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);
        assertEquals(14, subsequentKeywords.size());
        assertFalse(subsequentKeywords.contains("kiss"));
    }

    @Test
    public void testMatchingIsCaseInsensitive() {
        // Ensure defaults are initialized
        CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        // Test variations of case for default keywords
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "PORN video"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "funny SeX clip"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "aashiq banaya song"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "movie 18+ uncut"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "first KISS scene"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "INTIMATE moments"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "adult content"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "XXX rated"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "watch 0BAVD9JJE2Q&VL=EN now"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "video 19e65ton3ti live"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "check hot news"));
    }

    @Test
    public void testAllowedQueriesPass() {
        CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        assertFalse(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "nature documentary"));
        assertFalse(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "classical music orchestra"));
        assertFalse(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "python programming tutorial"));
        assertFalse(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, ""));
        assertFalse(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, null));
    }

    @Test
    public void testAddBlockedKeywordDuplicatePrevention() {
        CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);

        // Adding an already existing keyword case-insensitively should not duplicate
        CustomKeywordBlockingFragment.addBlockedKeyword(fakeContext, "ADULT");
        final Set<String> keywords = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);
        assertEquals(15, keywords.size());

        // Adding a new custom keyword
        CustomKeywordBlockingFragment.addBlockedKeyword(fakeContext, "custom_tag");
        final Set<String> afterAdd = CustomKeywordBlockingFragment.getBlockedKeywords(fakeContext);
        assertEquals(16, afterAdd.size());
        assertTrue(afterAdd.contains("custom_tag"));
        assertTrue(CustomKeywordBlockingFragment.isSearchQueryBlocked(fakeContext, "find custom_tag today"));
    }

    private static class FakeContext extends ContextWrapper {
        private final SharedPreferences preferences;

        FakeContext(final SharedPreferences preferences) {
            super(null);
            this.preferences = preferences;
        }

        @Override
        public SharedPreferences getSharedPreferences(final String name, final int mode) {
            return preferences;
        }

        @Override
        public String getPackageName() {
            return "org.schabi.newpipe";
        }
    }

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(storage);
        }

        @Override
        public String getString(final String key, final String defValue) {
            return storage.containsKey(key) ? (String) storage.get(key) : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(final String key, final Set<String> defValues) {
            return storage.containsKey(key) ? new HashSet<>((Set<String>) storage.get(key)) : defValues;
        }

        @Override
        public int getInt(final String key, final int defValue) {
            return storage.containsKey(key) ? (Integer) storage.get(key) : defValue;
        }

        @Override
        public long getLong(final String key, final long defValue) {
            return storage.containsKey(key) ? (Long) storage.get(key) : defValue;
        }

        @Override
        public float getFloat(final String key, final float defValue) {
            return storage.containsKey(key) ? (Float) storage.get(key) : defValue;
        }

        @Override
        public boolean getBoolean(final String key, final boolean defValue) {
            return storage.containsKey(key) ? (Boolean) storage.get(key) : defValue;
        }

        @Override
        public boolean contains(final String key) {
            return storage.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor(this);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                final OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                final OnSharedPreferenceChangeListener listener) {
        }

        private static class FakeEditor implements Editor {
            private final FakeSharedPreferences parent;
            private final Map<String, Object> tempChanges = new HashMap<>();
            private final Set<String> removedKeys = new HashSet<>();

            FakeEditor(final FakeSharedPreferences parent) {
                this.parent = parent;
            }

            @Override
            public Editor putString(final String key, final String value) {
                tempChanges.put(key, value);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor putStringSet(final String key, final Set<String> values) {
                tempChanges.put(key, values != null ? new HashSet<>(values) : null);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor putInt(final String key, final int value) {
                tempChanges.put(key, value);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor putLong(final String key, final long value) {
                tempChanges.put(key, value);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor putFloat(final String key, final float value) {
                tempChanges.put(key, value);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor putBoolean(final String key, final boolean value) {
                tempChanges.put(key, value);
                removedKeys.remove(key);
                return this;
            }

            @Override
            public Editor remove(final String key) {
                removedKeys.add(key);
                tempChanges.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                tempChanges.clear();
                removedKeys.addAll(parent.storage.keySet());
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                for (final String key : removedKeys) {
                    parent.storage.remove(key);
                }
                parent.storage.putAll(tempChanges);
            }
        }
    }
}
