package com.joutio.profile.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Pseudonyme initial d'un joueur (ex. {@code SwiftFox42}), **déterministe** : dérivé de l'identifiant du joueur et du
 * numéro de tentative, il est le même à chaque rejeu de la saga ou de la commande qui le propose.
 */
public final class PseudonymGenerator {

    private static final String[] ADJECTIVES = {
            "Swift", "Brave", "Calm", "Bold", "Bright",
            "Cool", "Dark", "Epic", "Fast", "Fierce",
            "Glad", "Grand", "Happy", "Iron", "Jolly",
            "Kind", "Lucky", "Mega", "Noble", "Proud",
            "Quick", "Rare", "Sharp", "Sly", "Smart",
            "Soft", "Stern", "Super", "True", "Wild"
    };

    private static final String[] ANIMALS = {
            "Bear", "Cat", "Crow", "Deer", "Eagle",
            "Falcon", "Fox", "Hawk", "Jaguar", "Lion",
            "Lynx", "Otter", "Owl", "Panda", "Panther",
            "Raven", "Shark", "Tiger", "Wolf", "Zebra"
    };

    private PseudonymGenerator() {
        //
    }

    /**
     * Pseudonyme du type "SwiftFox42" pour ce joueur et cette tentative (0 pour la première proposition).
     */
    public static String generate(String userId, int attempt) {
        SplittableRandom random = new SplittableRandom(
                UUID.nameUUIDFromBytes((userId + ":" + attempt).getBytes(StandardCharsets.UTF_8)).getMostSignificantBits());
        String adj    = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String animal = ANIMALS[random.nextInt(ANIMALS.length)];
        return adj + animal + (10 + random.nextInt(90));
    }
}
