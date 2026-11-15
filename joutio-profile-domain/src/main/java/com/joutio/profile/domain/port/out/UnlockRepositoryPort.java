package com.joutio.profile.domain.port.out;

import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.PlayerUnlocks;
import com.joutio.profile.domain.model.TitleUnlock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistance du read model des déblocages (titres et badges datés).
 *
 * <p>Les écritures sont idempotentes : un déblocage déjà connu garde sa date la plus ancienne.</p>
 */
public interface UnlockRepositoryPort {

    void saveTitle(TitleUnlock unlock);

    void saveBadge(BadgeUnlock unlock);

    /** Déblocages d'un joueur ; vide s'il n'en a aucun. */
    PlayerUnlocks findByUserId(String userId);

    /** Déblocages de plusieurs joueurs, un élément par identifiant demandé. */
    List<PlayerUnlocks> findByUserIds(Collection<String> userIds);

    /** Badges obtenus, par joueur (lecture en lot pour la progression). */
    Map<String, Set<Badge>> findBadgesByUserIds(Collection<String> userIds);

    /** Vide le read model (reset du processeur). */
    void deleteAll();
}
