package com.joutio.profile.application.service;

import com.joutio.profile.domain.model.AwardedGame;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.GameReward;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.port.out.GameRewardVersionPort;
import com.joutio.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProgressionQuery.GetGameRewardQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Récompense d'un duel en vue vivante ({@link GetGameRewardQuery}) : lecture pour un joueur, et émission à chaque
 * écriture qui la change.
 *
 * <p>{@link #changed(String)} s'appelle dans le handler d'événement qui vient d'écrire : la version est incrémentée
 * dans la même transaction, et l'émission part au commit de l'unité de travail, après l'écriture. Seuls les joueurs
 * récompensés pour la partie reçoivent un update : un abonné qui n'y a pas joué ne reçoit jamais rien.</p>
 */
@Service
public class GameRewardService {

    private final ProgressionAwardedGameRepositoryPort awardedGames;
    private final ProgressionRepositoryPort progressions;
    private final UnlockRepositoryPort unlocks;
    private final GameRewardVersionPort versions;
    private final QueryUpdateEmitter emitter;

    public GameRewardService(ProgressionAwardedGameRepositoryPort awardedGames,
                             ProgressionRepositoryPort progressions,
                             UnlockRepositoryPort unlocks,
                             GameRewardVersionPort versions,
                             QueryUpdateEmitter emitter) {
        this.awardedGames = awardedGames;
        this.progressions = progressions;
        this.unlocks = unlocks;
        this.versions = versions;
        this.emitter = emitter;
    }

    /** La récompense de la partie vue par ce joueur, vide s'il n'a pas (encore) été récompensé pour elle. */
    public Optional<GameReward> read(String gameId, String userId) {
        return read(gameId, userId, awardedGames.findByGame(gameId), versions.current(gameId));
    }

    /** Une écriture vient de changer la récompense de la partie : nouvelle version, émise à chacun de ses joueurs. */
    public void changed(String gameId) {
        long version = versions.increment(gameId);
        List<AwardedGame> awarded = awardedGames.findByGame(gameId);
        for (AwardedGame player : awarded) {
            read(gameId, player.userId(), awarded, version).ifPresent(reward -> emitter.emit(
                    GetGameRewardQuery.class,
                    query -> query.gameId().equals(gameId) && query.userId().equals(player.userId()),
                    reward));
        }
    }

    private Optional<GameReward> read(String gameId, String userId, List<AwardedGame> awarded, long version) {
        AwardedGame player = awarded.stream().filter(game -> game.userId().equals(userId)).findFirst().orElse(null);
        if (player == null) {
            return Optional.empty();
        }
        AwardedGame opponent = awarded.stream().filter(game -> !game.userId().equals(userId)).findFirst().orElse(null);
        PlayerProgress progress = progressions.findById(userId).orElse(PlayerProgress.empty(userId));
        int topicXp = player.topicId() == null ? player.xp()
                : Math.max(progress.xpByTopic().getOrDefault(player.topicId(), 0), player.xp());
        List<Badge> badges = unlocks.findByUserId(userId).badges().stream()
                .filter(unlock -> gameId.equals(unlock.gameId()))
                .sorted(Comparator.comparing(BadgeUnlock::earnedAt).thenComparing(BadgeUnlock::badge))
                .map(BadgeUnlock::badge)
                .toList();
        return Optional.of(new GameReward(gameId, userId, version, player, opponent, topicXp,
                progress.peakRating(), progress.ratedGames(), badges));
    }
}
