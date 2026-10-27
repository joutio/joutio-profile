# AGENTS.md — joutio-profile

> Service **référence** des patterns hexagonaux Joutio : agrégat + saga + projection.
> Architecture : Axon Framework (CQRS/EDA) + JPA (projection) + JWT Resource Server (SDK).
> Pour les règles de patterns :
> [`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

Propriétaire des **données modifiables** du profil utilisateur (`pseudonym`, `bio`, `country`,
`avatarOptions`, `bannerUrl`, `language`).
Identity (`joutio-identity`) reste le IdP : identité immuable + JWT (lecture du profil côté
frontend = claims du JWT). Ce service est **créé par événement** (saga sur
`UserRegisteredEvent` d'identity) et mis à jour **uniquement par le propriétaire**, **champ par
champ** (une commande/événement/endpoint par champ ; no-op si la valeur est inchangée).

Il est aussi la **source unique d'existence et de pseudonyme** des utilisateurs pour les
autres services : social et matchmaking n'interrogent plus identity mais **profile** (queries
`ProfileQuery.ProfileExistsByIdQuery` / `ProfileQuery.GetProfileQuery` sur le bus partagé).
Le `PseudonymGenerator` (pseudonyme déterministe généré à la création) vit dans ce domaine.

Il porte enfin la **progression du joueur** : XP **par thème**, Elo global
(`PlayerProgressAggregate`, `progress:<userId>`), **badges** (`BadgeAggregate`, `badges:<userId>`)
et **récompenses persistées** (`PlayerRewardAggregate`, `rewards:<userId>`). Les agrégats sont
namespacés pour ne pas entrer en collision avec le `ProfileAggregate`. L'XP et les badges sont
attribués de façon idempotente à la fin de chaque duel (sagas sur `GameEndedEvent` de
`joutio-game`) ; les récompenses sont débloquées par les badges. **L'XP n'existe qu'au niveau
d'un thème** ; l'Elo est la seule métrique au niveau du joueur. Il n'y a plus de niveau ni de
titre de compte.

Il expose aussi la **présence joueur** (`/api/presence`) : read-model éphémère piloté par le cycle
de vie des **sessions temps réel STOMP** (connexion/déconnexion) et les **heartbeats** du broker
(le BFF ferme un client silencieux en ~30 s) — utilisé pour les pastilles « en ligne » et le
forfait des duels. Côté web, un onglet non visible est déconnecté proprement ; la grâce de 15 s
absorbe les bascules rapides.

**Passage hors ligne — TTL en base + balayeur** (pas de deadline Axon) :

- la fermeture de la dernière session arme `presence_entry.offline_deadline_at` (+15 s de grâce) ;
  un balayeur périodique (`PresenceOfflineSweeper`, 5 s) confirme la transition par un `UPDATE`
  conditionnel (idempotent, multi-instances) et publie `PlayerWentOfflineEvent` ;
- **bail de session** : chaque session porte `last_seen_at`, renouvelé par le BFF en batch
  (`RenewPresenceSessionsCommand`, toutes les 10 s). Une session sans renouvellement depuis
  `SESSION_LEASE_TTL` (30 s) est supprimée par le balayeur (crash d'instance, partition réseau),
  ce qui évite toute session fantôme même si le BFF disparaît sans déconnexion propre ;
- les sessions portent `instance_id` : au démarrage, le BFF purge en plus ses propres sessions
  antérieures à son boot (`ResetInstanceSessionsCommand`) et déconnecte ses sessions locales à
  l'arrêt (`@PreDestroy`). Les commandes de présence du BFF sont retentées tant que le routage
  distribué n'est pas prêt.

**Package** : `com.joutio.profile`

**Avatar** : le profil porte `avatarOptions` (JSON des options DiceBear, styles `micah`, `lorelei` ou
`notionists`, cf. `web-applications/joutio-web/src/shared/avatar/styles/`) choisi par
le propriétaire. L'avatar de base (`base`) est gratuit ; **Micah** est débloqué par la récompense
`MICAH_AVATAR` (badge d'assiduité Bronze, 7 jours) ou Premium/admin ; **Lorelei** et
**Notionists** restent réservés Premium/admin. L'avatar de base utilise la silhouette Lucide et
un fond de la palette Micah. **Aucun stockage d'image** : l'avatar est régénéré côté client de façon déterministe
à partir de ces options (repli sur `userId` si `null`).

**Onboarding** : `profile_entry.onboarding_completed_at` (migration `V17`, nullable, **sans
backfill**) porte la fin ou le skip explicite du parcours ; commande
`CompleteOnboardingCommand` idempotente et événement `ProfileOnboardingCompletedEvent`. Le
`OnboardingCompletionHandler` écoute `XpAwardedEvent` dans le groupe existant
`progression-projection` : une première partie **humaine** terminée vaut onboarding acquis
(entraînement bot exclu), sans rejeu historique au déploiement. Le flag est exposé au BFF via
`Profile.onboardingCompletedAt` ; `MeView.onboardingCompleted` le complète par
`PlayerProgress.gamesPlayed() > 0` (parties humaines déjà jouées avant le flag).

**Centres d'intérêt** : `profile_entry.interests` (migration `V20`, `VARCHAR(255)`, nullable)
porte une liste **ordonnée** de codes `TopicCategory` séparés par des virgules, au maximum 3 ;
le premier est l'**univers principal** et priorise la recommandation du premier thème / le rail
« Pour toi ». `UpdateProfileInterestsCommand` est idempotente et émet
`ProfileInterestsUpdatedEvent` ; l'agrégat valide la forme (`[A-Z][A-Z_]*`, max 3, sans doublon)
mais **pas l'appartenance à l'enum thème** (frontière de bounded context : le domaine profile ne
dépend pas de `joutio-theme-domain`). Le BFF valide les codes avec `TopicCategory` avant l'envoi.
Le flag onboarding reste indépendant : les intérêts peuvent être posés avant ou après lui.

**Récompenses** : la table `player_reward` (migration `V18`) persiste les droits débloqués
(`user_id`, `reward_code`, `source`, `unlocked_at`). `PlayerRewardAggregate` (`rewards:<userId>`)
est idempotent et définitif. Un handler de `BadgeEarnedEvent` traduit `ASSIDUITY_BRONZE` en
`MICAH_AVATAR` (7 jours d'activité consécutive) via `RewardRules` ; le badge reste une réussite
autonome et ne connaît pas l'avatar. `GetPlayerRewardsQuery` expose les `RewardGrant` (code,
source, date) et `RewardRules.catalog()` fournit le catalogue complet, verrouillées comprises,
pour le BFF.

**Bannière** : le profil porte `bannerUrl` (URL http(s) ≤1024, `null` = aucune), uploadée via
`POST /api/media` (BFF → MinIO) puis attachée par `PUT /api/profiles/{userId}/banner` ; l'image est
affichée derrière l'avatar des fiches joueur (web). Migration `V7__add_banner_url_to_profile_entry.sql`.

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`joutio-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `GetProfileUseCase` — récupération par userId (404 si inconnu)
- `UpdateProfileUseCase` — mise à jour par champ (pseudonyme, bio, pays, avatar, bannière, langue) ;
  propriétaire uniquement, no-op si la valeur est inchangée
- `CreateProfileUseCase` — création (utilisée par le seeding système ; en nominal par la saga)
- `CheckProfileUseCase` — vérification d'existence (`existsById`)
- `SearchProfileUseCase` — recherche paginée (pattern SDK `SearchRequest` → `SearchResponse`)
- `GetProgressionUseCase` — XP par thème, Elo et badges

**Queries** (`domain/query/ProfileQuery.java`) : `GetProfileQuery` (consommée par
social/matchmaking pour le pseudonyme), `ProfileExistsByIdQuery` (consommée par social),
`ProfileSearchQuery`.
**Queries progression** (`ProgressionQuery.java`) : `GetProgressionQuery`, `GetTopicProgressionQuery`.
**Queries récompenses** (`RewardQuery.java`) : `GetPlayerRewardsQuery` → `RewardGrant` (code, source, date).

**Ports sortants locaux** : `ProfileRepositoryPort` (persistance de la projection, dont
`existsById`), `ProgressionRepositoryPort` (projection de progression).

---

## 4. Dépendances inter-services

**Conso** : `joutio-identity-domain` (artifact Maven) — la saga
`CreateProfileSaga` (`@StartSaga @SagaEventHandler(associationProperty = "userId")`) consomme
`UserEvent.UserRegisteredEvent` via le bus Kafka partagé (transport Axon distributed). La saga est
**idempotente sans lecture de projection** : `CreateProfileCommand` est idempotente dans
`ProfileAggregate` (`@CreationPolicy(CREATE_IF_MISSING)`, no-op si le profil existe déjà) — la saga
est replay-safe et ne dépend d'aucun read model
(`best-practices/.backend/anti-patterns.md` § 4).

**Seeding système** : `ProfileDataSeeder` (`CommandLineRunner`, `app.seed-data.enabled`) garantit
les profils des utilisateurs système (`JoutioConstants.SYSTEM_USER_ID`),
indépendamment de la saga : si `profile` n'était pas abonné au flux Kafka quand `identity` a publié
`UserRegisteredEvent`, ces profils seraient sinon manquants. Il passe par les use cases
`CheckProfileUseCase` + `CreateProfileUseCase` (pas de port direct) et est idempotent.

**Conso progression** : `joutio-game-domain` (artifact Maven) — la saga
`AwardProgressSaga` (`@SagaEventHandler(associationProperty = "gameId")`) consomme
`GameEvent.GameEndedEvent` et envoie un `ProgressionCommand.AwardXpCommand` par joueur humain
(idempotence par `gameId` portée par l'agrégat). Le compte système (`JoutioConstants.SYSTEM_USER_ID`) est ignoré.

**Aucun QueryGateway sortant** : identity n'est jamais interrogé à l'exécution.

**Fourniture (sortant)** : les queries `ProfileQuery.GetProfileQuery` /
`ProfileExistsByIdQuery` sont consommées par `joutio-social` et `joutio-matchmaking` (bus
partagé) — ces services ne dépendent plus de `joutio-identity-domain`.

### Enrichissements progression

- `ProgressionResponse` porte `duelStats` (joués, victoires, défaites, égalités, winrate, meilleur
  score, meilleure série) et `TopicProgressResponse` porte `title` (titre par thème).
- **Elo global du profil** (`EloRules`) : départ **200**, provisoire sur **10 duels classés**,
  `K=40` puis `20`, victoire `1` / nul `0.5` / défaite `0`. Tous les duels humains comptent, avec
  un **plafond anti-farm de 2 duels par paire et par jour** ; les duels bot sont exclus. L'Elo est
  porté par `progression_entry` (`rating`, `peak_rating`, `rated_games`, `provisional`) et mis à
  jour par `AwardProgressSaga` + `PlayerProgressAggregate` ; il ne remplace pas l'XP de sujet, qui
  reste la source des titres, badges et classements par thème.
- **XP par thème** : `XpAwardedEvent` porte l'XP du sujet et l'Elo. Il n'existe plus d'XP globale,
  de niveau ni de titre de compte ; `ProgressionRules` ne conserve que le calcul d'XP de duel et
  les identifiants d'agrégats.
- **Maîtrise par sujet** (`MasteryTier`) : échelle de **rareté** portée par
  l'XP cumulée **dans le sujet**, en 7 paliers à table de seuils explicite
  (`FER 0 · BRONZE 500 · ARGENT 1 500 · OR 4 000 · PLATINE 9 000 · EMERAUDE 21 000 ·
  DIAMANT 45 000`). C'est le support des **titres thématiques**. Le palier **est** la rareté :
  `TitleRank` a été supprimé et aucun rang séparé n'est persisté.
- **Titres portés** (`PlayerTitle`, clé stable `topicId:TIER`) : le joueur choisit le titre
  affiché sur son profil, les cartes et les listes. On persiste la **clé**, jamais le libellé :
  celui-ci se compose à l'affichage (palier + nom du sujet, ex. « Or · Histoire »), donc il
  suit le renommage du sujet et la langue du lecteur. Champ `profile_entry.equipped_title`
  (migration `V9`, commentaire mis à jour par `V19`), commande `UpdateEquippedTitleCommand`
  (propriétaire uniquement, clé validée, `null` efface) ; l'**appartenance** du titre (sujet avec
  **au moins une partie humaine terminée** — abandon inclus, l'entraînement bot à 0 XP n'ouvre
  rien — et palier réellement atteint) est vérifiée par l'appelant, qui prépare la commande.
- **Catalogue de badges** (`Badge`) organisé en **familles × paliers × métrique** : `BadgeFamily`
  (`VOLUME`, `VICTORY`, `MASTERY`, `EXPLOIT`, `COLLECTION`, puis `SERIES`, `ASSIDUITY`, `SOCIAL`,
  `CHALLENGES`, `VISION`, `EXPLORATION`, `PRECISION`, `SPEED`), `BadgeTier`
  (`FER → BRONZE → ARGENT → OR → PLATINE → EMERAUDE → DIAMANT`), `BadgeMetric` (`HUMAN_DUELS`, `HUMAN_WINS`, `TOPIC_MASTERY`,
  `DISTINCT_TOPICS`, `WIN_STREAK`, `PERFECT_DUELS`, `FAST_ANSWERS`, `DAILY_STREAK`,
  `FOLLOWERS_GAINED`, `CHALLENGES_SENT`, `IMAGE_CORRECT`, `EARNED_BADGES`, `ONE_SHOT`) et
  `target` — ce qui permet une **progression visible** (« 7/10
  duels ») et laisse une raison de continuer après le fer, jusqu'au diamant. Le catalogue
  compte **12 échelles × 7 paliers = 84 badges + 4 exploits = 88 badges** répartis en
  **13 familles** (`VICTORY` incluse : **une famille = une échelle**, aucune ne mélange deux
  compteurs ; les quatre exploits ponctuels partagent `EXPLOIT`) ; chaque échelle monte ses
  paliers à cibles strictement croissantes (« Dueliste de fer 1 · Duelliste 5 · Bretteur 15 ·
  Lame d'élite 40 · Légende des duels 100 · Maître d'armes 300 · Mythe vivant 1000 »).
  **Un palier = une constante**
  (`DUELIST_BRONZE`, … `DUELIST_OR`) : les badges acquis restent un simple ensemble de noms,
  **sans migration** ; les 4 codes historiques (`FIRST_WIN`, `PERFECT`, `LIGHTNING`,
  `STREAK_MASTER`) sont préservés (test dédié).
  Familles : **Volume** (duels entre joueurs **joués**), **Victoires** (`VICTORY` : duels entre
  joueurs **gagnés** — famille distincte, aucune famille ne mélange deux compteurs), **Maîtrise**
  (XP du meilleur sujet, **alignée 1:1 sur les 7 paliers** : Initié 1 · Spécialiste 500 · Érudit
  1 500 · Autorité 4 000 · Sommité 9 000 · Oracle 21 000 · Panthéon 45 000),
  **Exploit** (faits ponctuels : première victoire, duel parfait, 5 réponses rapides, série de
  victoires), **Collection** (nombre de badges acquis — 8 / 20 / 35 / 50 / 65 / 78 / 85 sur
  88), **Série** (`WIN_STREAK` : victoires
  consécutives — Sur la lancée 3 / En série 6 / Imbattable 15), **Assiduité** (`DAILY_STREAK` :
  jours joués d'affilée, même règle pure que la projection d'activité — Persévérant 3 / Régulier 7 /
  Accro 14), **Exploration** (`DISTINCT_TOPICS` :
  sujets joués — Explorateur 3 / Curieux 8 / Touche-à-tout 15), **Précision** (`PERFECT_DUELS` :
  duels parfaits cumulés — Rigoureux 3 / Métronome 8 / Machine à points 20) et **Rapidité**
  (`FAST_ANSWERS` : réponses rapides cumulées — Prompt 15 / Doigt sur la gâchette 40 / Rafale
  100), **Vision** (`IMAGE_CORRECT` : bonnes réponses sur les questions **illustrées**,
  comptées par `joutio-game` (release ≥ 8.5.0, `GameEndedEvent.playerNImageCorrectAnswers`) —
  Attentif 10 / Observateur 25 / Œil de lynx 60), **Social** (`FOLLOWERS_GAINED` : abonnés
  gagnés, cumulés — Sociable 3 / Populaire 10 / Célébrité 30) et **Défis** (`CHALLENGES_SENT` :
  défis nominatifs lancés — Opposant 2 / Challenger 5 / Provocateur 15).
  > **Première victoire** : elle ne débloque que **3 badges** (`FIRST_WIN`, `DUELIST_FER`,
  > `MASTER_FER`) ; `PERFECT` / `LIGHTNING` peuvent en ajouter deux au maximum, et la famille
  > Collection ne démarre qu'à **8 badges** — aucune cascade `COLLECTOR_FER` au premier duel.
  > **Compteurs hors duel** (Social, Défis) : les sagas `SocialBadgesSaga` (sur
  > `UserFollowedEvent` de `joutio-social`) et `ChallengeBadgesSaga` (sur
  > **`ChallengeStartedEvent`** de `joutio-matchmaking`, propriétaire des défis — émis quand la
  > salle du défi **produit la partie** : un défi compte donc **uniquement s'il a été accepté et a
  > abouti sur une partie**, ni la création ni l'acceptation seules ne créditent le compteur)
  > envoient un
  > `RecordBadgeCounterCommand` ; l'agrégat incrémente le compteur de la métrique
  > (`BadgeCounterRecordedEvent`, consommé par la projection) puis réévalue. Ces services ne sont
  > **pas modifiés** hors cet événement : `joutio-profile-infrastructure` consomme seulement leurs
  > contrats (pins
  > `joutio-social-domain` / `joutio-matchmaking-domain`). Les désabonnements ne retirent rien
  > (un badge est définitif) et la livraison étant au-moins-une-fois, un rejeu ne peut que faire
  > progresser un badge cosmétique.
- `AwardXpCommand` / `XpAwardedEvent` portent `correctAnswers`, `fastAnswers`, `draw` et
  `botGame` (issus de `GameEndedEvent`). **Règle produit** : un duel contre bot est un
  **entraînement** — `ProgressionRules.xpFor(score, won, botGame)` vaut **0 XP** et
  **aucun badge ni titre ne peut y être acquis** (`BadgeRules.evaluate` court-circuite
  `botGame`, aucun compteur d'agrégat ni de projection ne bouge) : l'acquisition est réservée aux
  **parties contre des humains**. Le duel est exclu des stats V/N/D (colonne `draws`, migr. V5),
  du classement et de la série journalière ; les égalités sont comptées comme telles (jamais en
  défaite). Compteurs `duelStats` maintenus en projection, les événements antérieurs
  (`botGame=false`, `draw=false`) restent comptés. Le **jour local** du duel
  (`app.activity.zone`) est préparé par la saga des badges et porté par la commande (l'agrégat
  reste pur).
- **Attribution des paliers** (`BadgeRules`, règles **pures** testées sans Axon) : les badges
  vivent dans un **agrégat dédié `BadgeAggregate`** (`badges:<userId>`), séparé de
  `PlayerProgressAggregate` (qui ne porte plus que XP par thème et Elo). La saga
  **`AwardBadgesSaga`** consomme `XpAwardedEvent` (qui porte déjà tous les faits du duel) et
  envoie un `EvaluateBadgesCommand` ; `BadgeAggregate` maintient ses propres compteurs (duels et
  victoires entre joueurs, XP par sujet et **série de victoires globale** (tous sujets
  confondus), duels parfaits et réponses rapides
  cumulés, sujets joués), les met à jour **au replay** depuis `BadgeFactsRecordedEvent`
  (idempotent par `gameId`), puis évalue les faits et émet un `BadgeEarnedEvent` par badge
  nouvellement acquis. L'évaluation est **générique par métrique** (tout badge dont la cible est
  atteinte), donc **ajouter un palier au catalogue l'active sans toucher à l'agrégat**. La famille
  `COLLECTION` se débloque **en cascade** (un badge acquis peut franchir un palier de collection,
  qui en franchit un autre). **Aucun badge ne s'acquiert sur un entraînement contre bot** : ni
  exploit (victoire, score parfait, réponses rapides), ni compteur de volume/maîtrise/série/
  assiduité/précision/rapidité/vision — tous ne progressent que sur les **parties contre des
  humains** (les compteurs affichés en projection suivent la même règle). Ordre d'émission stable
  (`EnumSet`) → replay déterministe. Le read
  model des badges est inchangé (badges écrits par la projection) ; deux compteurs ont été
  ajoutés à `progression_entry` pour l'affichage de la progression (`perfect_duels`,
  `fast_answers_total`, migration `V10` ; `current_day_streak` / `best_day_streak` /
  `last_active_date`, migration `V11` ; `followers_gained` / `challenges_sent`, migration
  `V12` ; `image_correct_total`, migration `V13`).
- **Exposition** : `MasteryTier`, `Badge` (famille/palier/cible), les titres thématiques
  (`equippedTitle`) et les badges sont exposés par le BFF (`/api/profiles/{id}/badges`, `/titles`,
  `/title`) et affichés par le web (collection de badges par familles, sélecteur de titre porté,
  ruban teinté par palier). `TitlesView.TitleView` porte `tier` (`FER`…`DIAMANT`) ; il n'y a plus
  de `rank` séparé, ni de titre de compte.
  > ⚠️ **Ordre de déploiement obligatoire** : les nouveaux codes de badges (`*_FER`, `DUELIST_*`,
  > `WINNER_*`, `MASTER_*`, `COLLECTOR_*`, `SERIES_*`, `ASSIDUITY_*`, `EXPLORER_*`,
  > `PRECISION_*`, `SPEED_*`, `SOCIAL_*`, `CHALLENGES_*`, `VISION_*`), le palier `BadgeTier.FER`
  > et les
  > familles/métriques ajoutées (`SERIES`, `ASSIDUITY`, `SOCIAL`, `CHALLENGES`, `VISION`,
  > `EXPLORATION`, `PRECISION`, `SPEED` ;
  > `DISTINCT_TOPICS`, `WIN_STREAK`, `PERFECT_DUELS`, `FAST_ANSWERS`, `DAILY_STREAK`,
  > `FOLLOWERS_GAINED`, `CHALLENGES_SENT`, `IMAGE_CORRECT`) sont des constantes
  > d'énumération qui traversent le query bus. Le Jackson par défaut **échoue sur une constante
  > inconnue** (`READ_UNKNOWN_ENUM_VALUES_AS_NULL` désactivé) : un BFF encore épinglé sur
  > l'ancienne version casserait la lecture de `PlayerProgress`. Il faut donc **release profile →
  > bump du pin dans le BFF → release BFF + web → déployer le web, puis le BFF, puis
  > `joutio-profile`** (le BFF/web à jour tolère les deux jeux de badges, l'ancien non).
- **Activité journalière** : `ActivityProjection` (règle pure `ActivityRules`, réutilisée par
  l'agrégat de badges pour la famille Assiduité) alimente `progression_activity` (série
  courante/record, dernier jour actif) et `progression_activity_day` (parties par jour) depuis
  `GameEndedEvent` ; capability conservée côté service mais **non exposée par le BFF pour le MVP**
  (le panneau web a été retiré, à rebrancher plus tard).

### Vues BFF (queries dédiées)

- `PresenceQuery.GetPresencesByIdsQuery(userIds)` → `List<PlayerPresence>` : présences en lot
  (les joueurs sans ligne sont absents ; `404` REST = jamais connecté).
- `ProgressionQuery.GetGamesXpQuery(userId, gameIds)` → `List<GameXp>` : XP réellement attribuée
  par partie, lue depuis le journal `progression_awarded_game` (colonne `xp`, migr. V3).
- `ProfileQuery.GetProfileSuggestionsQuery(nameQuery, limit)` : suggestions ⌘K par nom.
- `ProfileQuery.CountImageReferencesQuery(imageUrls)` → `List<ImageReferenceCount>` : comptage batch
  des bannières de profil référencées, réservé à la **console média admin** du BFF
  (`/api/admin/media`, `ROLE_ADMIN` — une suppression d'objet référencé est bloquée).
- `GetProgressionsByIdsQuery` est consommée par le BFF pour l'Elo et les badges des listes de personnes ;
  `SearchProfileUseCase` / `SearchChallengeUseCase` restent pour les futures surfaces d'administration.
