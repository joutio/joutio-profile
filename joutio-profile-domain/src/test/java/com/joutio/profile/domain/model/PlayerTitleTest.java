package com.joutio.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTitleTest {

    @Test
    void key_roundTripsThroughParse() {
        PlayerTitle title = new PlayerTitle("topic-1", MasteryTier.OR);

        assertEquals("topic-1:OR", title.key());
        assertEquals(Optional.of(title), PlayerTitle.parse(title.key()));
    }

    @Test
    void parse_readsTheLastSeparator() {
        // Les identifiants de sujet n'ont pas de séparateur, mais on ne veut jamais couper trop tôt.
        assertEquals(
                Optional.of(new PlayerTitle("a:b", MasteryTier.PLATINE)),
                PlayerTitle.parse("a:b:PLATINE"));
    }

    @Test
    void parse_rejectsMalformedOrUnknownKeys() {
        assertEquals(Optional.empty(), PlayerTitle.parse(null));
        assertEquals(Optional.empty(), PlayerTitle.parse("   "));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1"));
        assertEquals(Optional.empty(), PlayerTitle.parse(":OR"));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1:"));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1:GRAND_MAITRE"));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1:CONFIRME"));
    }

    @Test
    void label_appendsTheTopicNameAndFallsBackToTheTierAlone() {
        PlayerTitle title = new PlayerTitle("topic-1", MasteryTier.OR);

        assertEquals("Or · Histoire", title.label("Histoire"));
        assertEquals("Or", title.label(null));
        assertEquals("Or", title.label("  "));
    }

    @Test
    void constructor_rejectsIncompleteTitles() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerTitle("  ", MasteryTier.OR));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerTitle("topic-1", null));
    }

    @Test
    void requireValid_failsOnMalformedKeys() {
        assertEquals("topic-1:DIAMANT", PlayerTitle.requireValid("topic-1:DIAMANT").key());
        assertThrows(IllegalArgumentException.class, () -> PlayerTitle.requireValid("topic-1"));
    }

    @Test
    void maxKeyLength_coversTheWidestTitle() {
        int longestTier = java.util.Arrays.stream(MasteryTier.values())
                .mapToInt(tier -> tier.name().length())
                .max()
                .orElse(0);

        assertTrue(PlayerTitle.maxKeyLength() >= 36 + 1 + longestTier);
    }
}
