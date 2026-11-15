package com.joutio.profile.application.handler.event;

import com.joutio.profile.domain.command.ProfileCommand;
import com.joutio.profile.domain.event.ProgressionEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * La première partie humaine terminée vaut onboarding implicite : un joueur invité dans une
 * room privée a joué avant de voir le flow, il n'a plus besoin de celui-ci. Un entraînement
 * contre le bot émet bien un {@code XpAwardedEvent} (0 XP, {@code botGame}) : il est écarté
 * explicitement. Une partie annulée n'émet aucun événement d'XP.
 */
@Component
@ProcessingGroup("progression-projection")
public class OnboardingCompletionHandler {

    private final CommandGateway commandGateway;

    public OnboardingCompletionHandler(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void on(ProgressionEvent.XpAwardedEvent event) {
        if (event.botGame()) {
            return;
        }
        commandGateway.send(new ProfileCommand.CompleteOnboardingCommand(
                event.userId(), Instant.now()));
    }
}
