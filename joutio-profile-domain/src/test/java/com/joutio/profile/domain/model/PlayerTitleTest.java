package com.joutio.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
