package com.joutio.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PseudonymGeneratorTest {

    @Test
    void sameUserAndAttempt_givesTheSamePseudonym() {
        assertEquals(PseudonymGenerator.generate("user-1", 0), PseudonymGenerator.generate("user-1", 0));
    }

    @Test
    void anotherAttempt_proposesAnotherPseudonym() {
        assertNotEquals(PseudonymGenerator.generate("user-1", 0), PseudonymGenerator.generate("user-1", 1));
    }

    @Test
    void pseudonym_followsTheAdjectiveAnimalNumberShape() {
        String pseudonym = PseudonymGenerator.generate("user-2", 3);
        assertTrue(pseudonym.matches("[A-Z][a-z]+[A-Z][a-z]+[1-9][0-9]"), pseudonym);
        assertTrue(pseudonym.length() <= ProfileRules.MAX_PSEUDONYM_CHARS, pseudonym);
    }
}
