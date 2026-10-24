package org.schabi.newpipe.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.AudioTrackType;

import java.util.List;
import java.util.Locale;

public class DownloadAudioTrackSelectionTest {

    private AudioStream createAudioStream(final String trackId,
                                          final Locale locale,
                                          final AudioTrackType trackType,
                                          final int bitrate) {
        return new AudioStream.Builder()
                .setId(trackId)
                .setContent("https://example.com/audio_" + trackId, false)
                .setAudioTrackId(trackId)
                .setAudioTrackName(locale != null ? locale.getDisplayName() : trackId)
                .setAudioTrackType(trackType)
                .setAudioLocale(locale)
                .setMediaFormat(MediaFormat.M4A)
                .setAverageBitrate(bitrate)
                .build();
    }

    /**
     * Criterion 1: Bengali original selected when original is Bengali.
     */
    @Test
    public void testBengaliOriginalSelectedWhenOriginalIsBengali() {
        final AudioStream englishDubbed = createAudioStream("en_dub",
                Locale.ENGLISH, AudioTrackType.DUBBED, 128000);
        final AudioStream bengaliOriginal = createAudioStream("bn_orig",
                new Locale("bn"), AudioTrackType.ORIGINAL, 128000);
        final AudioStream hindiDubbed = createAudioStream("hi_dub",
                new Locale("hi"), AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(englishDubbed),
                List.of(bengaliOriginal),
                List.of(hindiDubbed)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(1, selectedIndex);
        assertEquals("bn_orig", groups.get(selectedIndex).get(0).getAudioTrackId());
    }

    /**
     * Criterion 2: Hindi original selected when original is Hindi, even if Bengali
     * dubbing is present.
     */
    @Test
    public void testHindiOriginalSelectedWhenOriginalIsHindiEvenWithBengaliDub() {
        final AudioStream bengaliDubbed = createAudioStream("bn_dub",
                new Locale("bn"), AudioTrackType.DUBBED, 128000);
        final AudioStream hindiOriginal = createAudioStream("hi_orig",
                new Locale("hi"), AudioTrackType.ORIGINAL, 128000);
        final AudioStream englishDubbed = createAudioStream("en_dub",
                Locale.ENGLISH, AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(bengaliDubbed),
                List.of(hindiOriginal),
                List.of(englishDubbed)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(1, selectedIndex);
        assertEquals("hi_orig", groups.get(selectedIndex).get(0).getAudioTrackId());
    }

    /**
     * Criterion 3: Bengali dubbing selected when original is English (or other third language)
     * and Bengali dubbing is present.
     */
    @Test
    public void testBengaliDubbingSelectedWhenOriginalIsOtherLanguage() {
        final AudioStream englishOriginal = createAudioStream("en_orig",
                Locale.ENGLISH, AudioTrackType.ORIGINAL, 128000);
        final AudioStream hindiDubbed = createAudioStream("hi_dub",
                new Locale("hi"), AudioTrackType.DUBBED, 128000);
        final AudioStream bengaliDubbed = createAudioStream("bn_dub",
                new Locale("bn"), AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(englishOriginal),
                List.of(hindiDubbed),
                List.of(bengaliDubbed)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(2, selectedIndex);
        assertEquals("bn_dub", groups.get(selectedIndex).get(0).getAudioTrackId());
    }

    /**
     * Criterion 4: Hindi selected when Bengali is unavailable and Hindi is available.
     */
    @Test
    public void testHindiSelectedWhenBengaliUnavailableAndHindiAvailable() {
        final AudioStream englishOriginal = createAudioStream("en_orig",
                Locale.ENGLISH, AudioTrackType.ORIGINAL, 128000);
        final AudioStream frenchDubbed = createAudioStream("fr_dub",
                Locale.FRENCH, AudioTrackType.DUBBED, 128000);
        final AudioStream hindiDubbed = createAudioStream("hi_dub",
                new Locale("hi"), AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(englishOriginal),
                List.of(frenchDubbed),
                List.of(hindiDubbed)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(2, selectedIndex);
        assertEquals("hi_dub", groups.get(selectedIndex).get(0).getAudioTrackId());
    }

    /**
     * Criterion 5: Original audio selected when neither Bengali nor Hindi is available
     * and original can be reliably identified.
     */
    @Test
    public void testOriginalAudioSelectedWhenNeitherBengaliNorHindiAvailable() {
        final AudioStream japaneseOriginal = createAudioStream("ja_orig",
                Locale.JAPANESE, AudioTrackType.ORIGINAL, 128000);
        final AudioStream englishDubbed = createAudioStream("en_dub",
                Locale.ENGLISH, AudioTrackType.DUBBED, 128000);
        final AudioStream spanishDubbed = createAudioStream("es_dub",
                new Locale("es"), AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(englishDubbed),
                List.of(japaneseOriginal),
                List.of(spanishDubbed)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(1, selectedIndex);
        assertEquals("ja_orig", groups.get(selectedIndex).get(0).getAudioTrackId());
    }

    /**
     * Criterion 6: Existing default selection preserved when neither Bengali nor Hindi is
     * available and original cannot be reliably identified.
     */
    @Test
    public void testDefaultSelectionPreservedWhenOriginalCannotBeReliablyIdentified() {
        final AudioStream spanishDubbed = createAudioStream("es_dub",
                new Locale("es"), AudioTrackType.DUBBED, 128000);
        final AudioStream frenchDubbed = createAudioStream("fr_dub",
                Locale.FRENCH, AudioTrackType.DUBBED, 128000);
        final AudioStream germanDubbed = createAudioStream("de_dub",
                Locale.GERMAN, AudioTrackType.DUBBED, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(spanishDubbed),
                List.of(frenchDubbed),
                List.of(germanDubbed)
        );

        final int downloadSelectedIndex =
                ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        final int defaultSelectedIndex = ListHelper.getDefaultAudioTrackGroup(null, groups);

        assertEquals(defaultSelectedIndex, downloadSelectedIndex);
    }

    /**
     * Criterion 7: Normal video/format selection and playback track selection remain unaffected.
     */
    @Test
    public void testNormalPlaybackAndFormatSelectionUnaffected() {
        final AudioStream stream1 = createAudioStream("stream_low",
                new Locale("bn"), AudioTrackType.ORIGINAL, 64000);
        final AudioStream stream2 = createAudioStream("stream_high",
                new Locale("bn"), AudioTrackType.ORIGINAL, 192000);

        final List<AudioStream> streams = List.of(stream1, stream2);

        // getAudioFormatIndex matches by track ID or defaults to highest quality format
        final int indexById = ListHelper.getAudioFormatIndex(null, streams, "stream_low");
        assertEquals(0, indexById);

        // getDefaultAudioFormat prioritizes highest bitrate / quality ranking
        final int defaultFormatIndex = ListHelper.getDefaultAudioFormat(null, streams);
        assertEquals(1, defaultFormatIndex);

        // Verify language check helpers strictly identify standard ISO codes
        assertTrue(ListHelper.isBengaliAudioTrack(stream1));
        assertFalse(ListHelper.isHindiAudioTrack(stream1));
        assertTrue(ListHelper.isOriginalAudioTrack(stream1));

        final AudioStream nonOriginal = createAudioStream("stream_unknown",
                new Locale("bn"), null, 128000);
        assertFalse("Untagged track should not be classified as original",
                ListHelper.isOriginalAudioTrack(nonOriginal));
    }

    /**
     * Tie-breaking test: When multiple tracks of the same preferred language are present,
     * existing comparator logic breaks the tie.
     */
    @Test
    public void testTieBreakingMultipleSameLanguageTracks() {
        final AudioStream bengaliDescriptive = createAudioStream("bn_desc",
                new Locale("bn"), AudioTrackType.DESCRIPTIVE, 128000);
        final AudioStream bengaliOriginal = createAudioStream("bn_orig",
                new Locale("bn"), AudioTrackType.ORIGINAL, 128000);

        final List<List<AudioStream>> groups = List.of(
                List.of(bengaliDescriptive),
                List.of(bengaliOriginal)
        );

        final int selectedIndex = ListHelper.getDefaultDownloadAudioTrackGroup(null, groups);
        assertEquals(1, selectedIndex);
        assertEquals("bn_orig", groups.get(selectedIndex).get(0).getAudioTrackId());
    }
}
