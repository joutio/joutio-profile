package com.joutio.profile.domain.model;

import java.util.Optional;

/**
 * Titre **porté** par un joueur : un titre thématique désigné par une clé stable
 * {@code topicId:TIER} (ex. {@code 8f1c…:OR}).
 *
 * <p>On persiste la <b>clé</b>, jamais le libellé : le libellé se compose à l'affichage
 * ({@link #label(String)}) à partir du nom du sujet — il suit donc le renommage du sujet, la
 * langue du lecteur et l'ajout ultérieur d'un gentilé éditorial.</p>
 *
 * <p>L'appartenance du titre au joueur (palier réellement atteint dans ce sujet) est vérifiée par
 * l'appelant, qui prépare la commande (cf. `best-practices/.backend/command-preparation.md`).</p>
 */
public record PlayerTitle(String topicId, MasteryTier tier) {

    private static final String SEPARATOR = ":";

    public PlayerTitle {
        if (topicId == null || topicId.isBlank()) {
            throw new IllegalArgumentException("topicId is required");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier is required");
        }
    }

    /** Clé persistée du titre ({@code topicId:TIER}). */
    public String key() {
        return topicId + SEPARATOR + tier.name();
    }

    /**
     * Libellé affiché du titre : palier + nom du sujet (« Or · Histoire »), ou le palier seul si
     * le nom du sujet est inconnu (sujet supprimé, nom non résolu).
     */
    public String label(String topicName) {
        if (topicName == null || topicName.isBlank()) {
            return tier.label();
        }

        return tier.label() + " · " + topicName;
    }

    /** Longueur maximale d'une clé de titre persistée. */
    public static int maxKeyLength() {
        return 128;
    }

    /** Analyse une clé persistée ; vide si la clé est absente ou mal formée. */
    public static Optional<PlayerTitle> parse(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        int separator = key.lastIndexOf(SEPARATOR);
        if (separator <= 0 || separator == key.length() - 1) {
            return Optional.empty();
        }

        String topicId = key.substring(0, separator).trim();
        if (topicId.isEmpty()) {
            return Optional.empty();
        }

        return MasteryTier.fromKey(key.substring(separator + 1))
                .map(tier -> new PlayerTitle(topicId, tier));
    }

    /** Analyse une clé persistée en rejetant les clés absentes ou mal formées. */
    public static PlayerTitle requireValid(String key) {
        return parse(key).orElseThrow(() -> new IllegalArgumentException("invalid title key: " + key));
    }
}
