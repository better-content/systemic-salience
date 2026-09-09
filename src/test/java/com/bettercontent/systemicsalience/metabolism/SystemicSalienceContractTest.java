package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemicSalienceContractTest {
    @Test
    void dietKeepsItsNativeScreen() throws IOException {
        Path clientRoot = Path.of("src/main/java/com/bettercontent/systemicsalience/client");
        String stateEvents = Files.readString(clientRoot.resolve("ClientStateEvents.java"));

        assertFalse(Files.exists(clientRoot.resolve("SystemicDietScreen.java")));
        assertFalse(Files.exists(clientRoot.resolve("DietScreenEvents.java")));
        assertFalse(stateEvents.contains("ScreenEvent.Opening"));
        assertFalse(stateEvents.contains("setNewScreen"));
    }

    @Test
    void configuredThresholdsPreservePreparationOrdering() {
        assertTrue(SalienceConfig.ORDINARY_THRESHOLD.getDefault() < SalienceConfig.PREPARED_THRESHOLD.getDefault());
        assertTrue(SalienceConfig.PREPARED_THRESHOLD.getDefault() < SalienceConfig.FEAST_THRESHOLD.getDefault());
        assertTrue(SalienceConfig.FEAST_THRESHOLD.getDefault() < 1.0);
    }

    @Test
    void readinessStripContainsExactlyEightSquareCells() throws IOException {
        try (var stream = getClass().getResourceAsStream("/assets/systemic_salience/textures/gui/nutrition_states.png")) {
            assertTrue(stream != null);
            var image = ImageIO.read(stream);
            assertTrue(image != null);
            assertTrue(image.getHeight() == 18);
            assertTrue(image.getWidth() == image.getHeight() * 8);
        }
    }
}
