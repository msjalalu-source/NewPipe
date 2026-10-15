package org.schabi.newpipe.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchKeywordFilterTest {

    @Test
    public void testPredefinedKeywordsMatching() {
        // Aashiq Banaya
        assertTrue(SearchKeywordFilter.isQueryBlocked("Aashiq Banaya", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("aashiq banaya", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("Aashiq Banaya remix", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("Best of aashiq banaya 2024", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("aashiq   banaya song", null));

        // adult
        assertTrue(SearchKeywordFilter.isQueryBlocked("adult", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("ADULT", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("adult movies", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("young adult novel", null));

        // porn
        assertTrue(SearchKeywordFilter.isQueryBlocked("porn", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("free porn video", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("PORN", null));

        // sex
        assertTrue(SearchKeywordFilter.isQueryBlocked("sex", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("safe sex tips", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("education on sex", null));

        // xxx
        assertTrue(SearchKeywordFilter.isQueryBlocked("xxx", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("XXX", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("action xxx movie", null));

        // 18+
        assertTrue(SearchKeywordFilter.isQueryBlocked("18+", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("18+ content", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("rated 18+", null));

        // intimate
        assertTrue(SearchKeywordFilter.isQueryBlocked("intimate", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("intimate acoustic session", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("very intimate", null));

        // kiss
        assertTrue(SearchKeywordFilter.isQueryBlocked("kiss", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("first kiss scene", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("blow a kiss", null));

        // hot
        assertTrue(SearchKeywordFilter.isQueryBlocked("hot", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("HOT", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("hot weather report", null));
        assertTrue(SearchKeywordFilter.isQueryBlocked("super hot summer", null));
    }

    @Test
    public void testAccidentalSubstringNonMatching() {
        // "hot" substrings should NOT trigger blocking
        assertFalse(SearchKeywordFilter.isQueryBlocked("shot", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("hotel", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("photography", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("photographer", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("snapshot", null));

        // "sex" substrings should NOT trigger blocking
        assertFalse(SearchKeywordFilter.isQueryBlocked("Essex", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("Sussex", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("Middlesex", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("assessment", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("unisex", null));

        // "adult" substrings should NOT trigger blocking
        assertFalse(SearchKeywordFilter.isQueryBlocked("adulthood", null));

        // "xxx" boundary check
        assertFalse(SearchKeywordFilter.isQueryBlocked("xxxx", null));

        // "18+" boundary check
        assertFalse(SearchKeywordFilter.isQueryBlocked("18", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("180+", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("18+1", null));

        // Single words of phrase should NOT trigger blocking
        assertFalse(SearchKeywordFilter.isQueryBlocked("Aashiq", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("Banaya", null));
    }

    @Test
    public void testExactVideoIdMatching() {
        final List<String> videoIds = Arrays.asList(
                "0bAVd9jJE2Q",
                "19E65tOn3tI",
                "kbObKrBoIjI",
                "3QCgD4R4Ly8",
                "RlbGLrMuZ5I",
                "7vBsI0Hv5L0"
        );

        for (final String videoId : videoIds) {
            // Direct search for the ID
            assertTrue(SearchKeywordFilter.isQueryBlocked(videoId, null));

            // YouTube URL formats containing the exact ID
            assertTrue(SearchKeywordFilter.isQueryBlocked(
                    "https://www.youtube.com/watch?v=" + videoId, null));
            assertTrue(SearchKeywordFilter.isQueryBlocked(
                    "https://youtu.be/" + videoId, null));
            assertTrue(SearchKeywordFilter.isQueryBlocked(
                    "watch " + videoId + " online", null));

            // Arbitrary substrings embedding the video ID must NOT match
            assertFalse(SearchKeywordFilter.isQueryBlocked("prefix" + videoId, null));
            assertFalse(SearchKeywordFilter.isQueryBlocked(videoId + "suffix", null));
            assertFalse(SearchKeywordFilter.isQueryBlocked("part_" + videoId, null));
        }
    }

    @Test
    public void testNormalUnrelatedSearchesAllowed() {
        assertFalse(SearchKeywordFilter.isQueryBlocked("android development tutorial", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("newpipe open source music", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("kotlin coroutines guide", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("cooking authentic pasta", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("space telescope deep field", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("lofi hip hop study music", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked("", null));
        assertFalse(SearchKeywordFilter.isQueryBlocked(null, null));
    }

    @Test
    public void testCustomKeywordsMatching() {
        final List<String> customList = Arrays.asList("gambling", "deep fake");

        // Matches custom
        assertTrue(SearchKeywordFilter.isQueryBlocked("online gambling tutorial", customList));
        assertTrue(SearchKeywordFilter.isQueryBlocked("deep fake news", customList));
        assertTrue(SearchKeywordFilter.isQueryBlocked("deep   fake", customList));

        // Substrings do not match
        assertFalse(SearchKeywordFilter.isQueryBlocked("gambler", customList));
        assertFalse(SearchKeywordFilter.isQueryBlocked("deep faker", customList));

        // Unrelated does not match
        assertFalse(SearchKeywordFilter.isQueryBlocked("science experiment", customList));
    }

    @Test
    public void testKeywordNormalization() {
        assertEquals("hello world",
                SearchKeywordFilter.normalizeKeyword("   hello    world   "));
        assertEquals("abc", SearchKeywordFilter.normalizeKeyword("abc"));
        assertEquals("", SearchKeywordFilter.normalizeKeyword(null));
        assertEquals("", SearchKeywordFilter.normalizeKeyword("    "));
    }

    @Test
    public void testPredefinedEntriesProtection() {
        assertTrue(SearchKeywordFilter.isPredefined("hot"));
        assertTrue(SearchKeywordFilter.isPredefined("HOT"));
        assertTrue(SearchKeywordFilter.isPredefined("Aashiq Banaya"));
        assertTrue(SearchKeywordFilter.isPredefined("0bAVd9jJE2Q"));
        assertTrue(SearchKeywordFilter.isPredefined("18+"));

        assertFalse(SearchKeywordFilter.isPredefined("custom_kw"));
        assertFalse(SearchKeywordFilter.isPredefined("kotlin"));
        assertFalse(SearchKeywordFilter.isPredefined(null));
    }

    @Test
    public void testExtractorHelperRejection() {
        // ExtractorHelper searchFor must reject blocked searches with Single.error
        ExtractorHelper.searchFor(1, "hot", Collections.emptyList(), "")
                .test()
                .assertError(IllegalArgumentException.class);

        ExtractorHelper.searchFor(1, "0bAVd9jJE2Q", Collections.emptyList(), "")
                .test()
                .assertError(IllegalArgumentException.class);

        // ExtractorHelper suggestionsFor must return empty list for blocked searches
        ExtractorHelper.suggestionsFor(1, "adult")
                .test()
                .assertValue(Collections.emptyList());
    }
}
