package com.joutio.profile.domain.model;

import com.joutio.microservice.core.domain.model.i18n.Language;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Règles de validation du domaine Profile (limites de longueur).
 */
public final class ProfileRules {

    /** Longueur maximale historique (pseudonymes antérieurs au 2026-11-12, conservés tels quels). */
    public static final int MAX_PSEUDONYM_LENGTH = 100;

    public static final int MIN_PSEUDONYM_CHARS = 3;
    public static final int MAX_PSEUDONYM_CHARS = 20;

    /**
     * Pseudonyme choisi par un joueur (2026-11-12) : lettres ASCII, chiffres et tiret bas, 3 à 20
     * caractères, commençant par une lettre. Unique sans tenir compte de la casse.
     */
    public static final Pattern PSEUDONYM_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{2,19}$");

    public static boolean isValidPseudonym(String pseudonym) {
        return pseudonym != null && PSEUDONYM_PATTERN.matcher(pseudonym).matches();
    }

    /** Clé d'unicité : insensible à la casse et aux espaces en bord. */
    public static String pseudonymKey(String pseudonym) {
        return pseudonym == null ? null : pseudonym.trim().toLowerCase(Locale.ROOT);
    }

    public static final int MAX_BIO_LENGTH = 300;

    /** Pays : code ISO 3166-1 alpha-2, voir {@link CountryCodes}. */
    public static final int COUNTRY_CODE_LENGTH = 2;

    /** Sérialisation JSON des options d'avatar DiceBear (styles micah, lorelei, notionists). */
    public static final int MAX_AVATAR_OPTIONS_LENGTH = 2000;

    /** URL externe de la bannière de profil (image panoramique, http/https). */
    public static final int MAX_BANNER_URL_LENGTH = 1024;

    /** Clé du titre porté (`topicId:TIER`, cf. {@link PlayerTitle}). */
    public static final int MAX_EQUIPPED_TITLE_LENGTH = 128;

    /** Nombre maximum de centres d'intérêt persistés (le premier est l'univers principal). */
    public static final int MAX_INTERESTS = 3;

    /** Longueur maximale d'un code de catégorie (ex. {@code FOOD_AND_DRINK}). */
    public static final int MAX_INTEREST_CODE_LENGTH = 32;

    /**
     * Code de catégorie accepté : le domaine profile ne connaît pas l'enum thème (frontière de
     * bounded context), il valide seulement la forme canonique ; le BFF valide l'appartenance à
     * {@code TopicCategory}.
     */
    public static final Pattern INTEREST_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z_]*$");

    /** Langue par défaut d'un profil (appliquée à la création). */
    public static final Language DEFAULT_LANGUAGE = Language.FR;

    /** Préférence de jeu par défaut : entraînement automatique si aucun adversaire n'est trouvé. */
    public static final boolean DEFAULT_AUTO_TRAIN_ON_TIMEOUT = true;

    private ProfileRules() {
    }
}
