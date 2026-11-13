package com.joutio.profile.domain.model;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pays du profil : code ISO 3166-1 alpha-2 strict (décision du 2026-11-13, {@code editer-profil.md} § 4).
 *
 * <p>Les profils antérieurs ont pu enregistrer un nom libre (« France », « Belgique »). Le profil étant
 * event-sourcé, ces valeurs restent dans les anciens {@code ProfileCountryUpdatedEvent} : {@link #normalize(String)}
 * les convertit à la relecture (agrégat et projections), un nom inconnu devenant {@code null}. Contrat partagé : les
 * consommateurs de l'événement (leaderboard) l'appliquent aussi.</p>
 */
public final class CountryCodes {

    /** Codes ISO 3166-1 alpha-2 connus du JDK. */
    public static final Set<String> ISO = Set.of(Locale.getISOCountries());

    private static final Locale[] NAME_LOCALES = {Locale.FRENCH, Locale.ENGLISH};

    private static final Map<String, String> BY_NAME = buildNames();

    private CountryCodes() {
    }

    /** Vrai pour un code ISO alpha-2 en majuscules. */
    public static boolean isValid(String code) {
        return code != null && ISO.contains(code);
    }

    /**
     * Code ISO d'une valeur historique : code (toute casse) ou nom français / anglais (sans tenir compte des accents ni
     * de la casse). {@code null} si la valeur est vide ou inconnue.
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (ISO.contains(upper)) {
            return upper;
        }
        return BY_NAME.get(fold(trimmed));
    }

    private static Map<String, String> buildNames() {
        Map<String, String> names = new HashMap<>();
        for (String code : ISO) {
            Locale region = Locale.of("", code);
            for (Locale language : NAME_LOCALES) {
                names.putIfAbsent(fold(region.getDisplayCountry(language)), code);
            }
        }
        // Graphies courantes absentes des libellés du JDK.
        names.put(fold("Royaume Uni"), "GB");
        names.put(fold("UK"), "GB");
        names.put(fold("USA"), "US");
        names.put(fold("Etats Unis"), "US");
        names.put(fold("Cote d'Ivoire"), "CI");
        return Map.copyOf(names);
    }

    private static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('’', '\'')
                .replace('-', ' ')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
