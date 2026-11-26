package com.joutio.profile.application.service;

import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.PlayerProgressUpdate;
import com.joutio.profile.domain.model.PlayerRewards;
import com.joutio.profile.domain.model.PlayerRewardsUpdate;
import com.joutio.profile.domain.model.ProfileUpdate;
import com.joutio.profile.domain.port.out.LiveVersionPort;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProfileQuery.WatchProfilesQuery;
import com.joutio.profile.domain.query.ProgressionQuery.WatchProgressionsQuery;
import com.joutio.profile.domain.query.RewardQuery.WatchPlayerRewardsQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vues d'un joueur surveillées par les caches du BFF : profil, progression et récompenses.
 *
 * <p>Chaque méthode {@code *Changed} s'appelle dans le handler qui vient d'écrire : la version de la vue est
 * incrémentée dans la même transaction (la ligne reste verrouillée, donc deux groupes qui écrivent pour le même joueur
 * se suivent), puis la vue est relue et émise. L'émission part au commit de l'unité de travail.</p>
 */
@Service
public class PlayerLiveViews {

    static final String PROFILE = "profile";
    static final String PROGRESSION = "progression";
    static final String REWARDS = "rewards";

    private final ProfileRepositoryPort profiles;
    private final ProgressionRepositoryPort progressions;
    private final UnlockRepositoryPort unlocks;
    private final PlayerRewardRepositoryPort rewards;
    private final LiveVersionPort versions;
    private final QueryUpdateEmitter emitter;

    public PlayerLiveViews(ProfileRepositoryPort profiles,
                           ProgressionRepositoryPort progressions,
                           UnlockRepositoryPort unlocks,
                           PlayerRewardRepositoryPort rewards,
                           LiveVersionPort versions,
                           QueryUpdateEmitter emitter) {
        this.profiles = profiles;
        this.progressions = progressions;
        this.unlocks = unlocks;
        this.rewards = rewards;
        this.versions = versions;
        this.emitter = emitter;
    }

    /** Progressions des joueurs, badges compris, une par identifiant (neutre pour un joueur sans duel). */
    public List<PlayerProgress> progressions(List<String> userIds) {
        Map<String, PlayerProgress> byId = progressions.findByIds(userIds).stream()
                .collect(Collectors.toMap(PlayerProgress::userId, Function.identity(), (a, _) -> a));
        Map<String, Set<Badge>> badges = unlocks.findBadgesByUserIds(userIds);
        return userIds.stream()
                .map(userId -> byId.getOrDefault(userId, PlayerProgress.empty(userId)).toBuilder()
                        .badges(badges.getOrDefault(userId, Set.of()))
                        .build())
                .toList();
    }

    public PlayerRewards rewards(String userId) {
        return new PlayerRewards(userId, rewards.findRewards(userId));
    }

    public void profileChanged(String userId) {
        long version = versions.increment(PROFILE, userId);
        profiles.findById(userId).ifPresent(profile -> emitter.emit(WatchProfilesQuery.class, _ -> true,
                new ProfileUpdate(userId, version, profile)));
    }

    public void progressionChanged(String userId) {
        long version = versions.increment(PROGRESSION, userId);
        emitter.emit(WatchProgressionsQuery.class, _ -> true,
                new PlayerProgressUpdate(userId, version, progressions(List.of(userId)).getFirst()));
    }

    public void rewardsChanged(String userId) {
        long version = versions.increment(REWARDS, userId);
        emitter.emit(WatchPlayerRewardsQuery.class, _ -> true,
                new PlayerRewardsUpdate(userId, version, rewards(userId)));
    }

    /** Compte supprimé : chaque vue est émise vide (le handler de suppression efface aussi les versions). */
    public void deleted(String userId) {
        emitter.emit(WatchProfilesQuery.class, _ -> true, new ProfileUpdate(userId, Long.MAX_VALUE, null));
        emitter.emit(WatchProgressionsQuery.class, _ -> true, new PlayerProgressUpdate(userId, Long.MAX_VALUE, null));
        emitter.emit(WatchPlayerRewardsQuery.class, _ -> true, new PlayerRewardsUpdate(userId, Long.MAX_VALUE, null));
    }
}
