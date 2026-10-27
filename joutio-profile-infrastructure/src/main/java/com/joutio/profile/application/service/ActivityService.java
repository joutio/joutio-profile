package com.joutio.profile.application.service;

import com.joutio.profile.domain.model.ActivityDay;
import com.joutio.profile.domain.model.ActivityRules;
import com.joutio.profile.domain.model.ActivityView;
import com.joutio.profile.domain.model.PlayerActivity;
import com.joutio.profile.domain.port.in.GetActivityUseCase;
import com.joutio.profile.domain.port.out.ActivityRepositoryPort;
import com.joutio.profile.infrastructure.properties.AppProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Activité journalière d'un joueur : série courante/record et détail des jours joués.
 */
@Service
public class ActivityService implements GetActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ZoneId activityZone;

    public ActivityService(ActivityRepositoryPort activityRepositoryPort,
                           AppProperties properties) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityZone = ZoneId.of(properties.activity().zone());
    }

    @Override
    public ActivityView get(String userId, LocalDate from, LocalDate to) {
        PlayerActivity activity = activityRepositoryPort.findActivity(userId)
                .orElseGet(() -> PlayerActivity.empty(userId));
        List<ActivityDay> days = activityRepositoryPort.findDays(userId, from, to);

        return new ActivityView(
                userId,
                // Le read model n'est écrit qu'à la fin d'un duel : sans nouvelle partie, une
                // série interrompue doit tout de même apparaître à zéro.
                ActivityRules.currentStreakAt(activity, LocalDate.now(activityZone)),
                activity.longestStreak(),
                activity.lastActiveDate(),
                days.size(),
                days
        );
    }
}
