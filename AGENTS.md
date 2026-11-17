# AGENTS.md — joutio-profile

Profil public des joueurs (source d'existence et du pseudonyme), **présence** en ligne et
**progression** (XP par sujet, Elo, badges, titres, récompenses). Package `com.joutio.profile`,
base `joutio_profile`, port local 8088. Service headless : le BFF l'interroge par le bus.

Lire : `../../AGENTS.md`, [`docs/backend/`](../../docs/backend/hexagonal.md), règles produit dans
[`docs/produit/engagement.md`](../../docs/produit/engagement.md), exposition dans
[`docs/api/joueurs.md`](../../docs/api/joueurs.md).

## Agrégats

| Agrégat (identifiant) | Porte |
|---|---|
| `ProfileAggregate` (`userId`) | `pseudonym`, `bio`, `country` (ISO 3166-1 alpha-2), `avatarOptions`, `bannerUrl`, `language`, `interests` (0–3 codes ordonnés), `equippedTitle`, `autoTrainOnTimeout`, fin d'onboarding |
| `PlayerProgressAggregate` (`progress:<userId>`) | XP par sujet, Elo, `MasteryTierReachedEvent` par palier franchi |
| `BadgeAggregate` (`badges:<userId>`) | compteurs par métrique et badges acquis |
| `PlayerRewardAggregate` (`rewards:<userId>`) | récompenses définitives (`MICAH_AVATAR`) |

Les identifiants sont préfixés pour ne pas se heurter à `ProfileAggregate`. Les changements de
profil se font **champ par champ** (une commande et un événement par champ, no-op si la valeur ne
change pas), par le propriétaire.

## Flux

- **Création** : `CreateProfileSaga` sur `UserRegisteredEvent` (identity). `CreateProfileCommand`
  est idempotente (`CREATE_IF_MISSING`), donc la saga ne lit aucune projection.
  - Pseudonyme initial : `PseudonymGenerator`.
  - Unicité insensible à la casse : table `pseudonym_claim` ; règles 3–20 caractères
    `[A-Za-z][A-Za-z0-9_]*`.
  - `ProfileDataSeeder` garantit le profil du compte système.
- **Fin de partie** : `AwardProgressSaga` sur `GameEndedEvent` (game) envoie un `AwardXpCommand`
  par joueur humain, idempotent par `gameId`.
  - XP : `ProgressionRules.xpFor`, **0 pour un entraînement bot**.
  - Elo (`EloRules`) : départ 200, K fixe 20, victoire 1 / nul 0,5 / défaite 0. Plafond anti-farm
    de 2 duels par paire et par jour ; bot exclu.
- **Badges** : `AwardBadgesSaga` sur `XpAwardedEvent` → `EvaluateBadgesCommand`.
  - `BadgeRules` est **pur et générique par métrique** : ajouter un palier au catalogue (`Badge`)
    l'active sans toucher à l'agrégat.
  - `COLLECTION` se débloque en cascade.
  - Aucun compteur ne bouge sur un entraînement bot.
  - Le jour local du duel (`app.activity.zone`) est préparé par la saga et porté par la commande.
- **Compteurs hors duel** (au moins une fois, dédoublonnés par `sourceId`) →
  `RecordBadgeCounterCommand` :
  - `SocialBadgesSaga` sur `UserFollowedEvent` (social) ;
  - `ChallengeBadgesSaga` sur `ChallengeStartedEvent` (matchmaking : un défi compte seulement s'il
    a produit une partie).
- **Récompenses** : `ASSIDUITY_BRONZE` → `MICAH_AVATAR` (`RewardRules`, groupe `reward-grant`).
  `RewardRules.catalog()` liste aussi les récompenses verrouillées.
- **Onboarding** : `CompleteOnboardingCommand` (idempotente). Une première partie humaine terminée
  vaut onboarding acquis (`OnboardingCompletionHandler` sur `XpAwardedEvent`).
- **Titre porté** : `UpdateEquippedTitleCommand` (clé `topicId:TIER` ou `null`). L'appartenance
  (partie humaine dans le sujet, palier atteint) est vérifiée **par l'appelant** (BFF).
- **Centres d'intérêt** : forme validée ici (`[A-Z][A-Z_]*`, 3 au plus, sans doublon). Leur
  appartenance à `TopicCategory` est vérifiée par le BFF : profile ne dépend pas de theme.
- **Présence** :
  - le BFF pilote les sessions STOMP ;
  - bail de 30 s renouvelé par lot (`RenewPresenceSessionsCommand`) ;
  - purge des sessions d'une instance au démarrage (`ResetInstanceSessionsCommand`) ;
  - fermeture de la dernière session → `offline_deadline_at` à +15 s ;
  - `PresenceOfflineSweeper` (5 s, `UPDATE` conditionnel, multi-instances) confirme et publie
    `PlayerWentOfflineEvent`. Pas de deadline Axon.

## Projections et requêtes

- **Groupes** :
  - `profile-projection`, `progression-projection`, `activity-projection` ;
  - `unlock-projection` (rejouable, `@ResetHandler`) ;
  - `reward-projection`, `reward-grant` ;
  - les sagas ;
  - `account-deletion-profile`.
- **Déblocages datés** (`title_unlock`, `badge_unlock`, sans clé étrangère) :
  - écriture idempotente qui garde la date la plus ancienne ;
  - titre Fer et `MASTER_FER` à la première partie humaine du sujet (même à 0 XP) ;
  - paliers suivants par `MasteryTierReachedEvent` ;
  - badges par `BadgeEarnedEvent.earnedAt`.
- **Activité** (`ActivityRules`, partagé avec la famille Assiduité) : série courante et record,
  parties par jour.
- **Requêtes** (`joutio-profile-domain`) :
  - `ProfileQuery` : `GetProfileQuery`, `GetProfilesByIdsQuery`, `ProfileExistsByIdQuery`,
    `GetProfileSuggestionsQuery`, `CountImageReferencesQuery`, `ProfileSearchQuery` ;
  - `ProgressionQuery` : `GetProgressionQuery`, `GetProgressionsByIdsQuery`,
    `GetTopicProgressionQuery`, `GetTopicProgressionsQuery`, `GetActivityQuery`, `GetUnlocksQuery`,
    `GetUnlocksByIdsQuery`, `GetGamesXpQuery` ;
  - `PresenceQuery.GetPresencesByIdsQuery` ;
  - `RewardQuery.GetPlayerRewardsQuery`.

## Contrats consommés

`joutio-identity-domain`, `joutio-game-domain`, `joutio-social-domain`,
`joutio-matchmaking-domain`. Aucune requête sortante à l'exécution.

## Commandes

```bash
mvn -q -pl joutio-profile-domain test
mvn -pl joutio-profile-infrastructure test
```

## Pièges

- Les codes de badges, paliers et métriques sont des enums qui traversent le bus. Les ajouter ou
  les renommer impose de déployer le BFF (consommateur) **avant** profile
  ([contrats](../../docs/backend/contrats-et-build.md#versions-et-releases)).
- `PseudonymGenerator` est déterministe (identifiant du joueur + tentative) : la saga de création propose le même
  pseudonyme à chaque rejeu.
