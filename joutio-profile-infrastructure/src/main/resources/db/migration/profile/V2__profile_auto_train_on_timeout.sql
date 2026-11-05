-- Préférence de jeu : entraînement automatique contre le bot quand une recherche expire sans adversaire.
alter table profile_entry
    add column auto_train_on_timeout boolean not null default true;
