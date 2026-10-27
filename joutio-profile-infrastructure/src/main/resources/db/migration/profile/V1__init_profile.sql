-- Joutio profile — baseline schema (squashed from V1..V20)
-- Generated from a fresh PostgreSQL 16 database after applying the legacy migrations.

--
-- PostgreSQL database dump
--


-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: player_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.player_reward (
    user_id character varying(255) NOT NULL,
    reward_code character varying(64) NOT NULL,
    source character varying(32) NOT NULL,
    unlocked_at timestamp with time zone NOT NULL
);


--
-- Name: presence_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.presence_entry (
    user_id character varying(255) NOT NULL,
    status character varying(10) NOT NULL,
    last_seen_at timestamp without time zone,
    offline_deadline_at timestamp without time zone
);


--
-- Name: presence_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.presence_session (
    session_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    connected_at timestamp without time zone NOT NULL,
    instance_id character varying(255),
    last_seen_at timestamp without time zone NOT NULL
);


--
-- Name: profile_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profile_entry (
    user_id character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    pseudonym character varying(100) NOT NULL,
    bio character varying(300),
    country character varying(100),
    language character varying(5) DEFAULT 'fr'::character varying NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    avatar_options character varying(2000),
    banner_url character varying(1024),
    equipped_title character varying(128),
    onboarding_completed_at timestamp without time zone,
    interests character varying(255)
);


--
-- Name: TABLE profile_entry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.profile_entry IS 'Table des profils utilisateur - projection read-only mise a jour via Event Handlers';


--
-- Name: COLUMN profile_entry.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.user_id IS 'Identifiant unique de l''utilisateur (UUID)';


--
-- Name: COLUMN profile_entry.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.email IS 'Email de l''utilisateur (denormalise depuis identity pour la recherche)';


--
-- Name: COLUMN profile_entry.pseudonym; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.pseudonym IS 'Pseudonyme public du joueur (modifiable par le proprietaire)';


--
-- Name: COLUMN profile_entry.bio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.bio IS 'Bio libre de l''utilisateur (max 300 caracteres)';


--
-- Name: COLUMN profile_entry.country; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.country IS 'Pays de l''utilisateur (max 100 caracteres)';


--
-- Name: COLUMN profile_entry.language; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.language IS 'Langue preferee du joueur (code ISO 639-1, defaut fr)';


--
-- Name: COLUMN profile_entry.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.created_at IS 'Date de creation du profil';


--
-- Name: COLUMN profile_entry.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.updated_at IS 'Date de derniere mise a jour du profil';


--
-- Name: COLUMN profile_entry.avatar_options; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.avatar_options IS 'Options DiceBear (style micah) serialisees en JSON, choisies par le proprietaire ; NULL = avatar derive du userId';


--
-- Name: COLUMN profile_entry.banner_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.banner_url IS 'URL externe optionnelle de la bannière de profil (image panoramique)';


--
-- Name: COLUMN profile_entry.equipped_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profile_entry.equipped_title IS 'Clé du titre thématique porté (topicId:TIER), NULL = aucun';


--
-- Name: progression_activity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_activity (
    user_id character varying(255) NOT NULL,
    current_streak integer DEFAULT 0 NOT NULL,
    longest_streak integer DEFAULT 0 NOT NULL,
    last_active_date date
);


--
-- Name: progression_activity_day; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_activity_day (
    user_id character varying(255) NOT NULL,
    activity_date date NOT NULL,
    games_count integer DEFAULT 0 NOT NULL
);


--
-- Name: progression_activity_day_game; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_activity_day_game (
    user_id character varying(255) NOT NULL,
    activity_date date NOT NULL,
    game_id character varying(255) NOT NULL
);


--
-- Name: progression_awarded_game; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_awarded_game (
    user_id character varying(255) NOT NULL,
    game_id character varying(255) NOT NULL,
    xp integer,
    rating_delta integer DEFAULT 0 NOT NULL,
    rated boolean DEFAULT false NOT NULL
);


--
-- Name: progression_badge; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_badge (
    user_id character varying(255) NOT NULL,
    badge character varying(50) NOT NULL
);


--
-- Name: TABLE progression_badge; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.progression_badge IS 'Badges debloques par joueur';


--
-- Name: progression_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_entry (
    user_id character varying(255) NOT NULL,
    games_played integer DEFAULT 0 NOT NULL,
    wins integer DEFAULT 0 NOT NULL,
    losses integer DEFAULT 0 NOT NULL,
    best_score integer DEFAULT 0 NOT NULL,
    current_win_streak integer DEFAULT 0 NOT NULL,
    best_win_streak integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    draws integer DEFAULT 0 NOT NULL,
    perfect_duels integer DEFAULT 0 NOT NULL,
    fast_answers_total integer DEFAULT 0 NOT NULL,
    current_day_streak integer DEFAULT 0 NOT NULL,
    best_day_streak integer DEFAULT 0 NOT NULL,
    last_active_date date,
    followers_gained integer DEFAULT 0 NOT NULL,
    challenges_sent integer DEFAULT 0 NOT NULL,
    image_correct_total integer DEFAULT 0 NOT NULL,
    rating integer DEFAULT 200 NOT NULL,
    peak_rating integer DEFAULT 200 NOT NULL,
    rated_games integer DEFAULT 0 NOT NULL,
    provisional boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE progression_entry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.progression_entry IS 'Progression globale par joueur (XP totale, niveau, titre)';


--
-- Name: COLUMN progression_entry.perfect_duels; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.perfect_duels IS 'Duels parfaits cumules (badge Precision)';


--
-- Name: COLUMN progression_entry.fast_answers_total; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.fast_answers_total IS 'Reponses rapides cumulees (badge Rapidite)';


--
-- Name: COLUMN progression_entry.current_day_streak; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.current_day_streak IS 'Serie courante de jours joues';


--
-- Name: COLUMN progression_entry.best_day_streak; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.best_day_streak IS 'Meilleure serie de jours joues (badge Assiduite)';


--
-- Name: COLUMN progression_entry.last_active_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.last_active_date IS 'Dernier jour joue (fuseau activite), NULL si aucun';


--
-- Name: COLUMN progression_entry.followers_gained; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.followers_gained IS 'Abonnes gagnes cumules (badge Social)';


--
-- Name: COLUMN progression_entry.challenges_sent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.challenges_sent IS 'Defis nominatifs lances (badge Defis)';


--
-- Name: COLUMN progression_entry.image_correct_total; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.progression_entry.image_correct_total IS 'Bonnes reponses sur questions illustrees (badge Vision)';


--
-- Name: progression_topic_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_topic_entry (
    user_id character varying(255) NOT NULL,
    topic_id character varying(255) NOT NULL,
    xp integer DEFAULT 0 NOT NULL
);


--
-- Name: TABLE progression_topic_entry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.progression_topic_entry IS 'XP cumulee par joueur et par theme';


--
-- Name: progression_topic_games; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progression_topic_games (
    user_id character varying(255) NOT NULL,
    topic_id character varying(255) NOT NULL,
    games integer DEFAULT 0 NOT NULL
);


--
-- Name: TABLE progression_topic_games; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.progression_topic_games IS 'Parties humaines jouees par joueur et par theme';


--
-- Name: player_reward player_reward_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.player_reward
    ADD CONSTRAINT player_reward_pkey PRIMARY KEY (user_id, reward_code);


--
-- Name: presence_entry presence_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.presence_entry
    ADD CONSTRAINT presence_entry_pkey PRIMARY KEY (user_id);


--
-- Name: presence_session presence_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.presence_session
    ADD CONSTRAINT presence_session_pkey PRIMARY KEY (session_id);


--
-- Name: profile_entry profile_entry_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile_entry
    ADD CONSTRAINT profile_entry_email_key UNIQUE (email);


--
-- Name: profile_entry profile_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile_entry
    ADD CONSTRAINT profile_entry_pkey PRIMARY KEY (user_id);


--
-- Name: progression_activity_day_game progression_activity_day_game_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_activity_day_game
    ADD CONSTRAINT progression_activity_day_game_pkey PRIMARY KEY (user_id, activity_date, game_id);


--
-- Name: progression_activity_day progression_activity_day_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_activity_day
    ADD CONSTRAINT progression_activity_day_pkey PRIMARY KEY (user_id, activity_date);


--
-- Name: progression_activity progression_activity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_activity
    ADD CONSTRAINT progression_activity_pkey PRIMARY KEY (user_id);


--
-- Name: progression_awarded_game progression_awarded_game_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_awarded_game
    ADD CONSTRAINT progression_awarded_game_pkey PRIMARY KEY (user_id, game_id);


--
-- Name: progression_badge progression_badge_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_badge
    ADD CONSTRAINT progression_badge_pkey PRIMARY KEY (user_id, badge);


--
-- Name: progression_entry progression_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_entry
    ADD CONSTRAINT progression_entry_pkey PRIMARY KEY (user_id);


--
-- Name: progression_topic_entry progression_topic_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_topic_entry
    ADD CONSTRAINT progression_topic_entry_pkey PRIMARY KEY (user_id, topic_id);


--
-- Name: progression_topic_games progression_topic_games_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_topic_games
    ADD CONSTRAINT progression_topic_games_pkey PRIMARY KEY (user_id, topic_id);


--
-- Name: idx_presence_entry_last_seen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_entry_last_seen ON public.presence_entry USING btree (last_seen_at);


--
-- Name: idx_presence_entry_offline_deadline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_entry_offline_deadline ON public.presence_entry USING btree (offline_deadline_at);


--
-- Name: idx_presence_entry_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_entry_status ON public.presence_entry USING btree (status);


--
-- Name: idx_presence_session_instance; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_session_instance ON public.presence_session USING btree (instance_id);


--
-- Name: idx_presence_session_last_seen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_session_last_seen ON public.presence_session USING btree (last_seen_at);


--
-- Name: idx_presence_session_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_presence_session_user ON public.presence_session USING btree (user_id);


--
-- Name: idx_profile_entry_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_profile_entry_email ON public.profile_entry USING btree (email);


--
-- Name: idx_profile_entry_pseudonym; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_profile_entry_pseudonym ON public.profile_entry USING btree (pseudonym);


--
-- Name: idx_progression_activity_day_game_user_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_progression_activity_day_game_user_date ON public.progression_activity_day_game USING btree (user_id, activity_date);


--
-- Name: progression_badge fk_progression_badge_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_badge
    ADD CONSTRAINT fk_progression_badge_user FOREIGN KEY (user_id) REFERENCES public.progression_entry(user_id) ON DELETE CASCADE;


--
-- Name: progression_topic_games fk_progression_topic_games_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_topic_games
    ADD CONSTRAINT fk_progression_topic_games_user FOREIGN KEY (user_id) REFERENCES public.progression_entry(user_id) ON DELETE CASCADE;


--
-- Name: progression_topic_entry fk_progression_topic_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progression_topic_entry
    ADD CONSTRAINT fk_progression_topic_user FOREIGN KEY (user_id) REFERENCES public.progression_entry(user_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


