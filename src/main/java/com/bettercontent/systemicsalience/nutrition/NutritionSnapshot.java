package com.bettercontent.systemicsalience.nutrition;

import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;

public record NutritionSnapshot(float fruits, float grains, float proteins, float vegetables) {
    public static final NutritionSnapshot EMPTY = new NutritionSnapshot(0.0f, 0.0f, 0.0f, 0.0f);

    public double effective(Group group, MetabolicState state) {
        return MetabolicMath.effectiveNutrient(actual(group), state.sugar, state.debt);
    }

    public float actual(Group group) {
        return switch (group) {
            case FRUITS -> fruits;
            case GRAINS -> grains;
            case PROTEINS -> proteins;
            case VEGETABLES -> vegetables;
        };
    }

    public enum Group {
        FRUITS("fruits"),
        GRAINS("grains"),
        PROTEINS("proteins"),
        VEGETABLES("vegetables");

        public final String id;

        Group(String id) {
            this.id = id;
        }
    }
}
