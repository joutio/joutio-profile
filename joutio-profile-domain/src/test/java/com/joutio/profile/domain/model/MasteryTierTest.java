package com.joutio.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasteryTierTest {

    @Test
    void tierFor_followsTheExplicitThresholds() {
        assertEquals(MasteryTier.FER, MasteryTier.tierFor(0));
        assertEquals(MasteryTier.FER, MasteryTier.tierFor(499));
        assertEquals(MasteryTier.BRONZE, MasteryTier.tierFor(500));
        assertEquals(MasteryTier.BRONZE, MasteryTier.tierFor(1_499));
        assertEquals(MasteryTier.ARGENT, MasteryTier.tierFor(1_500));
        assertEquals(MasteryTier.OR, MasteryTier.tierFor(4_000));
        assertEquals(MasteryTier.PLATINE, MasteryTier.tierFor(9_000));
        assertEquals(MasteryTier.EMERAUDE, MasteryTier.tierFor(21_000));
        assertEquals(MasteryTier.DIAMANT, MasteryTier.tierFor(45_000));
        assertEquals(MasteryTier.DIAMANT, MasteryTier.tierFor(1_000_000));
    }

    @Test
    void thresholds_areStrictlyIncreasing() {
        MasteryTier previous = MasteryTier.FER;
        for (MasteryTier tier : MasteryTier.values()) {
            assertTrue(tier.xpThreshold() >= previous.xpThreshold(),
                    "seuil croissant pour " + tier);
            previous = tier;
        }
        assertEquals(0, MasteryTier.FER.xpThreshold());
    }

    @Test
    void tiersAreTheRarityLadder() {
        assertEquals("Fer", MasteryTier.FER.label());
        assertEquals("Bronze", MasteryTier.BRONZE.label());
        assertEquals("Argent", MasteryTier.ARGENT.label());
        assertEquals("Or", MasteryTier.OR.label());
        assertEquals("Platine", MasteryTier.PLATINE.label());
        assertEquals("Émeraude", MasteryTier.EMERAUDE.label());
        assertEquals("Diamant", MasteryTier.DIAMANT.label());
    }

    @Test
    void next_isChainedAndEndsAtDiamant() {
        assertEquals(MasteryTier.BRONZE, MasteryTier.FER.next());
        assertNull(MasteryTier.DIAMANT.next());
        assertTrue(MasteryTier.DIAMANT.isLast());
        assertFalse(MasteryTier.EMERAUDE.isLast());
    }

    @Test
    void progressAndRemaining_bracketTheCurrentTier() {
        // Entre Or (4 000) et Platine (9 000).
        assertEquals(0, MasteryTier.OR.progressPercent(4_000));
        assertEquals(50, MasteryTier.OR.progressPercent(6_500));
        assertEquals(100, MasteryTier.OR.progressPercent(9_000));
        assertEquals(5_000, MasteryTier.OR.remainingXp(4_000));
        assertEquals(0, MasteryTier.OR.remainingXp(9_000));
    }

    @Test
    void lastTier_isAlwaysComplete() {
        assertEquals(100, MasteryTier.DIAMANT.progressPercent(45_000));
        assertEquals(0, MasteryTier.DIAMANT.remainingXp(45_000));
    }

    @Test
    void fromKey_readsThePersistedTierIgnoringCase() {
        assertEquals(Optional.of(MasteryTier.OR), MasteryTier.fromKey("OR"));
        assertEquals(Optional.of(MasteryTier.OR), MasteryTier.fromKey("or"));
        assertEquals(Optional.empty(), MasteryTier.fromKey("GRAND_MAITRE"));
        assertEquals(Optional.empty(), MasteryTier.fromKey("CONFIRME"));
        assertEquals(Optional.empty(), MasteryTier.fromKey(null));
        assertEquals(Optional.empty(), MasteryTier.fromKey("  "));
    }
}
