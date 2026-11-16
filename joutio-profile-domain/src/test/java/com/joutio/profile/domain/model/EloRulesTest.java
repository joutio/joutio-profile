package com.joutio.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRulesTest {

    @Test
    void initialScale_startsAtTwoHundredWithAFixedK() {
        assertEquals(200, EloRules.INITIAL_RATING);
        assertEquals(20, EloRules.K_FACTOR);
    }

    @Test
    void equalRatings_winGivesHalfTheK() {
        assertEquals(210, EloRules.newRating(200, 200, 1.0));
        assertEquals(190, EloRules.newRating(200, 200, 0.0));
        assertEquals(200, EloRules.newRating(200, 200, 0.5));
    }

    @Test
    void expectedScore_favoursTheHigherRatedPlayer() {
        assertTrue(EloRules.expectedScore(1000, 800) > 0.7);
        assertTrue(EloRules.expectedScore(800, 1000) < 0.3);
    }

    @Test
    void upset_againstStrongerPlayerAwardsMore() {
        int upset = EloRules.newRating(200, 1000, 1.0);
        int expectedWin = EloRules.newRating(1000, 200, 1.0);

        assertTrue(upset - 200 > expectedWin - 1000);
        assertTrue(upset > 210);
    }

    @Test
    void ratingNeverGoesBelowZero() {
        assertEquals(0, EloRules.newRating(10, 0, 0.0));
    }
}
