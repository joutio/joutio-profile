# Plan (à exécuter) : badges et titres datés et fiables (joutio-profile → BFF → @joutio/client → apps)

> Rédigé le 2026-11-15, non exécuté. Statut : en attente.

## Contexte

Le Profil doit montrer les « Derniers titres » (spec `profil.md` § 2, `titres.md` § 2-4). Aujourd'hui, aucune date de
déblocage n'existe en dehors du flux d'événements :

- **Titres** : aucun read model ne les stocke. Le BFF les recalcule à chaque requête à partir de l'XP par sujet
  (`ProfileViewService.titles()`), triés par palier. `MasteryTierReachedEvent(reachedAt)` n'est projeté nulle part ; seul
  `AchievementPublisher` (BFF) le pousse en STOMP, et aucun client ne s'y abonne.
- **Badges** : `BadgeEarnedEvent(earnedAt)` est projeté dans un simple ensemble (`progression_badge(user_id, badge)`).
  La date ne sert qu'à `updatedAt` puis est perdue.

En auditant `joutio-profile`, d'autres défauts sont apparus autour des badges et des titres :

1. **Badge perdu** : `ProgressionProjection.on(BadgeEarnedEvent)` ne fait rien si la ligne de progression n'existe pas
   encore (`ifPresent`, l. 172). Les agrégats badges et progression ont des identifiants différents, leurs événements
   peuvent donc arriver dans le désordre.
2. **Règle Fer incohérente** : le titre Fer s'obtient avec 1 partie humaine (BFF), le badge `MASTER_FER` avec 1 XP.
3. **Paliers sautés** : `PlayerProgressAggregate` n'annonce que le palier final. Fer n'est jamais annoncé.
4. **Compteurs comptés deux fois** : `RecordBadgeCounterCommand` ne vérifie pas `sourceId` (commentaire l. 121-125).
5. **`GrantRewardCommand` sans `@TargetAggregateIdentifier`** (`RewardCommand.java`), contrairement à toutes les autres
   commandes. Axon ne sait sans doute pas cibler l'agrégat : les récompenses de badges (styles d'avatar) échoueraient
   dans `reward-grant`. À confirmer par la DLQ du groupe dans la console.
6. **Restes du badge Recruteur** : `V6__drop_recruiter.sql` ne supprime pas les lignes `RECRUITER_*` de
   `progression_badge`, et l'event store peut contenir des `BadgeEarnedEvent(RECRUITER_*)` alors que la constante
   n'existe plus. Charger ces lignes ou reconstruire l'agrégat échouerait.
7. **Commentaires périmés** : `ProgressEntity` l. 53 et 57, `OnboardingCompletionHandler`, table `progression_entry`.

Décisions de l'utilisateur (2026-11-15) :
- **Fer = 1 partie humaine terminée dans le sujet** (abandon compris), pour le titre comme pour le badge `MASTER_FER`.
- **Date des badges** : exposée par le BFF et `@joutio/client`, **pas affichée** tant que le design ne la prévoit pas.
- Libellé « Derniers titres » / « Latest titles », publication du paquet et montée de version dans les apps.

## 1. joutio-profile (service 2.0.0 → release mineure)

### Domaine (`joutio-profile-domain`)
- `model/TitleUnlock(userId, topicId, MasteryTier tier, Instant unlockedAt)` et
  `model/BadgeUnlock(userId, Badge badge, Instant earnedAt)`, plus `model/PlayerUnlocks(titles, badges)`.
- `query/ProgressionQuery.GetUnlocksQuery(userId)` → `PlayerUnlocks`, et `GetUnlocksByIdsQuery(List)` si le BFF en a
  besoin pour plusieurs joueurs.
- `PlayerProgressAggregate` (l. 114-125) : émettre **un `MasteryTierReachedEvent` par palier franchi**, dans l'ordre
  (une boucle de `tierBefore + 1` à `tierAfter`). Fer reste implicite (voir la projection).
- `BadgeRules` / `BadgeAggregate` : `MASTER_FER` obtenu à la **première partie humaine** du sujet, même à 0 XP. Il faut
  savoir qu'un sujet est joué : l'agrégat enregistre déjà `xpByTopic` pour les parties humaines, on y inscrit le sujet
  même à 0 XP (`merge(topic, xp)`), puis on teste la présence de la clé et non `xp ≥ 1`.
- `BadgeAggregate.handle(RecordBadgeCounterCommand)` : ignorer un `sourceId` déjà enregistré (ensemble
  `recordedCounterSources` reconstruit par `@EventSourcingHandler` sur `BadgeCounterRecordedEvent`). Même pattern que
  `evaluatedGameIds`.
- `RewardCommand.GrantRewardCommand` : `@TargetAggregateIdentifier` sur `rewardId`.
- Tests `AggregateTestFixture` (`best-practices/.backend/aggregate-testing.md`) :
  - saut de deux paliers → deux événements ;
  - `MASTER_FER` à 0 XP ;
  - compteur rejoué avec le même `sourceId` ;
  - `GrantRewardCommand` routée.

### Read model des déblocages (`joutio-profile-infrastructure`)
- Migration `V7__unlocks.sql` :
  - créer `title_unlock(user_id, topic_id, tier, unlocked_at, PK(user_id, topic_id, tier))` ;
  - créer `badge_unlock(user_id, badge, earned_at NULL, PK(user_id, badge))` ;
  - copier les lignes de `progression_badge` dans `badge_unlock`, sans date, hors `RECRUITER_%` ;
  - supprimer les lignes `RECRUITER_%` de `progression_badge`.
- `application/projection/UnlockProjection`, `@ProcessingGroup("unlock-projection")`, dédié : il ne touche à aucune
  autre table, donc un reset ou un replay est sans risque. Toutes les écritures sont **idempotentes** : insérer si absent,
  garder la date la plus ancienne, remplir une date `null`.
  - `XpAwardedEvent` d'une partie humaine (`!botGame`) → titre **Fer** du sujet à `awardedAt`.
  - `MasteryTierReachedEvent` → tous les paliers de Bronze à `tier` absents, à `reachedAt` (couvre les sauts des
    événements anciens).
  - `BadgeEarnedEvent` → `badge_unlock` à `earnedAt`.
  - `@ResetHandler` qui vide les deux tables.
- Ports et adaptateurs : `UnlockRepositoryPort` (domaine) et l'adaptateur JPA, sur le modèle de
  `PlayerRewardRepositoryAdapter` (qui stocke déjà `unlocked_at`).
- `ProgressionQueryHandler` : répond à `GetUnlocksQuery`. `PlayerProgress.badges` est désormais lu depuis
  `badge_unlock` : la source de vérité ne dépend plus de l'ordre d'arrivée, ce qui corrige le badge perdu.
  `ProgressionProjection.on(BadgeEarnedEvent)` est retiré ; `progression_badge` sera supprimée plus tard (V8), une fois
  la release validée.
- **Remplissage de l'historique** : le topic Kafka ne garantit pas la rétention de tout l'historique, et la projection
  de progression n'est pas rejouable (pas de `@ResetHandler`, compteurs incrémentés). On lit donc l'event store, selon
  `best-practices/.backend/event-store-adapter.md` :
  - `ProgressionEventStorePort` (domaine) et son adaptateur dans `infrastructure/out/messaging/adapter/`. L'adaptateur
    lit les flux `progress:<userId>` et `badges:<userId>`, garde les `ProgressionEvent` et ignore les payloads
    illisibles (`RECRUITER_*`) ;
  - `UnlockBackfill`, un `ApplicationRunner` qui parcourt les `user_id` de `progression_entry` et réapplique les mêmes
    upserts que la projection. Il est idempotent et marqué fait dans une table `unlock_backfill_run` (une seule exécution
    même avec plusieurs répliques : `INSERT … ON CONFLICT DO NOTHING` comme verrou).
- **Recruteur dans l'event store** : avant de coder, vérifier en base ce que contient l'event store, par exemple
  `SELECT count(*) FROM domain_event_entry WHERE payload_type LIKE '%BadgeEarnedEvent' AND convert_from(payload,'UTF8') LIKE '%RECRUITER%'`.
  S'il y en a, ajouter un upcaster qui écarte ces événements, pour que `BadgeAggregate` se recharge. Sinon, rien à faire
  (règle : pas de garde-fou spéculatif).
- Commentaires périmés corrigés (point 7).

## 2. BFF (`services/joutio-bff`)
- Monter le pin `joutio.profile-domain.version` (`joutio-bff-infrastructure/pom.xml` l. 25) vers la nouvelle release.
- `ProfileViewService.titles()` :
  - la liste des titres vient de `GetUnlocksQuery` (titres réellement débloqués, avec leur date) ;
  - l'XP par sujet sert seulement aux champs de progression (`tierProgressPercent`, `tierRemainingXp`, `nextTier`) ;
  - tri par `unlockedAt` décroissant, puis palier décroissant, puis nom du sujet.
- `TitlesView.TitleView` : ajouter `Instant unlockedAt`. `BadgeCollectionView.BadgeItemView` : ajouter
  `Instant earnedAt`, `null` si le badge n'est pas acquis ou si sa date n'est pas connue.
- `badges()` : `earned` et `earnedAt` depuis `PlayerUnlocks.badges`.
- `ProfileUpdateService` (contrôle du titre porté, l. 128-146) : vérifier la possession sur les mêmes déblocages.
- Mettre à jour `ProfileControllerTest`, qui construit `TitlesView`.

## 3. @joutio/client (`libraries/joutio-client`)
- `src/api-types/bff.ts` : `TitleView.unlockedAt: string` et `BadgeItemView.earnedAt: string | null`.
- i18n : `profile.lastTitles` → FR « Derniers titres », EN « Latest titles ».
- Commit `feat(...)` sur `main`, la CI publie avec semantic-release. À vérifier au passage : le `package.json` local est
  en 3.0.0 alors que la version 4.0.0 est publiée (tags locaux à jour ?).

## 4. Apps
- **Web** (`features/profile/components/profile-tabs.tsx` `TitlesTab`, `titles-page.tsx`) :
  - onglet Titres : les 5 derniers titres, **groupés par date** (`formatDate`), comme le prévoit la spec ;
  - tri « Plus récents » / « Plus anciens » réel sur `unlockedAt`, l'ordre des sujets suivant `titres.md` § 3-4 ;
  - retirer les commentaires « sans `unlockedAt` ».
- **Mobile** (`features/profile/components/profile-tabs.tsx`, `titles-screen.tsx`) : même onglet ; ajouter le **tri**
  que la spec prévoit (`titres.md` § 9) et qui manque au code.
- Monter la version de `@joutio/client` dans les deux apps.
- Badges : rien d'affiché (décision du 2026-11-15).

## 5. Specs
- `design/specs/screens/titres.md` § 6 et § 9 et `profil.md` § 7 : la dépendance `unlockedAt` est levée.
- `design/specs/components/…` (badges) : `earnedAt` est exposé, pas affiché.
- `services/joutio-profile/AGENTS.md` : read model des déblocages, règle Fer, remplissage de l'historique.

## Ordre des releases
1. joutio-profile : domaine, infrastructure, migration V7 et remplissage. Release, puis déploiement ; vérifier le
   remplissage (nombre de lignes de `title_unlock` et `badge_unlock`, dates non nulles).
2. BFF : montée du pin et vues. Release.
3. `@joutio/client` : publication.
4. Web et mobile : montée de version et écrans.

## Vérification
- `mvn -pl services/joutio-profile -am verify` : tests d'agrégats (sauts de paliers, Fer à 0 XP, compteurs
  idempotents, routage de `GrantRewardCommand`) et tests unitaires de `UnlockProjection` (idempotence, date la plus
  ancienne gardée, Fer sur la première partie humaine, bot ignoré).
- Tests du remplissage : un faux port d'event store avec des flux mêlant paliers, badges et un payload illisible.
- BFF : `mvn verify`, `ProfileControllerTest` et `ProfileViewService` (tri par date, champs de progression intacts).
- Après le déploiement de profile :
  - les DLQ `reward-grant` et `progression-projection` sont vides dans la console ;
  - un duel humain crée la ligne Fer ;
  - un badge obtenu a sa date ;
  - `GET /api/profiles/{id}/titles` est trié par date.
- Apps : `npm run typecheck`, `lint` et tests. Onglet Titres groupé par date sur le web compact et sur mobile, tri de
  Tous les titres.
