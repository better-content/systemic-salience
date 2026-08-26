package com.bettercontent.systemicsalience.presentation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class IdentityAssetContractTest {
    private static final String BADGE_HASH = "b59717a5da26f633cd120f09b750c15875577ec9c74a8c7838c32aea8ee5eeed";
    private static final Map<String, String> MOTIF_HASHES = Map.of(
            "impact", "73f2a5256be3a4f0c85ab65aeaa6d139ac43103e8e8d24003b9a4119ea2ab51a",
            "tempo", "e77950b8b2e29f7425af7ae1673ed12f325de741af149b1b308bb6af64d44665",
            "work", "aeabd6b7a437211ff683fbb452e5671b84d106d8daab63c7333869e3f4cf8a3e",
            "mobility", "ba1f67679ec62909c6bfb16532efed80e736c258ad9582037df423c04157a570",
            "endurance", "6409e26c4f80c0586dc220542cd4a18d44089a3d7bda8ea79af94b5aa5d7e64d",
            "robustness", "e914400a5a03a898528a1d6674398e20fca5d5778ea4c8f617df23958d780311",
            "renewal", "6fe2b992a4ffd3bead0992d53bc6885ad86e24e0fd71e84f67b6f812ca061a21",
            "control", "971dee8ac79c5961407651e16ac73821f8864923738d8b6c8d637e001e07d457"
    );

    @Test
    void canonicalBadgeAndFontCoverEightIdentities() throws Exception {
        byte[] bytes = resource("/assets/systemic_salience/textures/gui/aspect_badges.png").readAllBytes();
        assertEquals(BADGE_HASH, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        assertEquals(144, image.getWidth()); assertEquals(18, image.getHeight());
        String font = new String(resource("/assets/systemic_salience/font/aspects.json").readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(font.contains(""));
    }

    @Test
    void everyIdentityHasARegisteredOggMotif() throws Exception {
        String sounds = new String(resource("/assets/systemic_salience/sounds.json").readAllBytes(), StandardCharsets.UTF_8);
        for (AspectIdentity aspect : AspectIdentity.values()) {
            String id = aspect.name().toLowerCase();
            assertTrue(sounds.contains("aspect." + id));
            byte[] bytes = resource("/assets/systemic_salience/sounds/aspect/" + id + ".ogg").readAllBytes();
            assertTrue(bytes.length > 4000, id);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, java.util.Arrays.copyOf(bytes, 4));
            assertEquals(MOTIF_HASHES.get(id),
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), id);
        }
    }

    private InputStream resource(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        return stream;
    }
}
