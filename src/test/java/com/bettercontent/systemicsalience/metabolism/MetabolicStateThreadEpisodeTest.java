package com.bettercontent.systemicsalience.metabolism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MetabolicStateThreadEpisodeTest {
    @Test
    void nutritionEpisodeTokenSurvivesStatePersistence() {
        MetabolicState state = new MetabolicState();
        state.nutritionThreadToken = "player:nutrition:120";

        assertEquals(state.nutritionThreadToken, MetabolicState.load(state.save()).nutritionThreadToken);
    }

    @Test
    void invalidNutritionEpisodeTokenIsNotLoaded() {
        MetabolicState state = new MetabolicState();
        state.nutritionThreadToken = "bad token with spaces";

        assertEquals("", MetabolicState.load(state.save()).nutritionThreadToken);
    }
}
